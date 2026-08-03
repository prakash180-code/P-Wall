package com.prakash.pwall.service.depth

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskOpsTest {

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }

    private fun alphaAt(alpha: ByteArray, width: Int, row: Int, col: Int): Int =
        (alpha[row * width + col].toInt() and 0xFF)

    @Test
    fun expand_growsOpaqueRegionToFill() {
        // Single opaque center pixel; radius 1 reaches every pixel (3x3).
        val alpha = bytes(
            0, 0, 0,
            0, 255, 0,
            0, 0, 0
        )
        val out = MaskOps.expand(alpha, 3, 3, 1)
        assertTrue(out.all { (it.toInt() and 0xFF) == 255 })
    }

    @Test
    fun expand_respectsRadiusBounds() {
        // A corner pixel at radius 1 grows exactly one step in each direction,
        // not beyond (checked against the corner-adjacent diagonal cell).
        val alpha = bytes(
            255, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0
        )
        val out = MaskOps.expand(alpha, 5, 5, 1)
        assertEquals(255, alphaAt(out, 5, 0, 0))
        assertEquals(255, alphaAt(out, 5, 0, 1))
        assertEquals(255, alphaAt(out, 5, 1, 0))
        assertEquals(255, alphaAt(out, 5, 1, 1))
        assertEquals(0, alphaAt(out, 5, 2, 2))
    }

    @Test
    fun shrink_removesOpaqueIsland() {
        val alpha = bytes(
            0, 0, 0,
            0, 255, 0,
            0, 0, 0
        )
        val out = MaskOps.shrink(alpha, 3, 3, 1)
        assertTrue(out.all { (it.toInt() and 0xFF) == 0 })
    }

    @Test
    fun shrink_keepsThickCore() {
        // A 3x3 solid block on a 5x5 grid: radius 1 erosion keeps the center pixel.
        val alpha = bytes(
            0, 0, 0, 0, 0,
            0, 255, 255, 255, 0,
            0, 255, 255, 255, 0,
            0, 255, 255, 255, 0,
            0, 0, 0, 0, 0
        )
        val out = MaskOps.shrink(alpha, 5, 5, 1)
        assertEquals(255, alphaAt(out, 5, 2, 2))
        assertEquals(0, alphaAt(out, 5, 1, 1))
        assertEquals(0, alphaAt(out, 5, 0, 0))
    }

    @Test
    fun zeroRadius_isNoOpForRankOps() {
        val alpha = bytes(0, 255, 128)
        assertArrayEquals(alpha, MaskOps.expand(alpha, 3, 1, 0))
        assertArrayEquals(alpha, MaskOps.shrink(alpha, 3, 1, 0))
    }

    @Test
    fun feather_producesIntermediateAlpha() {
        // Single opaque center on a 5x5 grid: the blur is brightest in the middle
        // and fades to zero at the corners (in-between values => soft edge).
        val alpha = bytes(
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 255, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0
        )
        val out = MaskOps.feather(alpha, 5, 5, 1)
        val center = alphaAt(out, 5, 2, 2)
        val adjacent = alphaAt(out, 5, 1, 2)
        assertEquals(0, alphaAt(out, 5, 0, 0))
        assertTrue(center in 1 until 255)
        assertTrue(adjacent in 1 until 255)
        assertTrue(center >= adjacent)
    }

    @Test
    fun smooth_outputsOnlyBinaryValues() {
        val alpha = bytes(
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 255, 255, 255,
            0, 0, 255, 255, 255,
            0, 0, 255, 255, 255
        )
        val out = MaskOps.smooth(alpha, 5, 5, 1)
        out.forEach { value ->
            val v = value.toInt() and 0xFF
            assertTrue(v == 0 || v == 255)
        }
        // Interior of the block survives the blur + threshold.
        assertEquals(255, alphaAt(out, 5, 3, 3))
    }

    @Test
    fun feather_zeroRadius_isNoOp() {
        val alpha = bytes(0, 255, 128)
        assertArrayEquals(alpha, MaskOps.feather(alpha, 3, 1, 0))
    }
}
