package com.prakash.pwall.service.render

import com.prakash.pwall.data.model.ContainerShape
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.model.WidgetBackgroundMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeContainerPresetsTest {

    @Test
    fun defaultSettings_matchTransparentPreset() {
        assertTrue(TimeContainerPresets.matches(WallpaperSettings(), TimeContainerPreset.TRANSPARENT))
    }

    @Test
    fun customPreset_neverMatches() {
        assertFalse(TimeContainerPresets.matches(WallpaperSettings(), TimeContainerPreset.CUSTOM))
        val custom = TimeContainerPresets.apply(WallpaperSettings(), TimeContainerPreset.DARK_CARD)
        assertFalse(TimeContainerPresets.matches(custom, TimeContainerPreset.CUSTOM))
    }

    @Test
    fun transparentPreset_onlyFlipsModeAndKeepsOtherFields() {
        val before = WallpaperSettings(widgetBackgroundOpacity = 80)
        val after = TimeContainerPresets.apply(before, TimeContainerPreset.TRANSPARENT)

        assertEquals(WidgetBackgroundMode.TRANSPARENT, after.widgetBackgroundMode)
        assertEquals(80, after.widgetBackgroundOpacity)
    }

    @Test
    fun everyPreset_isIdempotentWithItsOwnApply() {
        for (preset in TimeContainerPreset.entries) {
            if (preset == TimeContainerPreset.CUSTOM) continue
            val settings = WallpaperSettings()
            val applied = TimeContainerPresets.apply(settings, preset)
            assertTrue(
                "Preset ${preset.name} should match itself after apply",
                TimeContainerPresets.matches(applied, preset)
            )
        }
    }

    @Test
    fun darkCard_appliesSolidFillWithBorderAndShadow() {
        val applied = TimeContainerPresets.apply(WallpaperSettings(), TimeContainerPreset.DARK_CARD)

        assertEquals(WidgetBackgroundMode.SOLID, applied.widgetBackgroundMode)
        assertTrue(applied.widgetBorderEnabled)
        assertTrue(applied.widgetShadowEnabled)
        assertEquals(ContainerShape.ROUNDED, applied.widgetShape)
    }

    @Test
    fun elegant_appliesGradient() {
        val applied = TimeContainerPresets.apply(WallpaperSettings(), TimeContainerPreset.ELEGANT)

        assertEquals(WidgetBackgroundMode.GRADIENT, applied.widgetBackgroundMode)
        assertTrue(applied.widgetBackgroundColor != applied.widgetBackgroundColor2)
        assertTrue(applied.widgetBorderEnabled)
    }

    @Test
    fun frostedGlass_appliesGlassMode() {
        val applied = TimeContainerPresets.apply(WallpaperSettings(), TimeContainerPreset.FROSTED_GLASS)

        assertEquals(WidgetBackgroundMode.GLASS, applied.widgetBackgroundMode)
        assertTrue(applied.widgetGlassBlur > 20f)
        assertTrue(applied.widgetBorderEnabled)
    }

    @Test
    fun tweakingAfterPreset_reportsCustom() {
        val base = TimeContainerPresets.apply(WallpaperSettings(), TimeContainerPreset.MINIMAL)
        assertTrue(TimeContainerPresets.matches(base, TimeContainerPreset.MINIMAL))

        val tweaked = base.copy(widgetBackgroundOpacity = 33)
        assertFalse(TimeContainerPresets.matches(tweaked, TimeContainerPreset.MINIMAL))
    }

    @Test
    fun presets_preserveTextStyleAndPosition() {
        val before = WallpaperSettings(clockColor = 0xFFABCDEF.toLong(), transparency = 42)
        val applied = TimeContainerPresets.apply(before, TimeContainerPreset.GLASS)

        assertEquals(before.clockColor, applied.clockColor)
        assertEquals(before.transparency, applied.transparency)
        assertEquals(before.clockFont, applied.clockFont)
        assertEquals(before.position, applied.position)
    }
}
