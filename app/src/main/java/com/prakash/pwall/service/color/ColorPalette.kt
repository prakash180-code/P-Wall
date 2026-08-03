package com.prakash.pwall.service.color

import kotlin.math.max
import kotlin.math.min

/**
 * Immutable palette of the dominant colors extracted from the wallpaper image.
 * Pure ARGB ints so it can be shared by the preview, the wallpaper engine and
 * unit tests without Android/Compose dependencies.
 *
 * [colors] is ordered by frequency (most dominant first).
 */
class ColorPalette(val colors: List<Int>) {

    /** Most frequent color. */
    fun dominant(): Int = colors.firstOrNull() ?: DEFAULT_TEXT

    /** The most saturated color, used as the date accent. */
    fun mostSaturated(): Int =
        colors.maxByOrNull { saturation(it) } ?: DEFAULT_TEXT

    /** Average relative luminance (0..1) of the palette. */
    val averageLuminance: Float =
        if (colors.isEmpty()) 0.5f else colors.map { luminance(it) }.average().toFloat()

    /**
     * Readable clock color derived from the wallpaper: starts from the dominant
     * color and pushes it toward white (dark wallpapers) or black (bright
     * wallpapers) until it contrasts with the image.
     */
    fun autoClockColor(): Int {
        val base = dominant()
        val lum = luminance(base)
        val blend = when {
            lum < 0.4f -> mix(base, WHITE, 0.72f)
            lum > 0.6f -> mix(base, BLACK, 0.78f)
            else -> mix(base, WHITE, 0.5f)
        }
        return blend and 0x00FFFFFF or (0xFF shl 24)
    }

    /**
     * Accent date color derived from the most saturated wallpaper color, kept
     * bright enough to read and rendered with a soft translucency.
     */
    fun autoDateColor(): Int {
        val base = mostSaturated()
        val lum = luminance(base)
        val blended = if (lum < 0.35f) mix(base, WHITE, 0.45f) else mix(base, WHITE, 0.25f)
        return blended and 0x00FFFFFF or (0xE6 shl 24)
    }

    companion object {
        const val WHITE = 0xFFFFFFFF.toInt()
        const val BLACK = 0xFF000000.toInt()
        const val DEFAULT_TEXT = WHITE

        /** Relative luminance (0..1) of an ARGB int, weighted for the human eye. */
        fun luminance(argb: Int): Float {
            val r = ((argb ushr 16) and 0xFF) / 255f
            val g = ((argb ushr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            val lin = { c: Float ->
                if (c <= 0.03928f) c / 12.92f else Math.pow((c + 0.055).toDouble(), 2.4).toFloat()
            }
            return 0.2126f * lin(r) + 0.7152f * lin(g) + 0.0722f * lin(b)
        }

        /** HSV-style saturation (0..1). */
        fun saturation(argb: Int): Float {
            val r = ((argb ushr 16) and 0xFF) / 255f
            val g = ((argb ushr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            val mx = maxOf(r, g, b)
            val mn = minOf(r, g, b)
            return if (mx == 0f) 0f else (mx - mn) / mx
        }

        /** Linear interpolation between two ARGB ints; [t] in 0..1. */
        fun mix(a: Int, b: Int, t: Float): Int {
            val tt = t.coerceIn(0f, 1f)
            val r = (((a ushr 16) and 0xFF) + ((((b ushr 16) and 0xFF) - ((a ushr 16) and 0xFF)) * tt)).toInt()
            val g = (((a ushr 8) and 0xFF) + ((((b ushr 8) and 0xFF) - ((a ushr 8) and 0xFF)) * tt)).toInt()
            val bl = ((a and 0xFF) + (((b and 0xFF) - (a and 0xFF)) * tt)).toInt()
            return (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or bl.coerceIn(0, 255)
        }

        /** Returns [base] with [alphaPercent] (0..100) applied to its alpha channel. */
        fun withAlpha(argb: Int, alphaPercent: Int): Int {
            val alpha = ((argb ushr 24) and 0xFF) * alphaPercent.coerceIn(0, 100) / 100
            return (alpha shl 24) or (argb and 0xFFFFFF)
        }
    }
}

/**
 * Resolves the final text colors for one frame. When a dynamic color source is
 * enabled and a palette is available the wallpaper-derived color wins; otherwise
 * the user's manual color applies. Shared by the preview and the live engine so
 * both surfaces always agree.
 */
object PremiumColors {

    fun clockColor(settings: com.prakash.pwall.data.model.WallpaperSettings, palette: ColorPalette?): Int =
        if (settings.dynamicClockColor && palette != null) {
            palette.autoClockColor()
        } else {
            settings.clockColor.toInt()
        }

    fun dateColor(settings: com.prakash.pwall.data.model.WallpaperSettings, palette: ColorPalette?): Int =
        if (settings.dynamicDateColor && palette != null) {
            palette.autoDateColor()
        } else {
            settings.dateColor.toInt()
        }
}
