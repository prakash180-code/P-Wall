package com.prakash.pwall.service.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FramePacerTest {

    @Test
    fun staticClock_waitsUntilNextSecondBoundary() {
        val wait = FramePacer.nextDelayMillis(
            nowElapsed = 0L,
            nowWallMs = 12_345L,
            parallaxActive = false,
            parallaxMoving = false,
            parallaxIdleMs = 0L,
            transitionActive = false,
            breathingActive = false,
            zoomActive = false,
            lowEnd = false
        )
        // Next whole second is 13_000; 13_000 - 12_345 = 655.
        assertEquals(655L, wait)
    }

    @Test
    fun parallaxMoving_runsAtAbout60Fps() {
        // 1008 is a multiple of 16, so the next aligned frame is exactly one period away.
        val wait = FramePacer.nextDelayMillis(
            nowElapsed = 1008L,
            nowWallMs = 0L,
            parallaxActive = true,
            parallaxMoving = true,
            parallaxIdleMs = 0L,
            transitionActive = false,
            breathingActive = false,
            zoomActive = false,
            lowEnd = false
        )
        assertEquals(FramePacer.PARALLAX_FRAME_MS, wait)
    }

    @Test
    fun parallaxSettling_runsAtAbout5Fps() {
        val wait = FramePacer.nextDelayMillis(
            nowElapsed = 1_000L,
            nowWallMs = 0L,
            parallaxActive = true,
            parallaxMoving = false,
            parallaxIdleMs = 1_000L,
            transitionActive = false,
            breathingActive = false,
            zoomActive = false,
            lowEnd = false
        )
        assertEquals(FramePacer.SETTLING_FRAME_MS, wait)
    }

    @Test
    fun parallaxIdle_returnsToOncePerSecond() {
        val wait = FramePacer.nextDelayMillis(
            nowElapsed = 1_000L,
            nowWallMs = 0L,
            parallaxActive = true,
            parallaxMoving = false,
            parallaxIdleMs = 6_000L,
            transitionActive = false,
            breathingActive = false,
            zoomActive = false,
            lowEnd = false
        )
        assertEquals(FramePacer.SECOND_MS, wait)
    }

    @Test
    fun transition_runsAtAbout30Fps() {
        val wait = FramePacer.nextDelayMillis(
            nowElapsed = 990L,
            nowWallMs = 0L,
            parallaxActive = false,
            parallaxMoving = false,
            parallaxIdleMs = 0L,
            transitionActive = true,
            breathingActive = false,
            zoomActive = false,
            lowEnd = false
        )
        assertEquals(FramePacer.ANIMATION_FRAME_MS, wait)
    }

    @Test
    fun breathingOrZoom_runsAtAbout8Fps() {
        val breathing = FramePacer.nextDelayMillis(
            nowElapsed = 1_000L,
            nowWallMs = 0L,
            parallaxActive = false,
            parallaxMoving = false,
            parallaxIdleMs = 0L,
            transitionActive = false,
            breathingActive = true,
            zoomActive = false,
            lowEnd = false
        )
        val zoom = FramePacer.nextDelayMillis(
            nowElapsed = 1_000L,
            nowWallMs = 0L,
            parallaxActive = false,
            parallaxMoving = false,
            parallaxIdleMs = 0L,
            transitionActive = false,
            breathingActive = false,
            zoomActive = true,
            lowEnd = false
        )
        assertEquals(FramePacer.BREATHING_FRAME_MS, breathing)
        assertEquals(FramePacer.BREATHING_FRAME_MS, zoom)
    }

    @Test
    fun lowEnd_alwaysRunsAtOneFpsEvenWithAnimations() {
        val wait = FramePacer.nextDelayMillis(
            nowElapsed = 1_000L,
            nowWallMs = 0L,
            parallaxActive = false,
            parallaxMoving = false,
            parallaxIdleMs = 0L,
            transitionActive = true,
            breathingActive = true,
            zoomActive = true,
            lowEnd = true
        )
        assertEquals(FramePacer.SECOND_MS, wait)
    }

    @Test
    fun align_neverSleepsBelowTenMillis() {
        val wait = FramePacer.align(nowElapsed = 66L, period = 33L)
        assertTrue(wait >= 10L)
    }
}
