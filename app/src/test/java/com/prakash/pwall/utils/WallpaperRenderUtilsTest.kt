package com.prakash.pwall.utils

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperRenderUtilsTest {

    @Test
    fun clampBlockTopLeft_insideArea_isUnchanged() {
        val (x, y) = clampBlockTopLeft(100f, 100f, 200f, 50f, 1000f, 2000f)
        assertEquals(100f, x, 0.001f)
        assertEquals(100f, y, 0.001f)
    }

    @Test
    fun clampBlockTopLeft_offLeftEdge_isClampedToZero() {
        val (x, _) = clampBlockTopLeft(-40f, 100f, 200f, 50f, 1000f, 2000f)
        assertEquals(0f, x, 0.001f)
    }

    @Test
    fun clampBlockTopLeft_offRightEdge_staysInside() {
        val (x, _) = clampBlockTopLeft(900f, 100f, 200f, 50f, 1000f, 2000f)
        assertEquals(800f, x, 0.001f)
    }

    @Test
    fun clampBlockTopLeft_offBottomEdge_staysInside() {
        val (_, y) = clampBlockTopLeft(100f, 1970f, 200f, 50f, 1000f, 2000f)
        assertEquals(1950f, y, 0.001f)
    }

    @Test
    fun clampBlockTopLeft_widerBlockIsHorizontallyCentered() {
        val (x, y) = clampBlockTopLeft(0f, 0f, 1200f, 60f, 1000f, 2000f)
        assertEquals(-100f, x, 0.001f)
        assertEquals(0f, y, 0.001f)
    }

    @Test
    fun clampBlockTopLeft_blockLargerThanAreaInBothAxes_isCentered() {
        val (x, y) = clampBlockTopLeft(0f, 0f, 1200f, 3000f, 1000f, 2000f)
        assertEquals(-100f, x, 0.001f)
        assertEquals(-500f, y, 0.001f)
    }

    @Test
    fun applyTransparency_fullKeepsAlpha() {
        val result = applyTransparency(Color.White.copy(alpha = 0.8f), 100)
        assertEquals(0.8f, result.alpha, 0.001f)
    }

    @Test
    fun applyTransparency_halfScalesAlpha() {
        val result = applyTransparency(Color.White.copy(alpha = 0.8f), 50)
        assertEquals(0.4f, result.alpha, 0.001f)
    }

    @Test
    fun applyTransparency_zeroIsTransparent() {
        val result = applyTransparency(Color.White.copy(alpha = 1f), 0)
        assertEquals(0f, result.alpha, 0.001f)
    }

    @Test
    fun applyTransparency_clampsOverRange() {
        val over = applyTransparency(Color.White.copy(alpha = 1f), 150)
        assertEquals(1f, over.alpha, 0.001f)
        val under = applyTransparency(Color.White.copy(alpha = 1f), -20)
        assertEquals(0f, under.alpha, 0.001f)
    }
}
