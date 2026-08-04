package com.prakash.pwall.service.performance

/**
 * Decides whether the render loop actually needs to draw a new frame. The
 * wallpaper only changes when the time text, an animation or a setting changes;
 * a frame whose key matches the previous drawn frame can be skipped entirely,
 * which avoids locking the surface (and re-compositing) when nothing changed.
 *
 * The key is only recorded via [markDrawn] once the frame has actually been
 * posted to the surface, so a failed lock (another thread already drawing)
 * never marks the frame as drawn and the next wake-up redraws it.
 *
 * [invalidate] forces the next frame to draw (settings change, image change,
 * mask reload, surface resize, failed frame, ...).
 */
class FrameDirtyChecker {

    private var lastKey: String? = null
    private var invalidated = true

    /** True when [key] differs from the last drawn frame (or invalidated). */
    fun shouldDraw(key: String): Boolean = invalidated || key != lastKey

    /** Records [key] as the most recent successfully drawn frame. */
    fun markDrawn(key: String) {
        lastKey = key
        invalidated = false
    }

    /** Forces the next frame to draw even if [key] is unchanged. */
    fun invalidate() {
        invalidated = true
    }

    fun reset() {
        lastKey = null
        invalidated = true
    }
}
