package com.prakash.pwall.service

import android.graphics.Bitmap
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.prakash.pwall.PWallApplication
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.di.AppContainer
import com.prakash.pwall.utils.BitmapCache
import com.prakash.pwall.utils.ImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Live wallpaper engine. Renders the selected image with a real-time clock and
 * date overlay, redrawing once per second (aligned to whole seconds) and
 * pausing all work while the wallpaper is not visible (e.g. screen off).
 */
class PWallWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = PWallEngine()

    private inner class PWallEngine : Engine() {

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private val container: AppContainer
            get() = (application as PWallApplication).container

        @Volatile
        private var settings: WallpaperSettings = WallpaperSettings()

        private var selectedBitmap: Bitmap? = null
        private var loadedPath: String? = null
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var surfaceHolder: SurfaceHolder? = null

        private var renderThread: Thread? = null

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            surfaceHolder = holder
            scope.launch {
                container.settingsRepository.settings.collect { newSettings ->
                    settings = newSettings
                    refreshBitmapIfNeeded()
                    renderOnce()
                }
            }
        }

        override fun onDestroy() {
            stopRenderThread()
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
                startRenderThread()
            } else {
                stopRenderThread()
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
            if (path != null && path != loadedPath) {
                loadedPath = path
                // Prefer the shared cache, otherwise decode once (synchronously on
                // the calling thread) and keep the decoded bitmap for the service.
                selectedBitmap = ImageLoader.cached(path)
                    ?: ImageLoader.decodeSampled(path)?.also {
                        BitmapCache.put(BitmapCache.keyFor(path), it)
                    }
            }
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
                // Sleep until the next whole second so the minute/second digit
                // changes line up with real time.
                val now = System.currentTimeMillis()
                val nextSecond = (now / 1000L + 1L) * 1000L
                val wait = nextSecond - now
                try {
                    Thread.sleep(wait.coerceAtLeast(100L))
                } catch (e: InterruptedException) {
                    break
                }
            }
        }

        private fun renderOnce() {
            if (surfaceWidth <= 0 || surfaceHeight <= 0) return
            val holder = surfaceHolder ?: return
            val canvas = holder.lockCanvas() ?: return
            try {
                WallpaperRenderer.drawBackground(canvas, selectedBitmap, settings.backgroundMode)
                WallpaperRenderer.drawClock(
                    canvas = canvas,
                    settings = settings,
                    displayDensity = resources.displayMetrics.scaledDensity
                )
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }
}
