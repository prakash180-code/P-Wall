package com.prakash.pwall.service.render

import com.prakash.pwall.data.model.DateLayout
import com.prakash.pwall.data.model.TimeLayout
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.model.WidgetStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The classic combination gate is the contract that keeps v1.0.1 wallpapers
 * rendering byte-for-byte through the unmodified legacy clock block. Any
 * customization must flip it to the independent widget path.
 */
class WidgetPresetsTest {

    @Test
    fun factoryDefaults_areClassicCombination() {
        assertTrue(WidgetPresets.isClassicCombination(WallpaperSettings()))
    }

    @Test
    fun classicCombination_falseWhenTimeHidden() {
        assertFalse(
            WidgetPresets.isClassicCombination(WallpaperSettings(clockVisible = false))
        )
    }

    @Test
    fun classicCombination_falseWhenDateHidden() {
        assertFalse(
            WidgetPresets.isClassicCombination(WallpaperSettings(dateVisible = false))
        )
    }

    @Test
    fun classicCombination_falseWhenTimeStyleChanged() {
        assertFalse(
            WidgetPresets.isClassicCombination(
                WallpaperSettings(timeStyle = WidgetStyle.MODERN)
            )
        )
    }

    @Test
    fun classicCombination_falseWhenDateStyleChanged() {
        assertFalse(
            WidgetPresets.isClassicCombination(
                WallpaperSettings(dateStyle = WidgetStyle.NEON)
            )
        )
    }

    @Test
    fun classicCombination_falseWhenTimeLayoutChanged() {
        assertFalse(
            WidgetPresets.isClassicCombination(
                WallpaperSettings(timeLayout = TimeLayout.VERTICAL)
            )
        )
    }

    @Test
    fun classicCombination_falseWhenDateLayoutChanged() {
        assertFalse(
            WidgetPresets.isClassicCombination(
                WallpaperSettings(dateLayout = DateLayout.COMPACT)
            )
        )
    }

    @Test
    fun classicCombination_falseWhenDateUnlinked() {
        assertFalse(
            WidgetPresets.isClassicCombination(
                WallpaperSettings(dateLinkedToTime = false)
            )
        )
    }

    @Test
    fun classicCombination_falseWhenGapScaled() {
        assertFalse(
            WidgetPresets.isClassicCombination(
                WallpaperSettings(dateGapMultiplier = 1.5f)
            )
        )
    }

    @Test
    fun apply_presetsLayoutsAndStyles() {
        val applied = WidgetPresets.apply(WallpaperSettings(), WidgetPreset.NEON)
        assertEquals(TimeLayout.CENTERED, applied.timeLayout)
        assertEquals(DateLayout.SHORT, applied.dateLayout)
        assertEquals(WidgetStyle.NEON, applied.timeStyle)
        assertEquals(WidgetStyle.NEON, applied.dateStyle)
        assertTrue(applied.dateLinkedToTime)
        assertEquals(1f, applied.dateGapMultiplier)
    }

    @Test
    fun apply_keepsUserColorsFontsAndPosition() {
        val base = WallpaperSettings(
            clockColor = 0xFF112233.toLong(),
            dateColor = 0xFF445566.toLong(),
            clockFontSizeSp = 70f,
            clockFont = com.prakash.pwall.data.model.ClockFont.MONOSPACE,
            position = com.prakash.pwall.data.model.PositionPreset.TOP_LEFT,
            positionXFraction = 0.25f,
            positionYFraction = 0.9f
        )
        val applied = WidgetPresets.apply(base, WidgetPreset.MODERN)
        assertEquals(base.clockColor, applied.clockColor)
        assertEquals(base.dateColor, applied.dateColor)
        assertEquals(base.clockFont, applied.clockFont)
        assertEquals(base.position, applied.position)
        assertEquals(base.positionXFraction, applied.positionXFraction)
    }

    @Test
    fun apply_glassPresetForcesGlassPanel() {
        assertTrue(WidgetPresets.apply(WallpaperSettings(), WidgetPreset.GLASS).glassEnabled)
    }

    @Test
    fun apply_nonGlassPresetKeepsExistingGlass() {
        val withGlass = WallpaperSettings(glassEnabled = true)
        assertTrue(WidgetPresets.apply(withGlass, WidgetPreset.NEON).glassEnabled)
    }

    @Test
    fun matches_trueAfterApplyAndFalseAfterChange() {
        val applied = WidgetPresets.apply(WallpaperSettings(), WidgetPreset.CLASSIC)
        assertTrue(WidgetPresets.matches(applied, WidgetPreset.CLASSIC))
        assertFalse(WidgetPresets.matches(applied, WidgetPreset.MODERN))
        assertTrue(
            WidgetPresets.matches(
                WallpaperSettings(
                    timeLayout = TimeLayout.SPLIT,
                    dateLayout = DateLayout.COMPACT,
                    timeStyle = WidgetStyle.FLIP_CLOCK,
                    dateStyle = WidgetStyle.FLIP_CLOCK
                ),
                WidgetPreset.FLIP
            )
        )
    }

    @Test
    fun classicRecipe_isIdentity() {
        val recipe = WidgetStyleRecipe.recipe(WidgetStyle.CLASSIC)
        assertEquals(null, recipe.family)
        assertEquals(null, recipe.bold)
        assertEquals(0f, recipe.letterSpacing)
        assertEquals(0f, recipe.glowRadiusFactor)
        assertFalse(recipe.strokeEnabled)
        assertFalse(recipe.chipEnabled)
    }

    @Test
    fun neonRecipe_hasGlow() {
        val recipe = WidgetStyleRecipe.recipe(WidgetStyle.NEON)
        assertTrue(recipe.glowRadiusFactor > 0f)
    }

    @Test
    fun outlineRecipe_hasStroke() {
        val recipe = WidgetStyleRecipe.recipe(WidgetStyle.OUTLINE)
        assertTrue(recipe.strokeEnabled)
        assertTrue(recipe.strokeWidthFactor > 0f)
    }

    @Test
    fun glassRecipe_hasChip() {
        val recipe = WidgetStyleRecipe.recipe(WidgetStyle.GLASS)
        assertTrue(recipe.chipEnabled)
        assertTrue(recipe.chipAlpha > 0f)
    }

    @Test
    fun styleDescriptions_areNonEmpty() {
        WidgetStyle.entries.forEach { style ->
            assertNotEquals("", WidgetStyleRecipe.description(style))
        }
    }
}
