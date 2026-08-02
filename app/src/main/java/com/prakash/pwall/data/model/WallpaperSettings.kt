package com.prakash.pwall.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** 12/24 hour clock format. */
enum class TimeFormat(val displayName: String, val is24Hour: Boolean) {
    HOUR_12("12 Hour", false),
    HOUR_24("24 Hour", true)
}

/** Available date formats. Patterns use java.time DateTimeFormatter syntax. */
enum class DateFormat(val displayName: String, val pattern: String, val showDay: Boolean = false) {
    DAY_MONTH_YEAR("15 July 2026", "d MMMM yyyy"),
    NUMERIC_DMY("15/07/2026", "dd/MM/yyyy"),
    DAY_ONLY("Tuesday", "EEEE", showDay = true),
    DAY_DATE("Tuesday, 15 July", "EEEE, d MMMM", showDay = true)
}

/** Preset anchors for the clock/date overlay. */
enum class PositionPreset(val displayName: String) {
    CENTER("Center"),
    TOP_LEFT("Top Left"),
    TOP_RIGHT("Top Right"),
    BOTTOM_LEFT("Bottom Left"),
    BOTTOM_RIGHT("Bottom Right"),
    BOTTOM_CENTER("Bottom Center"),
    CUSTOM("Custom")
}

/** How the wallpaper image fills the screen. */
enum class BackgroundMode(val displayName: String) {
    FIT("Fit"),
    FILL("Fill"),
    STRETCH("Stretch"),
    CENTER_CROP("Center Crop"),
    CUSTOM("Custom")
}

/** System font families exposed for the clock. */
enum class ClockFont(val displayName: String, val familyName: String) {
    DEFAULT("Default", "sans-serif"),
    LIGHT("Light", "sans-serif-light"),
    CONDENSED("Condensed", "sans-serif-condensed"),
    SERIF("Serif", "serif"),
    MONOSPACE("Monospace", "monospace"),
    CURSIVE("Cursive", "cursive")
}

/**
 * Immutable snapshot of every user-customizable wallpaper option.
 * Persisted via DataStore and read by both the preview and the live wallpaper.
 */
data class WallpaperSettings(
    val selectedImagePath: String? = null,
    val timeFormat: TimeFormat = TimeFormat.HOUR_24,
    val showSeconds: Boolean = true,
    val dateFormat: DateFormat = DateFormat.DAY_MONTH_YEAR,
    val clockFont: ClockFont = ClockFont.DEFAULT,
    val clockFontSizeSp: Float = 56f,
    val clockBold: Boolean = false,
    val clockItalic: Boolean = false,
    val clockColor: Long = Color.White.toArgb().toLong(),
    val dateColor: Long = Color(0xE6FFFFFF).toArgb().toLong(),
    val shadowEnabled: Boolean = true,
    val shadowBlurRadius: Float = 6f,
    val shadowOffsetX: Float = 2f,
    val shadowOffsetY: Float = 2f,
    val shadowColor: Long = Color(0x99000000).toArgb().toLong(),
    val transparency: Int = 100,
    val position: PositionPreset = PositionPreset.BOTTOM_CENTER,
    val positionXFraction: Float = 0.5f,
    val positionYFraction: Float = 0.88f,
    val backgroundMode: BackgroundMode = BackgroundMode.FIT,
    val backgroundZoom: Float = 1f,
    val backgroundRotationDegrees: Float = 0f,
    val backgroundTranslateXFraction: Float = 0f,
    val backgroundTranslateYFraction: Float = 0f
) {
    val clockColorValue: Color
        get() = Color(clockColor)

    val dateColorValue: Color
        get() = Color(dateColor)

    val shadowColorValue: Color
        get() = Color(shadowColor)
}
