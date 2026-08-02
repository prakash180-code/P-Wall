package com.prakash.pwall.service.motion

import kotlin.math.abs
import kotlin.math.max

/**
 * Pure math for the 3D parallax engine. No Android types so it can be unit
 * tested on the JVM. Handles the low-pass smoothing filter and the mapping from
 * raw tilt onto the background / foreground movement amplitudes.
 *
 * Movement is intentionally subtle: at full strength the background shifts a
 * small fraction of the screen and the foreground (clock) shifts a smaller
 * fraction in the opposite direction to create depth.
 */
object ParallaxMath {

    /** Foreground (clock) moves at this fraction of the background amplitude. */
    const val FOREGROUND_DEPTH_FACTOR = 0.35f

    /** Maximum background shift as a fraction of the smallest screen dimension. */
    const val MAX_BACKGROUND_SHIFT = 0.045f

    /**
     * Exponential moving average (low-pass filter). [smoothing] in 0..1; higher
     * values make the motion more fluid and laggy (premium feel), lower values
     * feel snappier. A value of 0 snaps instantly, 1 never moves.
     */
    fun smooth(current: Float, target: Float, smoothing: Float): Float {
        val factor = smoothing.coerceIn(0f, 1f)
        return current + (target - current) * (1f - factor)
    }

    /**
     * Maps a raw sensor tilt component (roughly -1..1) onto the final background
     * translation, scaled by [sensitivity] and [strength] (both 0..1).
     * Returns a value in device-independent normalized units centered on 0.
     */
    fun backgroundTilt(tilt: Float, sensitivity: Float, strength: Float): Float {
        val normalized = tilt.coerceIn(-1f, 1f)
        val applied = normalized * sensitivity.coerceIn(0f, 1f) * strength.coerceIn(0f, 1f)
        return applied * MAX_BACKGROUND_SHIFT
    }

    /**
     * Foreground offset for a given background offset, in the opposite
     * direction so the foreground appears to sit in front of the background.
     */
    fun foregroundTilt(backgroundOffset: Float): Float =
        -backgroundOffset * FOREGROUND_DEPTH_FACTOR

    /**
     * Clamps an accumulated background translation so the image always covers
     * the target. [maxPanX]/[maxPanY] are the real overflow margins (0 when the
     * image exactly fills the screen), so in Fill/Fit modes with no overflow the
     * parallax adds no visible movement.
     */
    fun clampBackground(
        offsetX: Float,
        offsetY: Float,
        maxPanX: Float,
        maxPanY: Float
    ): Pair<Float, Float> =
        offsetX.coerceIn(-maxPanX, maxPanX) to offsetY.coerceIn(-maxPanY, maxPanY)

    /**
     * Final background pixel shift for a frame: tilt -> normalized offset ->
     * pixels on the smallest screen dimension, then clamped to the pan room that
     * remains after the user's own custom pan is applied.
     */
    fun backgroundShiftPx(
        tiltX: Float,
        tiltY: Float,
        sensitivity: Float,
        strength: Float,
        minScreenDim: Float,
        maxPanX: Float,
        maxPanY: Float,
        userPanX: Float = 0f,
        userPanY: Float = 0f
    ): Pair<Float, Float> {
        val shiftX = backgroundTilt(tiltX, sensitivity, strength) * minScreenDim
        val shiftY = backgroundTilt(tiltY, sensitivity, strength) * minScreenDim
        val remainingX = max(0f, maxPanX - abs(userPanX))
        val remainingY = max(0f, maxPanY - abs(userPanY))
        return clampBackground(shiftX, shiftY, remainingX, remainingY)
    }
}
