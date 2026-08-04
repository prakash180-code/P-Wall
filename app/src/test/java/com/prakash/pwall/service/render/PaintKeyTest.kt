package com.prakash.pwall.service.render

import com.prakash.pwall.data.model.ClockFont
import com.prakash.pwall.data.model.WallpaperSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PaintKeyTest {

    @Test
    fun clockKey_isDeterministic() {
        val settings = WallpaperSettings()
        assertEquals(
            PaintKey.clock(settings, 2.625f, 0xFFFFFFFF.toInt()),
            PaintKey.clock(settings, 2.625f, 0xFFFFFFFF.toInt())
        )
    }

    @Test
    fun clockKey_changesWithColor() {
        val settings = WallpaperSettings()
        assertNotEquals(
            PaintKey.clock(settings, 2.625f, 0xFFFFFFFF.toInt()),
            PaintKey.clock(settings, 2.625f, 0xFF000000.toInt())
        )
    }

    @Test
    fun clockKey_changesWithFont() {
        val settings = WallpaperSettings(clockFont = ClockFont.SERIF)
        assertNotEquals(
            PaintKey.clock(WallpaperSettings(), 2.625f, 0xFFFFFFFF.toInt()),
            PaintKey.clock(settings, 2.625f, 0xFFFFFFFF.toInt())
        )
    }

    @Test
    fun clockKey_changesWithSize() {
        val settings = WallpaperSettings()
        assertNotEquals(
            PaintKey.clock(settings, 2.0f, 0xFFFFFFFF.toInt()),
            PaintKey.clock(settings, 3.0f, 0xFFFFFFFF.toInt())
        )
    }

    @Test
    fun dateKey_differsFromClockKey() {
        val settings = WallpaperSettings()
        assertNotEquals(
            PaintKey.clock(settings, 2.625f, 0xFFFFFFFF.toInt()),
            PaintKey.date(settings, 2.625f, 0xFFFFFFFF.toInt())
        )
    }

    @Test
    fun glowKey_changesWithRadius() {
        val settings = WallpaperSettings(glassEnabled = true)
        val base = PaintKey.glow(settings, 147f, 0xFFFFFFFF.toInt(), 52f, "clock")
        val bigger = PaintKey.glow(settings, 147f, 0xFFFFFFFF.toInt(), 60f, "clock")
        assertNotEquals(base, bigger)
    }

    @Test
    fun glowKey_changesWithGlowColor() {
        val normal = WallpaperSettings(glassEnabled = true)
        val tinted = WallpaperSettings(
            glassEnabled = true,
            glassGlowColor = 0xFF123456.toLong()
        )
        assertNotEquals(
            PaintKey.glow(normal, 147f, 0xFFFFFFFF.toInt(), 52f, "clock"),
            PaintKey.glow(tinted, 147f, 0xFFFFFFFF.toInt(), 52f, "clock")
        )
    }
}
