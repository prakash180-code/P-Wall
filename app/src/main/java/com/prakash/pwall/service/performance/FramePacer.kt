package com.prakash.pwall.service.performance

/**
 * Battery-aware frame pacing. Pure, JVM-testable version of the service's
 * adaptive render loop:
 * - 3D parallax moving: ~60 fps for smooth motion
 * - parallax settling: ~5 fps so pickup stays responsive
 * - parallax idle: 1 fps clock-only redraw
 * - low-end mode: always 1 fps (animations are disabled anyway)
 * - digit cross-fade: ~30 fps during the short transition
 * - breathing / cinematic zoom: ~8 fps while they sweep
 * - otherwise: aligned to the next whole second
 */
object FramePacer {

    /** ~60 fps during movement. */
    const val PARALLAX_FRAME_MS = 16L

    /** ~5 fps while the device settles after movement. */
    const val SETTLING_FRAME_MS = 200L

    /** After this long without movement, drop to the 1 fps clock loop. */
    const val IDLE_SETTLE_MS = 5000L

    /** ~30 fps during a text cross-fade. */
    const val ANIMATION_FRAME_MS = 33L

    /** ~8 fps for the continuous breathing / cinematic zoom sweep. */
    const val BREATHING_FRAME_MS = 125L

    /** One redraw per second for the plain clock. */
    const val SECOND_MS = 1000L

    /**
     * @param nowElapsed monotonic clock (SystemClock.elapsedRealtime)
     * @param nowWallMs wall clock (System.currentTimeMillis), for the 1 fps alignment
     */
    fun nextDelayMillis(
        nowElapsed: Long,
        nowWallMs: Long,
        parallaxActive: Boolean,
        parallaxMoving: Boolean,
        parallaxIdleMs: Long,
        transitionActive: Boolean,
        breathingActive: Boolean,
        zoomActive: Boolean,
        lowEnd: Boolean
    ): Long {
        if (parallaxActive) {
            return when {
                parallaxMoving -> align(nowElapsed, PARALLAX_FRAME_MS)
                parallaxIdleMs < IDLE_SETTLE_MS -> align(nowElapsed, SETTLING_FRAME_MS)
                else -> align(nowElapsed, SECOND_MS)
            }
        }
        if (lowEnd) return align(nowElapsed, SECOND_MS)
        if (transitionActive) return align(nowElapsed, ANIMATION_FRAME_MS)
        if (breathingActive || zoomActive) return align(nowElapsed, BREATHING_FRAME_MS)
        return nextSecondWait(nowWallMs)
    }

    /** Milliseconds until the next whole second boundary (wall clock). */
    fun nextSecondWait(nowWallMs: Long): Long =
        (nowWallMs / 1000L + 1L) * 1000L - nowWallMs

    /** Milliseconds until the next multiple of [period] on the monotonic clock. */
    fun align(nowElapsed: Long, period: Long): Long =
        (nextAligned(nowElapsed, period) - nowElapsed).coerceAtLeast(10L)

    private fun nextAligned(nowElapsed: Long, period: Long): Long =
        (nowElapsed / period + 1L) * period
}
