package com.prakash.pwall.service.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RenderGuardTest {

    @Test
    fun failure_countsUntilThreshold() {
        val guard = RenderGuard(maxFailures = 3)
        assertFalse(guard.onFailure("layer-a"))
        assertFalse(guard.onFailure("layer-a"))
        assertTrue(guard.onFailure("layer-a"))
    }

    @Test
    fun differentLayers_failIndependently() {
        val guard = RenderGuard(maxFailures = 2)
        assertFalse(guard.onFailure("a"))
        assertTrue(guard.onFailure("a"))
        // b starts at zero even though a has already tripped.
        assertFalse(guard.onFailure("b"))
        assertTrue(guard.onFailure("b"))
    }

    @Test
    fun reset_clearsFailureCount() {
        val guard = RenderGuard(maxFailures = 2)
        guard.onFailure("a")
        guard.reset("a")
        assertEquals(0, guard.failuresOf("a"))
        assertFalse(guard.onFailure("a"))
    }

    @Test
    fun resetAll_clearsEveryLayer() {
        val guard = RenderGuard(maxFailures = 1)
        guard.onFailure("a")
        guard.onFailure("b")
        guard.resetAll()
        assertEquals(0, guard.failuresOf("a"))
        assertEquals(0, guard.failuresOf("b"))
    }
}
