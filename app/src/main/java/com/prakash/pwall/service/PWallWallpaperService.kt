package com.prakash.pwall.service

import android.graphics.Bitmap
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.prakash.pwall.PWallApplication
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.di.AppContainer
import com.prakash.pwall.service.depth.DepthEngine
import com.prakash.pwall.service.depth.DiskMaskStore
import com.prakash.pwall.service.depth.MlKitSubjectSegmenter
import com.prakash.pwall.service.motion.ParallaxController
import com.prakash.pwall.service.render.RenderFrame
import com.prakash.pwall.service.render.WallpaperCoreModule
import com.prakash.pwall.service.render.WallpaperRenderEngine
import com.prakash.pwall.utils.BitmapCache
import com.prakash.pwall.utils.ImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** ~30 fps during movement. */
private const val PARALLAX_FRAME_MS = 33L

/** ~5 fps while the device settles after movement. */
private const val SETTLING_FRAME_MS = 200L

/** After this long without movement, drop to the 1 fps clock loop. */
private const val IDLE_SETTLE_MS = 5000L

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

        @Volatile
        private var selectedBitmap: Bitmap? = null
        private var loadedPath: String? = null
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var surfaceHolder: SurfaceHolder? = null

        private var decodeJob: Job? = null
        private var renderThread: Thread? = null

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
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            surfaceHolder = holder
            scope.launch {
                container.settingsRepository.settings.collect { newSettings ->
                    settings = newSettings
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
            stopRenderThread()
            parallaxController.stop()
            depthEngine.close()
            decodeJob?.cancel()
            scope.cancel()
            super.onDestroy()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceHolder = holder
            startRenderThread()
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
            renderOnce()
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            super.onSurfaceRedrawNeeded(holder)
            renderOnce()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
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
        }

        private fun refreshBitmapIfNeeded() {
            val path = settings.selectedImagePath
            if (path == null || path == loadedPath) return
            loadedPath = path
            decodeJob?.cancel()
            // Decode off the main thread; the shared cache avoids re-decoding.
            decodeJob = scope.launch(Dispatchers.IO) {
                val bitmap = ImageLoader.cached(path)
                    ?: ImageLoader.decodeSampled(path)?.also {
                        BitmapCache.put(BitmapCache.keyFor(path), it)
                    }
                selectedBitmap = bitmap
                if (bitmap != null) {
                    depthEngine.onImageChanged(path, bitmap)
                }
                renderOnce()
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
                renderOnce()
                val wait = nextFrameDelay()
                try {
                    Thread.sleep(wait)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }

        /**
         * Battery-aware frame pacing:
         * - parallax off: redraw once per second, aligned to whole seconds
         * - parallax on + moving: ~30 fps for smooth premium motion
         * - parallax on + settling: ~5 fps so pickup is responsive
         * - parallax on + idle: back to 1 fps (clock-only redraw)
         */
        private fun nextFrameDelay(): Long {
            val now = SystemClock.elapsedRealtime()
            if (parallaxController.isActive()) {
                if (parallaxController.isMoving()) {
                    return alignMillis(now, PARALLAX_FRAME_MS)
                }
                if (parallaxController.idleMilliseconds() < IDLE_SETTLE_MS) {
                    return alignMillis(now, SETTLING_FRAME_MS)
                }
                return alignMillis(now, 1000L)
            }
            return nextSecondWaitMillis()
        }

        private fun nextSecondWaitMillis(): Long {
            val now = System.currentTimeMillis()
            return (now / 1000L + 1L) * 1000L - now
        }

        private fun alignMillis(now: Long, period: Long): Long {
            val next = (now / period + 1L) * period
            return (next - now).coerceAtLeast(10L)
        }

        private fun renderOnce() {
            if (surfaceWidth <= 0 || surfaceHeight <= 0) return
            val holder = surfaceHolder ?: return
            // lockCanvas returns null if another thread is drawing; a failure
            // here is transient and must not kill the render thread.
            val canvas = runCatching { holder.lockCanvas() }.getOrNull() ?: return
            try {
                renderEngine.render(
                    canvas,
                    RenderFrame(
                        settings = settings,
                        backgroundBitmap = selectedBitmap,
                        foregroundBitmap = depthEngine.currentResult()?.foreground,
                        displayDensity = resources.displayMetrics.scaledDensity,
                        width = canvas.width,
                        height = canvas.height
                    )
                )
            } catch (_: Exception) {
                // Best-effort frame: ignore transient draw errors.
            } finally {
                runCatching { holder.unlockCanvasAndPost(canvas) }
            }
        }
    }
}
