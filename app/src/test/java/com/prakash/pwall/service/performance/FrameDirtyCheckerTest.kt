package com.prakash.pwall.service.performance

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameDirtyCheckerTest {

    @Test
    fun firstFrame_alwaysDraws() {
        val checker = FrameDirtyChecker()
        assertTrue(checker.shouldDraw("a"))
    }

    @Test
    fun unmarkedKey_redrawsUntilMarked() {
        val checker = FrameDirtyChecker()
        assertTrue(checker.shouldDraw("a"))
        // The frame was never posted, so it must be drawn again.
        assertTrue(checker.shouldDraw("a"))
        checker.markDrawn("a")
        assertFalse(checker.shouldDraw("a"))
    }

    @Test
    fun markedSameKey_skipsNextFrame() {
        val checker = FrameDirtyChecker()
        checker.markDrawn("a")
        assertFalse(checker.shouldDraw("a"))
    }

    @Test
    fun changedKey_drawsAgain() {
        val checker = FrameDirtyChecker()
        checker.markDrawn("a")
        assertTrue(checker.shouldDraw("b"))
    }

    @Test
    fun invalidate_forcesDrawDespiteSameKey() {
        val checker = FrameDirtyChecker()
        checker.markDrawn("a")
        checker.invalidate()
        assertTrue(checker.shouldDraw("a"))
    }

    @Test
    fun invalidate_thenMarkedSameKey_skipsOnSecondCall() {
        val checker = FrameDirtyChecker()
        checker.markDrawn("a")
        checker.invalidate()
        assertTrue(checker.shouldDraw("a"))
        checker.markDrawn("a")
        assertFalse(checker.shouldDraw("a"))
    }

    @Test
    fun reset_forcesDraw() {
        val checker = FrameDirtyChecker()
        checker.markDrawn("a")
        checker.reset()
        assertTrue(checker.shouldDraw("a"))
    }

    @Test
    fun multipleKeys_cycleCorrectly() {
        val checker = FrameDirtyChecker()
        assertTrue(checker.shouldDraw("1"))
        checker.markDrawn("1")
        assertFalse(checker.shouldDraw("1"))
        assertTrue(checker.shouldDraw("2"))
        checker.markDrawn("2")
        assertFalse(checker.shouldDraw("2"))
        // Switching back to a previous key is still a change.
        assertTrue(checker.shouldDraw("1"))
    }
}
