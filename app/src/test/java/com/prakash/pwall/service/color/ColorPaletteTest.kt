package com.prakash.pwall.service.color

import com.prakash.pwall.data.model.WallpaperSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorPaletteTest {

    private fun argb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun dominant_returnsMostFrequentColor() {
        val palette = ColorPalette(listOf(argb(0x12, 0x34, 0x56), argb(0x65, 0x43, 0x21)))
        assertEquals(argb(0x12, 0x34, 0x56), palette.dominant())
    }

    @Test
    fun emptyPalette_hasSafeDefaults() {
        val palette = ColorPalette(emptyList())
        assertEquals(ColorPalette.WHITE, palette.dominant())
        assertEquals(ColorPalette.WHITE, palette.mostSaturated())
        assertEquals(0.5f, palette.averageLuminance, 0.0001f)
    }

    @Test
    fun autoClockColor_brightensDarkWallpaper() {
        val palette = ColorPalette(listOf(argb(0, 0, 0)))
        val color = palette.autoClockColor()
        assertTrue(luminanceOf(color) > 0.5f)
    }

    @Test
    fun autoClockColor_darkensBrightWallpaper() {
        val palette = ColorPalette(listOf(argb(0xFF, 0xFF, 0xFF)))
        val color = palette.autoClockColor()
        assertTrue(luminanceOf(color) < 0.5f)
    }

    @Test
    fun autoDateColor_keepsSoftTranslucency() {
        val palette = ColorPalette(listOf(argb(0xFF, 0, 0)))
        assertEquals(0xE6, (palette.autoDateColor() ushr 24) and 0xFF)
    }

    @Test
    fun premiumColors_manualColorWhenDynamicDisabled() {
        val settings = WallpaperSettings(clockColor = argb(0x11, 0x22, 0x33).toLong())
        val palette = ColorPalette(listOf(argb(0, 0, 0)))
        assertEquals(argb(0x11, 0x22, 0x33), PremiumColors.clockColor(settings, palette))
    }

    @Test
    fun premiumColors_wallpaperColorWhenDynamicEnabled() {
        val settings = WallpaperSettings(clockColor = argb(0x11, 0x22, 0x33).toLong(), dynamicClockColor = true)
        val palette = ColorPalette(listOf(argb(0, 0, 0)))
        assertTrue(PremiumColors.clockColor(settings, palette) != argb(0x11, 0x22, 0x33))
    }

    @Test
    fun premiumColors_fallsBackToManualWhenNoPalette() {
        val settings = WallpaperSettings(dateColor = argb(0x44, 0x55, 0x66).toLong(), dynamicDateColor = true)
        assertEquals(argb(0x44, 0x55, 0x66), PremiumColors.dateColor(settings, null))
    }

    private fun luminanceOf(argb: Int): Float = ColorPalette.luminance(argb)
}
