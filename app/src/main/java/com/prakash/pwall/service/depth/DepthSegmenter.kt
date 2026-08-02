package com.prakash.pwall.service.depth

import android.graphics.Bitmap

/**
 * Pluggable backend for extracting the foreground subject from a wallpaper image.
 *
 * Implementations must be cheap to create, return null on any failure (automatic
 * fallback to normal rendering), and never block the caller (see [segment]).
 */
interface DepthSegmenter {

    /**
     * Whether this backend is usable on the current device. Returning false (or
     * creating the underlying model client failing) forces the automatic fallback.
     */
    fun isSupported(): Boolean

    /**
     * Runs a single segmentation pass. Suspends, so callers can invoke it off the
     * main thread; returns null on error or when the model is unavailable.
     */
    suspend fun segment(bitmap: Bitmap): SegmentationResult?

    /** Releases any native resources. Safe to call multiple times. */
    fun close()
}
