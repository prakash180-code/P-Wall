package com.prakash.pwall.service.render

import com.prakash.pwall.data.model.ClockFont
import com.prakash.pwall.data.model.DateLayout
import com.prakash.pwall.data.model.TimeLayout
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.model.WidgetStyle

/**
 * Built-in visual recipe for a [WidgetStyle]. Pure data (no Android/Compose
 * types) so it is JVM-testable and shared by the render engine, the live
 * wallpaper and the Compose preview. A [WidgetStyle.CLASSIC] widget never uses
 * a recipe: it reproduces the legacy engine exactly. The placeholder styles
 * (flip clock, retro LED, terminal, digital matrix) are reserved for future
 * premium packs and currently map to faithful approximations so switching to
 * them is always safe.
 *
 * Null fields mean "keep the widget's own choice" (font family, bold, italic).
 * [glowColor] null means the glow is derived from the resolved text color.
 */
data class StyleRecipe(
    val family: ClockFont? = null,
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    /** Letter spacing in em (relative to text size); 0 = none. */
    val letterSpacing: Float = 0f,
    /** Soft glow (neon / LED) under the text; radius is a factor of text size. */
    val glowColor: Long? = null,
    val glowRadiusFactor: Float = 0f,
    /** Hollow outline; stroke width is a factor of text size. */
    val strokeEnabled: Boolean = false,
    val strokeWidthFactor: Float = 0f,
    /** Frosted chip behind the text (glass look). */
    val chipEnabled: Boolean = false,
    val chipAlpha: Float = 0f,
    val chipPaddingFactor: Float = 0f
)

/** Pure recipe lookup for the widget styles. */
object WidgetStyleRecipe {

    fun recipe(style: WidgetStyle): StyleRecipe = when (style) {
        WidgetStyle.CLASSIC -> StyleRecipe()
        WidgetStyle.MINIMAL -> StyleRecipe(bold = false, letterSpacing = 0.06f)
        WidgetStyle.MODERN -> StyleRecipe(bold = true, letterSpacing = 0.04f)
        WidgetStyle.GLASS -> StyleRecipe(
            letterSpacing = 0.02f,
            chipEnabled = true,
            chipAlpha = 0.16f,
            chipPaddingFactor = 0.55f
        )
        WidgetStyle.NEON -> StyleRecipe(
            bold = true,
            letterSpacing = 0.05f,
            glowColor = null,
            glowRadiusFactor = 0.35f
        )
        WidgetStyle.OUTLINE -> StyleRecipe(
            bold = true,
            strokeEnabled = true,
            strokeWidthFactor = 0.06f
        )
        WidgetStyle.THIN -> StyleRecipe(family = ClockFont.LIGHT, bold = false)
        WidgetStyle.BOLD -> StyleRecipe(bold = true)
        WidgetStyle.NOTHING -> StyleRecipe(
            family = ClockFont.MONOSPACE,
            bold = false,
            letterSpacing = 0.1f
        )
        WidgetStyle.PIXEL -> StyleRecipe(bold = false, letterSpacing = 0.02f)
        WidgetStyle.SAMSUNG -> StyleRecipe(bold = true)
        WidgetStyle.IOS -> StyleRecipe(
            family = ClockFont.LIGHT,
            bold = false,
            letterSpacing = 0.02f
        )
        WidgetStyle.FLIP_CLOCK -> StyleRecipe(
            family = ClockFont.CONDENSED,
            bold = true,
            letterSpacing = 0.08f
        )
        WidgetStyle.RETRO_LED -> StyleRecipe(
            family = ClockFont.MONOSPACE,
            bold = true,
            letterSpacing = 0.1f,
            glowColor = null,
            glowRadiusFactor = 0.25f
        )
        WidgetStyle.TERMINAL -> StyleRecipe(
            family = ClockFont.MONOSPACE,
            letterSpacing = 0.04f
        )
        WidgetStyle.DIGITAL_MATRIX -> StyleRecipe(
            family = ClockFont.MONOSPACE,
            bold = true,
            letterSpacing = 0.06f,
            glowColor = null,
            glowRadiusFactor = 0.2f
        )
    }

    /** Short description shown in the editor UI under each style. */
    fun description(style: WidgetStyle): String = when (style) {
        WidgetStyle.CLASSIC -> "Classic look - matches the original engine."
        WidgetStyle.MINIMAL -> "Clean, light, wide-spaced digits."
        WidgetStyle.MODERN -> "Bold sans with gentle spacing."
        WidgetStyle.GLASS -> "Text on a frosted glass chip."
        WidgetStyle.NEON -> "Bright core with a soft neon halo."
        WidgetStyle.OUTLINE -> "Hollow outlined glyphs."
        WidgetStyle.THIN -> "Very light thin strokes."
        WidgetStyle.BOLD -> "Heavy bold strokes."
        WidgetStyle.NOTHING -> "Nothing-style monospace dot matrix."
        WidgetStyle.PIXEL -> "Pixel-style rounded default sans."
        WidgetStyle.SAMSUNG -> "Samsung One UI inspired bold."
        WidgetStyle.IOS -> "iOS SF Pro inspired light font."
        WidgetStyle.FLIP_CLOCK -> "Split-flap style (premium soon)."
        WidgetStyle.RETRO_LED -> "Retro LED segment glow (premium soon)."
        WidgetStyle.TERMINAL -> "Terminal monospace (premium soon)."
        WidgetStyle.DIGITAL_MATRIX -> "Digital matrix monospace (premium soon)."
    }
}

/**
 * Named bundles that configure the whole Clock & Date Engine in one tap. Each
 * preset sets the time/date styles and layouts while leaving the user's own
 * colors, fonts, positions and shadow choices untouched (style recipes drive
 * the typography look).
 */
enum class WidgetPreset(val displayName: String) {
    CLASSIC("Classic"),
    MINIMAL("Minimal"),
    MODERN("Modern"),
    NEON("Neon"),
    OUTLINE("Outline"),
    GLASS("Glass"),
    BOLD("Bold"),
    PIXEL("Pixel"),
    NOTHING("Nothing"),
    SAMSUNG("Samsung"),
    IOS("iOS"),
    FLIP("Flip"),
    RETRO_LED("Retro LED"),
    TERMINAL("Terminal"),
    MATRIX("Matrix")
}

object WidgetPresets {

    /** True when [settings] renders through the byte-identical classic path. */
    fun isClassicCombination(settings: WallpaperSettings): Boolean =
        settings.clockVisible &&
            settings.dateVisible &&
            settings.timeStyle == WidgetStyle.CLASSIC &&
            settings.dateStyle == WidgetStyle.CLASSIC &&
            settings.timeLayout == TimeLayout.HORIZONTAL &&
            settings.dateLayout == DateLayout.HORIZONTAL &&
            settings.dateLinkedToTime &&
            settings.dateGapMultiplier == 1f

    /**
     * True when [settings] carries exactly the layout/style pairing [preset]
     * applies (visibility, gap and glass are not part of the match).
     */
    fun matches(settings: WallpaperSettings, preset: WidgetPreset): Boolean {
        val applied = apply(settings, preset)
        return applied.timeLayout == settings.timeLayout &&
            applied.dateLayout == settings.dateLayout &&
            applied.timeStyle == settings.timeStyle &&
            applied.dateStyle == settings.dateStyle
    }

    /** Layout/style pairing applied by a [WidgetPreset]. */
    private data class PresetConfig(
        val timeLayout: TimeLayout,
        val dateLayout: DateLayout,
        val timeStyle: WidgetStyle,
        val dateStyle: WidgetStyle
    )

    fun apply(settings: WallpaperSettings, preset: WidgetPreset): WallpaperSettings {
        val config = when (preset) {
            WidgetPreset.CLASSIC ->
                PresetConfig(
                    TimeLayout.HORIZONTAL, DateLayout.HORIZONTAL,
                    WidgetStyle.CLASSIC, WidgetStyle.CLASSIC
                )
            WidgetPreset.MINIMAL ->
                PresetConfig(TimeLayout.MINIMAL, DateLayout.COMPACT, WidgetStyle.MINIMAL, WidgetStyle.MINIMAL)
            WidgetPreset.MODERN ->
                PresetConfig(TimeLayout.HORIZONTAL, DateLayout.LONG, WidgetStyle.MODERN, WidgetStyle.MODERN)
            WidgetPreset.NEON ->
                PresetConfig(TimeLayout.CENTERED, DateLayout.SHORT, WidgetStyle.NEON, WidgetStyle.NEON)
            WidgetPreset.OUTLINE ->
                PresetConfig(TimeLayout.SPLIT, DateLayout.COMPACT, WidgetStyle.OUTLINE, WidgetStyle.OUTLINE)
            WidgetPreset.GLASS ->
                PresetConfig(
                    TimeLayout.HORIZONTAL, DateLayout.HORIZONTAL,
                    WidgetStyle.GLASS, WidgetStyle.GLASS
                )
            WidgetPreset.BOLD ->
                PresetConfig(TimeLayout.STACKED, DateLayout.VERTICAL, WidgetStyle.BOLD, WidgetStyle.BOLD)
            WidgetPreset.PIXEL ->
                PresetConfig(TimeLayout.HORIZONTAL, DateLayout.COMPACT, WidgetStyle.PIXEL, WidgetStyle.PIXEL)
            WidgetPreset.NOTHING ->
                PresetConfig(TimeLayout.VERTICAL, DateLayout.COMPACT, WidgetStyle.NOTHING, WidgetStyle.NOTHING)
            WidgetPreset.SAMSUNG ->
                PresetConfig(TimeLayout.HORIZONTAL, DateLayout.LONG, WidgetStyle.SAMSUNG, WidgetStyle.SAMSUNG)
            WidgetPreset.IOS ->
                PresetConfig(TimeLayout.HORIZONTAL, DateLayout.COMPACT, WidgetStyle.IOS, WidgetStyle.IOS)
            WidgetPreset.FLIP ->
                PresetConfig(TimeLayout.SPLIT, DateLayout.COMPACT, WidgetStyle.FLIP_CLOCK, WidgetStyle.FLIP_CLOCK)
            WidgetPreset.RETRO_LED ->
                PresetConfig(TimeLayout.STACKED, DateLayout.COMPACT, WidgetStyle.RETRO_LED, WidgetStyle.RETRO_LED)
            WidgetPreset.TERMINAL ->
                PresetConfig(TimeLayout.VERTICAL, DateLayout.COMPACT, WidgetStyle.TERMINAL, WidgetStyle.TERMINAL)
            WidgetPreset.MATRIX ->
                PresetConfig(TimeLayout.SPLIT, DateLayout.COMPACT, WidgetStyle.DIGITAL_MATRIX, WidgetStyle.DIGITAL_MATRIX)
        }
        return settings.copy(
            clockVisible = true,
            dateVisible = true,
            timeLayout = config.timeLayout,
            dateLayout = config.dateLayout,
            timeStyle = config.timeStyle,
            dateStyle = config.dateStyle,
            dateLinkedToTime = true,
            dateGapMultiplier = 1f,
            glassEnabled = settings.glassEnabled || preset == WidgetPreset.GLASS
        )
    }
}
