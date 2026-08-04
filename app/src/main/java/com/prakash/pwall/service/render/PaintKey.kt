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
}
