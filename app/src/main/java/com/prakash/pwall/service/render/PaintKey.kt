package com.prakash.pwall.service.render

import com.prakash.pwall.data.model.WallpaperSettings

/**
 * Stable, unique signatures for the painted text elements of the wallpaper.
 * Two settings values that render identical pixels share one key (and one
 * cached [android.graphics.Paint]); any change produces a different key so the
 * paint cache stays correct. Pure string building, JVM-testable.
 */
object PaintKey {

    /** Key for the clock time text paint. [color] is the resolved per-frame color. */
    fun clock(settings: WallpaperSettings, density: Float, color: Int): String =
        textKey("clock", settings, density * settings.clockFontSizeSp, color)

    /** Key for the date text paint. [color] is the resolved per-frame color. */
    fun date(settings: WallpaperSettings, density: Float, color: Int): String =
        textKey("date", settings, density * settings.clockFontSizeSp * 0.36f, color)

    /**
     * Key for the soft glow (halo) paint drawn under the text when the glass
     * feature is enabled. Encodes the resolved text size/color plus the glow
     * configuration so a changed setting produces a fresh glow paint.
     */
    fun glow(
        settings: WallpaperSettings,
        textSizePx: Float,
        color: Int,
        radiusPx: Float,
        purpose: String
    ): String = buildString {
        append("glow|")
        append(purpose)
        append('|').append(settings.glassEnabled)
        append('|').append(radiusPx)
        append('|').append(settings.glassGlowColor.toInt())
        append('|').append(settings.clockFont.name)
        append('|').append(settings.clockBold)
        append('|').append(settings.clockItalic)
        append('|').append(settings.transparency)
        append('|').append(textSizePx)
        append('|').append(color)
    }

    /** Key for any text paint given its resolved pixel size. */
    fun textKey(
        purpose: String,
        settings: WallpaperSettings,
        textSizePx: Float,
        color: Int
    ): String = buildString {
        append(purpose)
        append('|').append(settings.clockFont.name)
        append('|').append(settings.clockBold)
        append('|').append(settings.clockItalic)
        append('|').append(settings.transparency)
        append('|').append(settings.shadowEnabled)
        append('|').append(settings.shadowBlurRadius)
        append('|').append(settings.shadowOffsetX)
        append('|').append(settings.shadowOffsetY)
        append('|').append(settings.shadowColor.toInt())
        append('|').append(textSizePx)
        append('|').append(color)
    }

    /**
     * Key for a Clock & Date Engine widget paint. Encodes every property that
     * changes the rendered glyphs (family, weight, spacing, stroke) on top of
     * the shared text properties so a settings or style change always produces
     * a fresh paint.
     */
    fun widget(
        purpose: String,
        familyName: String,
        bold: Boolean,
        italic: Boolean,
        textSizePx: Float,
        color: Int,
        transparency: Int,
        shadowEnabled: Boolean,
        shadowBlurRadius: Float,
        shadowOffsetX: Float,
        shadowOffsetY: Float,
        shadowColor: Int,
        letterSpacing: Float,
        strokeEnabled: Boolean,
        strokeWidthPx: Float
    ): String = buildString {
        append("widget|").append(purpose)
        append('|').append(familyName)
        append('|').append(bold)
        append('|').append(italic)
        append('|').append(transparency)
        append('|').append(shadowEnabled)
        append('|').append(shadowBlurRadius)
        append('|').append(shadowOffsetX)
        append('|').append(shadowOffsetY)
        append('|').append(shadowColor)
        append('|').append(letterSpacing)
        append('|').append(strokeEnabled)
        append('|').append(strokeWidthPx)
        append('|').append(textSizePx)
        append('|').append(color)
    }

    /** Key for a style-driven glow (neon / LED) paint. */
    fun widgetGlow(
        purpose: String,
        color: Int,
        textSizePx: Float,
        radiusPx: Float,
        glowColor: Int
    ): String = buildString {
        append("widget-glow|").append(purpose)
        append('|').append(radiusPx)
        append('|').append(glowColor)
        append('|').append(textSizePx)
        append('|').append(color)
    }
}
