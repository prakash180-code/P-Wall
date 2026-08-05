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

/**
 * How the time text is arranged inside the clock block. The horizontal layout
 * keeps the classic single-line `12:45` look; the vertical layouts stack the
 * digits (and optional seconds / AM/PM marker) into separate lines.
 *
 * Legacy enum kept for backward compatibility: persisted `clock_layout` values
 * migrate into the richer [TimeLayout] (see SettingsRepository). Rendering now
 * reads [TimeLayout] only.
 */
enum class ClockLayout(val displayName: String) {
    HORIZONTAL("Horizontal"),
    VERTICAL_DIGITAL("Vertical Digital"),
    STACKED_DIGITAL("Stacked Digital"),
    COMPACT_VERTICAL("Compact Vertical")
}

/**
 * Every time-widget arrangement offered by the Clock & Date Engine. The first
 * four mirror the legacy [ClockLayout] values exactly so existing saved
 * wallpapers keep their look; the rest are new arrangements.
 */
enum class TimeLayout(val displayName: String) {
    HORIZONTAL("Horizontal"),
    VERTICAL("Vertical"),
    STACKED("Stacked"),
    COMPACT("Compact"),
    SPLIT("Split"),
    MINIMAL("Minimal"),
    CENTERED("Centered"),
    LEFT_ALIGNED("Left Aligned"),
    RIGHT_ALIGNED("Right Aligned");

    /** Legacy mirror for persisted `clock_layout` compatibility. */
    val legacyLayout: ClockLayout?
        get() = when (this) {
            HORIZONTAL -> ClockLayout.HORIZONTAL
            VERTICAL -> ClockLayout.VERTICAL_DIGITAL
            STACKED -> ClockLayout.STACKED_DIGITAL
            COMPACT -> ClockLayout.COMPACT_VERTICAL
            else -> null
        }

    companion object {
        /** Migrates a persisted legacy [ClockLayout] to its [TimeLayout]. */
        fun fromLegacy(legacy: ClockLayout): TimeLayout = when (legacy) {
            ClockLayout.HORIZONTAL -> HORIZONTAL
            ClockLayout.VERTICAL_DIGITAL -> VERTICAL
            ClockLayout.STACKED_DIGITAL -> STACKED
            ClockLayout.COMPACT_VERTICAL -> COMPACT
        }
    }
}

/**
 * Date-widget arrangements. HORIZONTAL is the classic single line driven by
 * [WallpaperSettings.dateFormat]; the rest use fixed patterns so the look is
 * predictable regardless of the chosen date format.
 */
enum class DateLayout(val displayName: String) {
    HORIZONTAL("Horizontal"),
    VERTICAL("Vertical"),
    MONTH_NAME("Month Name"),
    LONG("Long"),
    SHORT("Short"),
    DAY_FIRST("Day First"),
    COMPACT("Compact")
}

/**
 * Visual style for a clock/date widget. CLASSIC reproduces the legacy engine
 * exactly (mirrors the time typography/shadow and links below the time).
 * The other styles apply a built-in typography + glow recipe on top of the
 * widget's own font/size/color; the placeholder styles (FLIP_CLOCK, RETRO_LED,
 * TERMINAL, DIGITAL_MATRIX) are reserved for future premium packs and currently
 * render a faithful approximation so switching to them is safe.
 */
enum class WidgetStyle(val displayName: String, val isClassic: Boolean = false) {
    CLASSIC("Classic", isClassic = true),
    MINIMAL("Minimal"),
    MODERN("Modern"),
    GLASS("Glass"),
    NEON("Neon"),
    OUTLINE("Outline"),
    THIN("Thin"),
    BOLD("Bold"),
    NOTHING("Nothing"),
    PIXEL("Pixel"),
    SAMSUNG("Samsung"),
    IOS("iOS"),
    FLIP_CLOCK("Flip Clock"),
    RETRO_LED("Retro LED"),
    TERMINAL("Terminal"),
    DIGITAL_MATRIX("Digital Matrix")
}

/**
 * Coarse sensitivity presets for the 3D parallax feature. Each level maps to a
 * multiplier applied on top of the strength slider.
 */
enum class ParallaxSensitivityLevel(val displayName: String, val factor: Float) {
    LOW("Low", 0.3f),
    MEDIUM("Medium", 0.5f),
    HIGH("High", 0.75f)
}

/** App color theme for the P-Wall UI itself (independent of the wallpaper). */
enum class AppTheme(val displayName: String) {
    AUTO("Auto"),
    LIGHT("Light"),
    DARK("Dark"),
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

/** How the cinematic zoom sweeps over time. */
enum class ZoomDirection(val displayName: String) {
    ZOOM_IN("Zoom In"),
    ZOOM_OUT("Zoom Out"),
    ALTERNATE("Alternate")
}

/**
 * User control over the low-end performance mode. AUTO applies it only on
 * low-RAM / low-memory-class devices; ON/OFF force it regardless of hardware.
 */
enum class LowEndPreference(val displayName: String) {
    AUTO("Auto"),
    ON("On"),
    OFF("Off")
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
    val clockLayout: ClockLayout = ClockLayout.HORIZONTAL,
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
    // --- Clock & Date Engine (v1.1.2) ---
    // Time widget keeps using the legacy position/font/shadow/format fields
    // above; date gets its own config. Every default reproduces the classic
    // single-block render byte-for-byte (see ClockWidgetLayout).
    val clockVisible: Boolean = true,
    val dateVisible: Boolean = true,
    val timeLayout: TimeLayout = TimeLayout.HORIZONTAL,
    val dateLayout: DateLayout = DateLayout.HORIZONTAL,
    val timeStyle: WidgetStyle = WidgetStyle.CLASSIC,
    val dateStyle: WidgetStyle = WidgetStyle.CLASSIC,
    val dateLinkedToTime: Boolean = true,
    /** Scales the classic time->date gap (1f = exact classic spacing). */
    val dateGapMultiplier: Float = 1f,
    val datePosition: PositionPreset = PositionPreset.CUSTOM,
    val datePositionXFraction: Float = 0.5f,
    val datePositionYFraction: Float = 0.5f,
    /** Date font family; ignored in CLASSIC style (mirrors the time font). */
    val dateFont: ClockFont = ClockFont.DEFAULT,
    /** Date size in sp; 0 (or negative) = classic derived size (0.36 * clock). */
    val dateFontSizeSp: Float = 0f,
    val dateBold: Boolean = false,
    val dateItalic: Boolean = false,
    // Date shadow (ignored in CLASSIC style, which mirrors the time shadow).
    val dateShadowEnabled: Boolean = true,
    val dateShadowBlurRadius: Float = 6f,
    val dateShadowOffsetX: Float = 2f,
    val dateShadowOffsetY: Float = 2f,
    val dateShadowColor: Long = Color(0x99000000).toArgb().toLong(),
    /** Whether the date widget participates in breathing/transition effects. */
    val dateAnimated: Boolean = true,
    val backgroundMode: BackgroundMode = BackgroundMode.FIT,
    val backgroundZoom: Float = 1f,
    val backgroundRotationDegrees: Float = 0f,
    val backgroundTranslateXFraction: Float = 0f,
    val backgroundTranslateYFraction: Float = 0f,
    val parallaxEnabled: Boolean = false,
    val parallaxSensitivityLevel: ParallaxSensitivityLevel = ParallaxSensitivityLevel.MEDIUM,
    val parallaxStrength: Float = 0.5f,
    val parallaxSmoothing: Float = 0.5f,
    val debugParallax: Boolean = false,
    val depthEnabled: Boolean = false,
    val glassEnabled: Boolean = false,
    val glassBlurRadius: Float = 14f,
    val glassPanelOpacity: Int = 35,
    val glassCornerRadius: Float = 24f,
    val glassBorderColor: Long = Color(0x59FFFFFF).toArgb().toLong(),
    val glassBorderWidth: Float = 2f,
    val glassGlowColor: Long = Color(0xFFFFF59D).toArgb().toLong(),
    val glassGlowRadius: Float = 20f,
    val dynamicClockColor: Boolean = false,
    val dynamicDateColor: Boolean = false,
    val fadeTransitionsEnabled: Boolean = true,
    val smoothSecondsEnabled: Boolean = true,
    val breathingEnabled: Boolean = true,
    val breathingStrength: Float = 0.35f,
    val zoomEnabled: Boolean = false,
    val zoomStrength: Float = 0.5f,
    val zoomDurationSeconds: Float = 30f,
    val zoomDirection: ZoomDirection = ZoomDirection.ALTERNATE,
    val lowEnd: LowEndPreference = LowEndPreference.AUTO,
    val appTheme: AppTheme = AppTheme.AUTO,
    val customPrimaryColor: Long = 0xFF4F5B92.toLong(),
    val customSecondaryColor: Long = 0xFF5B5D72.toLong(),
    val customAccentColor: Long = 0xFF00897B.toLong()
) {
    /** Effective parallax sensitivity multiplier derived from the selected level. */
    val parallaxSensitivityValue: Float
        get() = parallaxSensitivityLevel.factor

    val clockColorValue: Color
        get() = Color(clockColor)

    val dateColorValue: Color
        get() = Color(dateColor)

    val shadowColorValue: Color
        get() = Color(shadowColor)

    val dateShadowColorValue: Color
        get() = Color(dateShadowColor)

    val glassBorderColorValue: Color
        get() = Color(glassBorderColor)

    val glassGlowColorValue: Color
        get() = Color(glassGlowColor)
}
