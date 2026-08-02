package com.prakash.pwall.service.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParallaxMathTest {

    private val eps = 1e-4f

    @Test
    fun smooth_zeroFactor_snapsToTarget() {
        assertEquals(1f, ParallaxMath.smooth(0f, 1f, 0f), eps)
    }

    @Test
    fun smooth_oneFactor_neverMoves() {
        assertEquals(0f, ParallaxMath.smooth(0f, 1f, 1f), eps)
    }

    @Test
    fun smooth_halfFactor_movesHalfway() {
        assertEquals(0.5f, ParallaxMath.smooth(0f, 1f, 0.5f), eps)
    }

    @Test
    fun smooth_convergesTowardsTarget() {
        var value = 0f
        for (i in 1..30) {
            value = ParallaxMath.smooth(value, 1f, 0.2f)
        }
        assertTrue(value > 0.99f && value <= 1f)
    }

    @Test
    fun smooth_clampsFactorOutsideRange() {
        assertEquals(1f, ParallaxMath.smooth(0f, 1f, -5f), eps)
        assertEquals(0f, ParallaxMath.smooth(0f, 1f, 9f), eps)
    }

    @Test
    fun backgroundTilt_maxInputs_yieldsMaxShift() {
        assertEquals(
            ParallaxMath.MAX_BACKGROUND_SHIFT,
            ParallaxMath.backgroundTilt(1f, 1f, 1f),
            eps
        )
    }

    @Test
    fun backgroundTilt_zeroSensitivity_orStrength_hasNoEffect() {
        assertEquals(0f, ParallaxMath.backgroundTilt(1f, 0f, 1f), eps)
        assertEquals(0f, ParallaxMath.backgroundTilt(1f, 1f, 0f), eps)
    }

    @Test
    fun backgroundTilt_scalesLinearly() {
        assertEquals(
            ParallaxMath.MAX_BACKGROUND_SHIFT / 2f,
            ParallaxMath.backgroundTilt(1f, 0.5f, 1f),
            eps
        )
        assertEquals(
            -ParallaxMath.MAX_BACKGROUND_SHIFT / 4f,
            ParallaxMath.backgroundTilt(-0.5f, 0.5f, 1f),
            eps
        )
    }

    @Test
    fun backgroundTilt_clampsRawTilt() {
        assertEquals(
            ParallaxMath.MAX_BACKGROUND_SHIFT,
            ParallaxMath.backgroundTilt(42f, 1f, 1f),
            eps
        )
        assertEquals(
            -ParallaxMath.MAX_BACKGROUND_SHIFT,
            ParallaxMath.backgroundTilt(-42f, 1f, 1f),
            eps
        )
    }

    @Test
    fun foregroundTilt_isOppositeAndFractionOfBackground() {
        assertEquals(
            -ParallaxMath.MAX_BACKGROUND_SHIFT * ParallaxMath.FOREGROUND_DEPTH_FACTOR,
            ParallaxMath.foregroundTilt(ParallaxMath.MAX_BACKGROUND_SHIFT),
            eps
        )
    }

    @Test
    fun clampBackground_clampsToPanRoom() {
        assertEquals(5f to 5f, ParallaxMath.clampBackground(99f, 99f, 5f, 5f))
        assertEquals(-5f to -5f, ParallaxMath.clampBackground(-99f, -99f, 5f, 5f))
        assertEquals(3f to 4f, ParallaxMath.clampBackground(3f, 4f, 5f, 5f))
    }

    @Test
    fun clampBackground_zeroPanRoom_pinsToZero() {
        val (dx, dy) = ParallaxMath.clampBackground(10f, -10f, 0f, 0f)
        assertEquals(0f, dx, eps)
        assertEquals(0f, dy, eps)
    }

    @Test
    fun backgroundShiftPx_usesMinScreenDim() {
        val (dx, dy) = ParallaxMath.backgroundShiftPx(
            tiltX = 1f, tiltY = 1f, sensitivity = 1f, strength = 1f,
            minScreenDim = 1000f, maxPanX = 1000f, maxPanY = 1000f
        )
        assertEquals(45f, dx, eps)
        assertEquals(45f, dy, eps)
    }

    @Test
    fun backgroundShiftPx_clampsToRemainingPanRoom() {
        val (dx, dy) = ParallaxMath.backgroundShiftPx(
            tiltX = 1f, tiltY = 1f, sensitivity = 1f, strength = 1f,
            minScreenDim = 1000f, maxPanX = 10f, maxPanY = 5f
        )
        assertEquals(10f, dx, eps)
        assertEquals(5f, dy, eps)
    }

    @Test
    fun backgroundShiftPx_userPanReducesRoom() {
        val (dx, _) = ParallaxMath.backgroundShiftPx(
            tiltX = 1f, tiltY = 0f, sensitivity = 1f, strength = 1f,
            minScreenDim = 1000f, maxPanX = 45f, maxPanY = 45f,
            userPanX = 45f
        )
        assertEquals(0f, dx, eps)
    }

    @Test
    fun backgroundShiftPx_noPanRoom_noMovement() {
        val (dx, dy) = ParallaxMath.backgroundShiftPx(
            tiltX = 1f, tiltY = 1f, sensitivity = 1f, strength = 1f,
            minScreenDim = 1000f, maxPanX = 0f, maxPanY = 0f
        )
        assertEquals(0f, dx, eps)
        assertEquals(0f, dy, eps)
    }

    @Test
    fun backgroundShiftPx_zeroScreenDim_noMovement() {
        val (dx, dy) = ParallaxMath.backgroundShiftPx(
            tiltX = 1f, tiltY = 1f, sensitivity = 1f, strength = 1f,
            minScreenDim = 0f, maxPanX = 100f, maxPanY = 100f
        )
        assertEquals(0f, dx, eps)
        assertEquals(0f, dy, eps)
    }
}
