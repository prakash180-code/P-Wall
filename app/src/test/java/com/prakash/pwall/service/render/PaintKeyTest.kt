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

    @Test
    fun widgetKey_isDeterministic() {
        val key1 = PaintKey.widget(
            "time", ClockFont.DEFAULT.name, false, false, 147f,
            0xFFFFFFFF.toInt(), 100, true, 6f, 2f, 2f, 0x99000000.toInt(),
            0.04f, false, 0f
        )
        val key2 = PaintKey.widget(
            "time", ClockFont.DEFAULT.name, false, false, 147f,
            0xFFFFFFFF.toInt(), 100, true, 6f, 2f, 2f, 0x99000000.toInt(),
            0.04f, false, 0f
        )
        assertEquals(key1, key2)
    }

    @Test
    fun widgetKey_changesWithFamilyWeightAndSpacing() {
        val base = listOf(
            "time", ClockFont.DEFAULT.name, false, false, 147f,
            0xFFFFFFFF.toInt(), 100, true, 6f, 2f, 2f, 0x99000000.toInt(),
            0.04f, false, 0f
        )
        fun key(vararg parts: Any): String = PaintKey.widget(
            parts[0] as String, parts[1] as String, parts[2] as Boolean, parts[3] as Boolean,
            parts[4] as Float, parts[5] as Int, parts[6] as Int, parts[7] as Boolean,
            parts[8] as Float, parts[9] as Float, parts[10] as Float, parts[11] as Int,
            parts[12] as Float, parts[13] as Boolean, parts[14] as Float
        )
        assertNotEquals(key(*base.toTypedArray()), key(*(base.toMutableList().apply { this[1] = ClockFont.SERIF.name }).toTypedArray()))
        assertNotEquals(key(*base.toTypedArray()), key(*(base.toMutableList().apply { this[2] = true }).toTypedArray()))
        assertNotEquals(key(*base.toTypedArray()), key(*(base.toMutableList().apply { this[12] = 0.5f }).toTypedArray()))
    }

    @Test
    fun widgetKey_timeAndDateDiffer() {
        assertNotEquals(
            PaintKey.widget(
                "time", ClockFont.DEFAULT.name, false, false, 147f,
                0xFFFFFFFF.toInt(), 100, true, 6f, 2f, 2f, 0x99000000.toInt(),
                0f, false, 0f
            ),
            PaintKey.widget(
                "date", ClockFont.DEFAULT.name, false, false, 53f,
                0xFFFFFFFF.toInt(), 100, true, 6f, 2f, 2f, 0x99000000.toInt(),
                0f, false, 0f
            )
        )
    }

    @Test
    fun widgetGlowKey_changesWithRadiusAndColor() {
        val base = PaintKey.widgetGlow("time", 0xFFFFFFFF.toInt(), 147f, 51f, 0xFF00FF00.toInt())
        assertNotEquals(base, PaintKey.widgetGlow("time", 0xFFFFFFFF.toInt(), 147f, 70f, 0xFF00FF00.toInt()))
        assertNotEquals(base, PaintKey.widgetGlow("time", 0xFFFFFFFF.toInt(), 147f, 51f, 0xFFFF0000.toInt()))
    }
}
