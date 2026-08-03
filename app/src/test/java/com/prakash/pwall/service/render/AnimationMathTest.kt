package com.prakash.pwall.service.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class AnimationMathTest {

    @Test
    fun easeOutCubic_respectsBounds() {
        assertEquals(0f, AnimationMath.easeOutCubic(0f), 0.0001f)
        assertEquals(1f, AnimationMath.easeOutCubic(1f), 0.0001f)
        assertEquals(0f, AnimationMath.easeOutCubic(-1f), 0.0001f)
        assertEquals(1f, AnimationMath.easeOutCubic(2f), 0.0001f)
    }

    @Test
    fun easeOutCubic_isMonotonic() {
        var previous = -1f
        for (i in 0..20) {
            val value = AnimationMath.easeOutCubic(i / 20f)
            assertTrue(value >= previous)
            previous = value
        }
    }

    @Test
    fun breathingPhase_wrapsEveryPeriod() {
        val atZero = AnimationMath.breathingPhase(0L)
        assertEquals(0f, atZero, 0.0001f)
        assertEquals(atZero, AnimationMath.breathingPhase(Breathing.PERIOD_MS), 0.0001f)
        assertTrue(
            AnimationMath.breathingPhase(Breathing.PERIOD_MS / 2) in 1f..(PI.toFloat() + 0.0001f)
        )
    }

    @Test
    fun breathing_scaleStaysWithinStrengthBand() {
        val breathing = Breathing(0f, 1f)
        assertEquals(1f + 0.018f, breathing.scale, 0.0001f)
        assertEquals(1f - 0.05f * 0.5f, breathing.alpha, 0.0001f)

        val breathingPeak = Breathing(PI.toFloat() / 2f, 1f)
        assertEquals(1f - 0.05f, breathingPeak.alpha, 0.0001f)
        assertEquals(1f + 0.018f * 0.5f, breathingPeak.scale, 0.0001f)
    }

    @Test
    fun breathing_clampsStrengthAboveOne() {
        val breathing = Breathing(0f, 5f)
        assertEquals(1f + 0.018f, breathing.scale, 0.0001f)
    }
}
