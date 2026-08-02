package com.prakash.pwall.service.depth

import android.graphics.Bitmap
import com.prakash.pwall.data.model.WallpaperSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Owns the AI-depth pipeline for the live wallpaper.
 *
 * Invariants:
 * - Segmentation runs **only when the wallpaper image changes**, never per frame.
 * - When disabled, unsupported, or on any failure, it yields null and the renderer
 *   draws exactly as before (automatic fallback).
 * - Results are cached on disk keyed by image identity and reused across sessions.
 */
class DepthEngine(
    private val scope: CoroutineScope,
    private val store: DiskMaskStore,
    private val segmenter: DepthSegmenter,
    private val onResult: (SegmentationResult?) -> Unit
) {

    @Volatile private var enabled = false
    private val current = AtomicReference<SegmentationResult?>(null)
    private var job: Job? = null
    private var activeKey: String? = null

    /** True when depth is enabled and a segmenter is usable. */
    val isActive: Boolean get() = enabled && segmenter.isSupported()

    /**
     * Applies the latest settings. Returns true when depth was just turned on and
     * the caller should (re)run segmentation for the current image; turning it off
     * immediately clears the in-memory result so the renderer falls back.
     */
    fun updateSettings(settings: WallpaperSettings): Boolean {
        val wasEnabled = enabled
        enabled = settings.depthEnabled
        if (enabled == wasEnabled) return false
        if (enabled) {
            return true
        }
        clear()
        return false
    }

    /**
     * Called when the wallpaper image is replaced (service image-change path).
     * Cancels any in-flight segmentation, drops stale results, and starts a fresh
     * pass (disk cache first, then live segmentation).
     */
    fun onImageChanged(path: String, bitmap: Bitmap) {
        job?.cancel()
        current.set(null)
        onResult(null)
        if (!enabled || !segmenter.isSupported()) return
        val key = MaskKeys.forImage(path, File(path).lastModified())
        activeKey = key
        job = scope.launch {
            val result = withContext(Dispatchers.IO) {
                store.load(key) ?: segmenter.segment(bitmap)?.also { store.save(key, it) }
            }
            if (job?.isActive != true) return@launch
            if (result == null) {
                current.set(null)
                onResult(null)
            } else {
                current.set(result)
                onResult(result)
            }
        }
    }

    /** Latest completed result, for the render loop. */
    fun currentResult(): SegmentationResult? = current.get()

    /** Drops in-memory state and signals the renderer to fall back to plain drawing. */
    fun clear() {
        job?.cancel()
        job = null
        current.set(null)
        activeKey = null
        onResult(null)
    }

    fun close() {
        clear()
        segmenter.close()
    }
}
