package com.prakash.pwall.service

import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.prakash.pwall.PWallApplication
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.di.AppContainer
import com.prakash.pwall.service.color.ColorPalette
import com.prakash.pwall.service.color.DominantColorExtractor
import com.prakash.pwall.service.depth.DepthEngine
import com.prakash.pwall.service.depth.DiskMaskStore
import com.prakash.pwall.service.depth.MlKitSubjectSegmenter
import com.prakash.pwall.service.effects.CinematicZoom
import com.prakash.pwall.service.motion.ParallaxController
import com.prakash.pwall.service.performance.FrameDirtyChecker
import com.prakash.pwall.service.performance.FramePacer
import com.prakash.pwall.service.performance.LowEndDevice
import com.prakash.pwall.service.render.AnimationMath
import com.prakash.pwall.service.render.Breathing
import com.prakash.pwall.service.render.PremiumEffectsModule
import com.prakash.pwall.service.render.RenderFrame
import com.prakash.pwall.service.render.TimeTransition
import com.prakash.pwall.service.render.WallpaperCoreModule
import com.prakash.pwall.service.render.WallpaperRenderEngine
import com.prakash.pwall.utils.BitmapCache
import com.prakash.pwall.utils.ClockTextFormatter
import com.prakash.pwall.utils.ImageLoader
import com.prakash.pwall.utils.PWallLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/** Backoff sleep when the render thread keeps failing to draw a frame. */
private const val RENDER_FAILURE_BACKOFF_MS = 2000L

/**
 * Live wallpaper engine. Renders the selected image with a real-time clock and
 * date overlay. Redraws once per second (aligned to whole seconds) when static;
 * when the optional 3D parallax feature is enabled and the device is moving it
 * raises the frame rate for smooth motion, and pauses all work while the
 * wallpaper is not visible (e.g. screen off).
 */
class PWallWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = PWallEngine()

    private inner class PWallEngine : Engine() {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private val container: AppContainer
            get() = (application as PWallApplication).container

        @Volatile
        private var settings: WallpaperSettings = WallpaperSettings()

        /** True while the low-end performance profile is active. */
        @Volatile
        private var lowEnd: Boolean = false

        @Volatile
        private var selectedBitmap: Bitmap? = null
        private var loadedPath: String? = null
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var surfaceHolder: SurfaceHolder? = null

        private var decodeJob: Job? = null
        private var renderThread: Thread? = null

        private val dirtyChecker = FrameDirtyChecker()
        private var consecutiveRenderFailures = 0

        /** Dominant colors of the current image, for the dynamic colors feature. */
        private var palette: ColorPalette? = null

        private val frameTicker = FrameTicker()

        private val parallaxController: ParallaxController by lazy {
            ParallaxController(applicationContext)
        }

        private val depthEngine: DepthEngine by lazy {
            DepthEngine(
                scope = scope,
                store = DiskMaskStore(applicationContext),
                segmenter = MlKitSubjectSegmenter(),
                onResult = { renderOnce() }
            )
        }

        private val renderEngine: WallpaperRenderEngine by lazy {
            WallpaperRenderEngine(motionSource = parallaxController).apply {
                installModule(WallpaperCoreModule())
                installModule(PremiumEffectsModule())
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            surfaceHolder = holder
            PWallLog.i("Engine created")
            scope.launch {
                container.settingsRepository.settings.collect { newSettings ->
                    settings = newSettings
                    updateLowEnd()
                    parallaxController.updateSettings(newSettings)
                    if (depthEngine.updateSettings(newSettings)) {
                        runDepthForCurrentImage()
                    }
                    refreshBitmapIfNeeded()
                    renderOnce()
                }
            }
            scope.launch {
                container.maskEditNotifier.collect {
                    depthEngine.reloadFromCache()
                    renderOnce()
                }
            }
        }

        override fun onDestroy() {
            PWallLog.i("Engine destroyed")
            stopRenderThread()
            parallaxController.stop()
            depthEngine.close()
            decodeJob?.cancel()
            renderEngine.release()
            scope.cancel()
            super.onDestroy()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceHolder = holder
            startRenderThread()
            PWallLog.d("Surface created")
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            dirtyChecker.invalidate()
            renderOnce()
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            super.onSurfaceRedrawNeeded(holder)
            dirtyChecker.invalidate()
            renderOnce()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            PWallLog.d("Visibility changed: visible=$visible")
            if (visible) {
                dirtyChecker.invalidate()
                parallaxController.start()
                startRenderThread()
            } else {
                stopRenderThread()
                parallaxController.stop()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            stopRenderThread()
            surfaceHolder = null
            surfaceWidth = 0
            surfaceHeight = 0
            dirtyChecker.reset()
        }

        private fun refreshBitmapIfNeeded() {
            val path = settings.selectedImagePath
            if (path == null || path == loadedPath) return
            loadedPath = path
            decodeJob?.cancel()
            val maxDimension = if (lowEnd) {
                LowEndDevice.LOW_END_MAX_DIMENSION
            } else {
                LowEndDevice.NORMAL_MAX_DIMENSION
            }
            // Decode off the main thread; the shared cache avoids re-decoding.
            decodeJob = scope.launch(Dispatchers.IO) {
                val bitmap = ImageLoader.cached(path)
                    ?: ImageLoader.decodeSampled(path, maxDimension)?.also {
                        BitmapCache.put(BitmapCache.keyFor(path), it)
                    }
                selectedBitmap = bitmap
                if (bitmap != null) {
                    depthEngine.onImageChanged(path, bitmap)
                    palette = DominantColorExtractor.extract(bitmap)
                }
                renderOnce()
            }
        }

        /** Recomputes the low-end performance profile from hardware + preference. */
        private fun updateLowEnd() {
            val am = getSystemService(android.app.ActivityManager::class.java)
            val newValue = LowEndDevice.decide(
                memoryClassMb = am.memoryClass,
                isLowRamDevice = am.isLowRamDevice,
                preference = settings.lowEnd
            )
            if (newValue != lowEnd) {
                lowEnd = newValue
                PWallLog.i("Low-end mode ${if (newValue) "enabled" else "disabled"}")
                dirtyChecker.invalidate()
            }
        }

        /** Re-runs segmentation for the already-loaded image (depth just turned on). */
        private fun runDepthForCurrentImage() {
            val path = loadedPath ?: return
            val bitmap = selectedBitmap ?: return
            depthEngine.onImageChanged(path, bitmap)
        }

        private fun startRenderThread() {
            if (renderThread != null) return
            val thread = Thread(
                { renderLoop() },
                "pwall-renderer"
            )
            renderThread = thread
            thread.start()
        }

        private fun stopRenderThread() {
            val thread = renderThread
            renderThread = null
            thread?.interrupt()
            thread?.join(300)
        }

        private fun renderLoop() {
            while (!Thread.currentThread().isInterrupted) {
                val ok = runCatching { renderOnce() }.isSuccess
                if (ok) {
                    consecutiveRenderFailures = 0
                } else {
                    consecutiveRenderFailures++
                    PWallLog.e("Render frame failed (${consecutiveRenderFailures})")
                }
                val wait = if (consecutiveRenderFailures >= 5) {
                    RENDER_FAILURE_BACKOFF_MS
                } else {
                    nextFrameDelay()
                }
                try {
                    Thread.sleep(wait)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }

        /**
         * Battery-aware frame pacing (pure logic in [FramePacer]):
         * - parallax off: redraw once per second, aligned to whole seconds
         * - parallax on + moving: ~30 fps for smooth premium motion
         * - parallax on + settling: ~5 fps so pickup is responsive
         * - parallax on + idle: back to 1 fps (clock-only redraw)
         * - premium animations: ~30 fps during a text cross-fade, ~8 fps while
         *   breathing or the cinematic zoom is sweeping
         * - low-end mode: always 1 fps (animations are disabled anyway)
         */
        private fun nextFrameDelay(): Long {
            val nowElapsed = SystemClock.elapsedRealtime()
            val nowMs = System.currentTimeMillis()
            val s = settings
            return FramePacer.nextDelayMillis(
                nowElapsed = nowElapsed,
                nowWallMs = nowMs,
                parallaxActive = parallaxController.isActive(),
                parallaxMoving = parallaxController.isMoving(),
                parallaxIdleMs = parallaxController.idleMilliseconds(),
                transitionActive = (s.fadeTransitionsEnabled || s.smoothSecondsEnabled) &&
                    frameTicker.isTransitionActive(nowMs),
                breathingActive = s.breathingEnabled && s.breathingStrength > 0f,
                zoomActive = s.zoomEnabled && s.zoomStrength > 0f,
                lowEnd = lowEnd
            )
        }

        private fun renderOnce() {
            if (surfaceWidth <= 0 || surfaceHeight <= 0) return
            val holder = surfaceHolder ?: return
            val now = LocalDateTime.now()
            val nowMs = System.currentTimeMillis()
            val current = if (lowEnd) {
                LowEndDevice.optimizedSettings(settings, lowEnd)
            } else {
                settings
            }

            val transition = frameTicker.transition(now, current, nowMs)
            val motion = parallaxController.currentMotion()
            val breathing = if (current.breathingEnabled && current.breathingStrength > 0f) {
                Breathing(AnimationMath.breathingPhase(nowMs), current.breathingStrength)
            } else {
                null
            }
            val cinematicZoom = if (current.zoomEnabled && current.zoomStrength > 0f) {
                CinematicZoom.zoomAt(
                    elapsedMs = nowMs,
                    durationMs = (current.zoomDurationSeconds * 1000f).toLong(),
                    strength = current.zoomStrength,
                    direction = current.zoomDirection
                )
            } else {
                1f
            }

            // Skip the frame when nothing that affects pixels changed since the
            // last drawn frame (steady-state clock between whole seconds, or a
            // settings event that did not alter rendering). The key is only
            // recorded after the frame is actually posted (see markDrawn below).
            val signature = frameSignature(
                now, current, motion, transition, breathing, cinematicZoom
            )
            if (!dirtyChecker.shouldDraw(signature)) {
                return
            }

            // Prefer a hardware canvas (API 30+), falling back to the software
            // canvas when the surface rejects it; lockCanvas returns null if
            // another thread is drawing, which is transient and must not kill
            // the render thread.
            val canvas = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }
            }.getOrNull() ?: runCatching { holder.lockCanvas() }.getOrNull() ?: return
            try {
                renderEngine.render(
                    canvas,
                    RenderFrame(
                        settings = current,
                        backgroundBitmap = selectedBitmap,
                        foregroundBitmap = depthEngine.currentResult()?.foreground,
                        displayDensity = resources.displayMetrics.scaledDensity,
                        width = canvas.width,
                        height = canvas.height,
                        palette = palette,
                        motion = motion,
                        timeTransition = transition,
                        breathing = breathing,
                        cinematicZoom = cinematicZoom,
                        paintCache = renderEngine.paintCache
                    )
                )
            } catch (_: Exception) {
                // Best-effort frame: ignore transient draw errors.
            } finally {
                runCatching { holder.unlockCanvasAndPost(canvas) }
                dirtyChecker.markDrawn(signature)
            }
        }

        /**
         * Compact signature of everything that affects the rendered pixels.
         * A settings change, a new time/date string, a motion change or an
         * active animation all alter it, so identical signatures imply the
         * previous frame is still current and can be skipped.
         */
        private fun frameSignature(
            now: LocalDateTime,
            current: WallpaperSettings,
            motion: Any?,
            transition: TimeTransition?,
            breathing: Breathing?,
            cinematicZoom: Float
        ): String = buildString {
            append(ClockTextFormatter.formatTime(now, current.timeFormat, current.showSeconds))
            append('|').append(ClockTextFormatter.formatDate(now, current.dateFormat))
            append('|').append(current)
            append('|').append(motion)
            append('|').append(transition?.progress?.toString() ?: "null")
            append('|').append(breathing?.phase?.toString() ?: "null")
            append('|').append(cinematicZoom.toString())
        }
    }

    /**
     * Remembers the previous time/date text and the moment it changed, so the
     * render loop can cross-fade between the old and new digits whenever the
     * clock ticks. Pure state; no Android types.
     */
    private class FrameTicker {

        private var prevTime: String? = null
        private var prevDate: String? = null
        private var currentTime: String? = null
        private var currentDate: String? = null
        private var changeMs = 0L
        private var inited = false

        fun transition(now: LocalDateTime, settings: WallpaperSettings, nowMs: Long): TimeTransition? {
            val timeText = ClockTextFormatter.formatTime(
                now, settings.timeFormat, settings.showSeconds
            )
            val dateText = ClockTextFormatter.formatDate(now, settings.dateFormat)
            if (!inited) {
                currentTime = timeText
                currentDate = dateText
                inited = true
                return null
            }
            if (timeText != currentTime || dateText != currentDate) {
                prevTime = currentTime
                prevDate = currentDate
                currentTime = timeText
                currentDate = dateText
                changeMs = nowMs
            }
            if (!settings.fadeTransitionsEnabled && !settings.smoothSecondsEnabled) return null
            val elapsed = nowMs - changeMs
            if (elapsed >= AnimationMath.TRANSITION_MS) return null
            return TimeTransition(
                oldTimeText = prevTime,
                oldDateText = prevDate,
                progress = AnimationMath.easeOutCubic(
                    elapsed.toFloat() / AnimationMath.TRANSITION_MS
                )
            )
        }

        /** True while a cross-fade is still running (drives the frame pacing). */
        fun isTransitionActive(nowMs: Long): Boolean =
            inited && (nowMs - changeMs) < AnimationMath.TRANSITION_MS
    }
}
