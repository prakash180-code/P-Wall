package com.prakash.pwall.data.repository

import com.prakash.pwall.data.model.AppTheme
import com.prakash.pwall.data.model.BackgroundMode
import com.prakash.pwall.data.model.ClockFont
import com.prakash.pwall.data.model.ClockLayout
import com.prakash.pwall.data.model.DateFormat
import com.prakash.pwall.data.model.LowEndPreference
import com.prakash.pwall.data.model.ParallaxSensitivityLevel
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.TimeFormat
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.model.ZoomDirection

/**
 * Dependency-free JSON codec for [WallpaperSettings], used for the backup /
 * restore feature. The format is a flat JSON object of scalar values so it can
 * be exchanged between devices (enums are stored by [Enum.name]). The selected
 * image path is intentionally omitted: the image itself lives in app-private
 * storage on each device.
 */
object SettingsBackup {

    private data class Entry(val raw: String, val isString: Boolean)

    fun encode(settings: WallpaperSettings): String {
        val fields = mutableListOf<Pair<String, Entry>>()
        fun string(key: String, value: String?) {
            if (value != null) fields += key to Entry(escape(value), true)
        }

        fun bool(key: String, value: Boolean) = fields.add(key to Entry(value.toString(), false))
        fun int(key: String, value: Int) = fields.add(key to Entry(value.toString(), false))
        fun float(key: String, value: Float) = fields.add(key to Entry(value.toString(), false))
        fun long(key: String, value: Long) = fields.add(key to Entry(value.toString(), false))
        fun enum(key: String, value: Enum<*>) = fields.add(key to Entry(value.name, false))

        bool("show_seconds", settings.showSeconds)
        bool("clock_bold", settings.clockBold)
        bool("clock_italic", settings.clockItalic)
        bool("shadow_enabled", settings.shadowEnabled)
        bool("parallax_enabled", settings.parallaxEnabled)
        bool("debug_parallax", settings.debugParallax)
        bool("depth_enabled", settings.depthEnabled)
        bool("glass_enabled", settings.glassEnabled)
        bool("dynamic_clock_color", settings.dynamicClockColor)
        bool("dynamic_date_color", settings.dynamicDateColor)
        bool("fade_transitions_enabled", settings.fadeTransitionsEnabled)
        bool("smooth_seconds_enabled", settings.smoothSecondsEnabled)
        bool("breathing_enabled", settings.breathingEnabled)
        bool("zoom_enabled", settings.zoomEnabled)
        int("transparency", settings.transparency)
        int("glass_panel_opacity", settings.glassPanelOpacity)
        float("clock_font_size_sp", settings.clockFontSizeSp)
        float("shadow_blur_radius", settings.shadowBlurRadius)
        float("shadow_offset_x", settings.shadowOffsetX)
        float("shadow_offset_y", settings.shadowOffsetY)
        float("position_x_fraction", settings.positionXFraction)
        float("position_y_fraction", settings.positionYFraction)
        float("background_zoom", settings.backgroundZoom)
        float("background_rotation_degrees", settings.backgroundRotationDegrees)
        float("background_translate_x_fraction", settings.backgroundTranslateXFraction)
        float("background_translate_y_fraction", settings.backgroundTranslateYFraction)
        float("parallax_strength", settings.parallaxStrength)
        float("parallax_smoothing", settings.parallaxSmoothing)
        float("glass_blur_radius", settings.glassBlurRadius)
        float("glass_corner_radius", settings.glassCornerRadius)
        float("glass_border_width", settings.glassBorderWidth)
        float("glass_glow_radius", settings.glassGlowRadius)
        float("breathing_strength", settings.breathingStrength)
        float("zoom_strength", settings.zoomStrength)
        float("zoom_duration_seconds", settings.zoomDurationSeconds)
        long("clock_color", settings.clockColor)
        long("date_color", settings.dateColor)
        long("shadow_color", settings.shadowColor)
        long("glass_border_color", settings.glassBorderColor)
        long("glass_glow_color", settings.glassGlowColor)
        long("custom_primary_color", settings.customPrimaryColor)
        long("custom_secondary_color", settings.customSecondaryColor)
        long("custom_accent_color", settings.customAccentColor)
        enum("time_format", settings.timeFormat)
        enum("date_format", settings.dateFormat)
        enum("clock_layout", settings.clockLayout)
        enum("clock_font", settings.clockFont)
        enum("position", settings.position)
        enum("background_mode", settings.backgroundMode)
        enum("parallax_sensitivity_level", settings.parallaxSensitivityLevel)
        enum("zoom_direction", settings.zoomDirection)
        enum("low_end", settings.lowEnd)
        enum("app_theme", settings.appTheme)

        return buildString {
            append('{')
            fields.forEachIndexed { index, (key, entry) ->
                if (index > 0) append(',')
                append('"').append(key).append('"').append(':')
                if (entry.isString) append('"').append(entry.raw).append('"')
                else append(entry.raw)
            }
            append('}')
        }
    }

    /**
     * Decodes a backup produced by [encode]. Returns null when the payload is
     * not a valid flat JSON object.
     */
    fun decode(json: String): WallpaperSettings? {
        val values = parseFlatObject(json) ?: return null
        val defaults = WallpaperSettings()

        fun str(key: String): String? = values[key]
        fun bool(key: String, default: Boolean): Boolean =
            values[key]?.toBooleanStrictOrNull() ?: default
        fun int(key: String, default: Int): Int = values[key]?.toIntOrNull() ?: default
        fun float(key: String, default: Float): Float = values[key]?.toFloatOrNull() ?: default
        fun long(key: String, default: Long): Long = values[key]?.toLongOrNull() ?: default
        fun <T : Enum<T>> enum(key: String, all: List<T>, default: T): T =
            all.firstOrNull { it.name == values[key] } ?: default

        return WallpaperSettings(
            timeFormat = enum("time_format", TimeFormat.entries, defaults.timeFormat),
            showSeconds = bool("show_seconds", defaults.showSeconds),
            dateFormat = enum("date_format", DateFormat.entries, defaults.dateFormat),
            clockLayout = enum("clock_layout", ClockLayout.entries, defaults.clockLayout),
            clockFont = enum("clock_font", ClockFont.entries, defaults.clockFont),
            clockFontSizeSp = float("clock_font_size_sp", defaults.clockFontSizeSp),
            clockBold = bool("clock_bold", defaults.clockBold),
            clockItalic = bool("clock_italic", defaults.clockItalic),
            clockColor = long("clock_color", defaults.clockColor),
            dateColor = long("date_color", defaults.dateColor),
            shadowEnabled = bool("shadow_enabled", defaults.shadowEnabled),
            shadowBlurRadius = float("shadow_blur_radius", defaults.shadowBlurRadius),
            shadowOffsetX = float("shadow_offset_x", defaults.shadowOffsetX),
            shadowOffsetY = float("shadow_offset_y", defaults.shadowOffsetY),
            shadowColor = long("shadow_color", defaults.shadowColor),
            transparency = int("transparency", defaults.transparency),
            position = enum("position", PositionPreset.entries, defaults.position),
            positionXFraction = float("position_x_fraction", defaults.positionXFraction),
            positionYFraction = float("position_y_fraction", defaults.positionYFraction),
            backgroundMode = enum("background_mode", BackgroundMode.entries, defaults.backgroundMode),
            backgroundZoom = float("background_zoom", defaults.backgroundZoom),
            backgroundRotationDegrees =
                float("background_rotation_degrees", defaults.backgroundRotationDegrees),
            backgroundTranslateXFraction =
                float("background_translate_x_fraction", defaults.backgroundTranslateXFraction),
            backgroundTranslateYFraction =
                float("background_translate_y_fraction", defaults.backgroundTranslateYFraction),
            parallaxEnabled = bool("parallax_enabled", defaults.parallaxEnabled),
            parallaxSensitivityLevel = enum(
                "parallax_sensitivity_level",
                ParallaxSensitivityLevel.entries,
                defaults.parallaxSensitivityLevel
            ),
            parallaxStrength = float("parallax_strength", defaults.parallaxStrength),
            parallaxSmoothing = float("parallax_smoothing", defaults.parallaxSmoothing),
            debugParallax = bool("debug_parallax", defaults.debugParallax),
            depthEnabled = bool("depth_enabled", defaults.depthEnabled),
            glassEnabled = bool("glass_enabled", defaults.glassEnabled),
            glassBlurRadius = float("glass_blur_radius", defaults.glassBlurRadius),
            glassPanelOpacity = int("glass_panel_opacity", defaults.glassPanelOpacity),
            glassCornerRadius = float("glass_corner_radius", defaults.glassCornerRadius),
            glassBorderColor = long("glass_border_color", defaults.glassBorderColor),
            glassBorderWidth = float("glass_border_width", defaults.glassBorderWidth),
            glassGlowColor = long("glass_glow_color", defaults.glassGlowColor),
            glassGlowRadius = float("glass_glow_radius", defaults.glassGlowRadius),
            dynamicClockColor = bool("dynamic_clock_color", defaults.dynamicClockColor),
            dynamicDateColor = bool("dynamic_date_color", defaults.dynamicDateColor),
            fadeTransitionsEnabled = bool("fade_transitions_enabled", defaults.fadeTransitionsEnabled),
            smoothSecondsEnabled = bool("smooth_seconds_enabled", defaults.smoothSecondsEnabled),
            breathingEnabled = bool("breathing_enabled", defaults.breathingEnabled),
            breathingStrength = float("breathing_strength", defaults.breathingStrength),
            zoomEnabled = bool("zoom_enabled", defaults.zoomEnabled),
            zoomStrength = float("zoom_strength", defaults.zoomStrength),
            zoomDurationSeconds = float("zoom_duration_seconds", defaults.zoomDurationSeconds),
            zoomDirection = enum("zoom_direction", ZoomDirection.entries, defaults.zoomDirection),
            lowEnd = enum("low_end", LowEndPreference.entries, defaults.lowEnd),
            appTheme = enum("app_theme", AppTheme.entries, defaults.appTheme),
            customPrimaryColor = long("custom_primary_color", defaults.customPrimaryColor),
            customSecondaryColor = long("custom_secondary_color", defaults.customSecondaryColor),
            customAccentColor = long("custom_accent_color", defaults.customAccentColor)
        )
    }

    /**
     * Parses a flat `{"key":value,...}` object into a map of raw string values.
     * Handles string escaping (\\, \", \/, \n, \t, \uXXXX) for string values
     * and returns the literal text for numbers/booleans. Returns null on any
     * structural error.
     */
    private fun parseFlatObject(json: String): Map<String, String>? {
        val text = json.trim()
        if (!text.startsWith("{") || !text.endsWith("}")) return null
        val result = mutableMapOf<String, String>()
        var i = 1
        val n = text.length
        while (i < n) {
            while (i < n && (text[i].isWhitespace() || text[i] == ',')) i++
            if (i >= n || text[i] == '}') break
            if (text[i] != '"') return null
            val (key, next) = readQuoted(text, i) ?: return null
            i = next
            while (i < n && text[i].isWhitespace()) i++
            if (i >= n || text[i] != ':') return null
            i++
            while (i < n && text[i].isWhitespace()) i++
            if (i >= n) return null
            when {
                text[i] == '"' -> {
                    val (value, after) = readQuoted(text, i) ?: return null
                    result[key] = value
                    i = after
                }
                text[i] == '{' || text[i] == '[' -> return null
                else -> {
                    var j = i
                    while (j < n && text[j] != ',' && text[j] != '}') j++
                    result[key] = text.substring(i, j).trim()
                    i = j
                }
            }
        }
        return result
    }

    /** Reads a quoted string starting at [start] (which must be a quote). */
    private fun readQuoted(text: String, start: Int): Pair<String, Int>? {
        val n = text.length
        val sb = StringBuilder()
        var i = start + 1
        while (i < n) {
            when (val c = text[i]) {
                '"' -> return sb.toString() to (i + 1)
                '\\' -> {
                    if (i + 1 >= n) return null
                    when (val esc = text[i + 1]) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'r' -> sb.append('\r')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'u' -> {
                            if (i + 5 >= n) return null
                            val hex = text.substring(i + 2, i + 6)
                            sb.append(hex.toIntOrNull(16)?.toChar() ?: return null)
                            i += 4
                        }
                        else -> return null
                    }
                    i += 2
                }
                else -> sb.append(c)
            }
            i++
        }
        return null
    }

    private fun escape(value: String): String = buildString {
        value.forEach { c ->
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
    }
}
