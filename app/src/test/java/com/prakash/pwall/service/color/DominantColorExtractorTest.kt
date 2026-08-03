package com.prakash.pwall.service.color

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DominantColorExtractorTest {

    private fun argb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun quantize_empty_returnsEmptyList() {
        assertEquals(emptyList<Int>(), DominantColorExtractor.quantize(emptyList(), 4))
    }

    @Test
    fun quantize_allTransparent_fallsBackToWhite() {
        assertEquals(
            listOf(ColorPalette.WHITE),
            DominantColorExtractor.quantize(List(10) { 0x00000000 }, 4)
        )
    }

    @Test
    fun quantize_singleColor_returnsIt() {
        // 0x141414 is a quantization bucket center, so it survives round-trip.
        val color = argb(0x14, 0x14, 0x14)
        val result = DominantColorExtractor.quantize(List(100) { color }, 4)
        assertEquals(listOf(color), result)
    }

    @Test
    fun quantize_ordersByFrequency() {
        val pixels = List(80) { argb(0, 0, 0) } + List(20) { argb(0xFF, 0xFF, 0xFF) }
        val result = DominantColorExtractor.quantize(pixels, 4)

        assertEquals(2, result.size)
        val redOf = { c: Int -> (c ushr 16) and 0xFF }
        assertTrue(redOf(result[0]) < redOf(result[1]))
    }

    @Test
    fun quantize_mergesNearbyShadesIntoOne() {
        // Both colors land in the same quantized bucket, so they collapse to one.
        val pixels = List(60) { argb(0x14, 0x14, 0x14) } + List(40) { argb(0x15, 0x15, 0x15) }
        val result = DominantColorExtractor.quantize(pixels, 4)

        assertEquals(1, result.size)
    }

    @Test
    fun quantize_limitsResultsToMaxColors() {
        val colors = listOf(
            argb(0, 0, 0), argb(0xFF, 0, 0), argb(0, 0xFF, 0), argb(0, 0, 0xFF),
            argb(0xFF, 0xFF, 0), argb(0xFF, 0, 0xFF), argb(0, 0xFF, 0xFF)
        )
        val pixels = mutableListOf<Int>()
        colors.forEachIndexed { index, color -> repeat((index + 1) * 10) { pixels.add(color) } }

        val result = DominantColorExtractor.quantize(pixels, 4)

        assertTrue(result.size <= 4)
    }

    @Test
    fun quantize_ignoresTransparentPixels() {
        val pixels = List(50) { 0x00000000 } + List(50) { argb(0xFF, 0, 0) }
        val result = DominantColorExtractor.quantize(pixels, 4)

        assertEquals(1, result.size)
        val red = (result[0] ushr 16) and 0xFF
        assertTrue(red > 200)
    }
}
