package com.prakash.pwall.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.prakash.pwall.data.model.BackgroundMode
import com.prakash.pwall.data.model.ClockFont
import com.prakash.pwall.data.model.DateFormat
import com.prakash.pwall.data.model.LowEndPreference
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.TimeFormat
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.model.ZoomDirection
import com.prakash.pwall.data.storage.ImageStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Single source of truth for all user preferences, backed by DataStore.
 *
 * Exposes an immutable [WallpaperSettings] snapshot as a [Flow] and provides a
 * transform-based update API so callers never touch raw preferences.
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val imageStore: ImageStore
) {

    val settings: Flow<WallpaperSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs.toSettings() }

    /** Atomically applies [transform] to the current settings and persists it. */
    suspend fun updateSettings(transform: (WallpaperSettings) -> WallpaperSettings) {
        dataStore.edit { prefs ->
            transform(prefs.toSettings()).writeTo(prefs)
        }
    }

    private fun Preferences.toSettings(): WallpaperSettings {
        return WallpaperSettings(
            selectedImagePath = imageStore.imagePath,
            timeFormat = TimeFormat.entries.firstOrNull { it.name == stringPreference(TIME_FORMAT) }
                ?: WallpaperSettings().timeFormat,
            showSeconds = booleanPreference(SHOW_SECONDS, true),
            dateFormat = DateFormat.entries.firstOrNull { it.name == stringPreference(DATE_FORMAT) }
                ?: WallpaperSettings().dateFormat,
            clockFont = ClockFont.entries.firstOrNull { it.name == stringPreference(CLOCK_FONT) }
                ?: WallpaperSettings().clockFont,
            clockFontSizeSp = floatPreference(CLOCK_FONT_SIZE_SP, 56f),
            clockBold = booleanPreference(CLOCK_BOLD, false),
            clockItalic = booleanPreference(CLOCK_ITALIC, false),
            clockColor = longPreference(CLOCK_COLOR, WallpaperSettings().clockColor),
            dateColor = longPreference(DATE_COLOR, WallpaperSettings().dateColor),
            shadowEnabled = booleanPreference(SHADOW_ENABLED, true),
            shadowBlurRadius = floatPreference(SHADOW_BLUR_RADIUS, 6f),
            shadowOffsetX = floatPreference(SHADOW_OFFSET_X, 2f),
            shadowOffsetY = floatPreference(SHADOW_OFFSET_Y, 2f),
            shadowColor = longPreference(SHADOW_COLOR, WallpaperSettings().shadowColor),
            transparency = intPreference(TRANSPARENCY, 100),
            position = PositionPreset.entries.firstOrNull { it.name == stringPreference(POSITION) }
                ?: WallpaperSettings().position,
            positionXFraction = floatPreference(POSITION_X_FRACTION, 0.5f),
            positionYFraction = floatPreference(POSITION_Y_FRACTION, 0.88f),
            backgroundMode = BackgroundMode.entries.firstOrNull { it.name == stringPreference(BACKGROUND_MODE) }
                ?: WallpaperSettings().backgroundMode,
            backgroundZoom = floatPreference(BACKGROUND_ZOOM, 1f),
            backgroundRotationDegrees = floatPreference(BACKGROUND_ROTATION_DEGREES, 0f),
            backgroundTranslateXFraction = floatPreference(BACKGROUND_TRANSLATE_X_FRACTION, 0f),
            backgroundTranslateYFraction = floatPreference(BACKGROUND_TRANSLATE_Y_FRACTION, 0f),
            parallaxEnabled = booleanPreference(PARALLAX_ENABLED, false),
            parallaxSensitivity = floatPreference(PARALLAX_SENSITIVITY, 0.5f),
            parallaxStrength = floatPreference(PARALLAX_STRENGTH, 0.5f),
            parallaxSmoothing = floatPreference(PARALLAX_SMOOTHING, 0.5f),
            depthEnabled = booleanPreference(DEPTH_ENABLED, false),
            glassEnabled = booleanPreference(GLASS_ENABLED, false),
            glassBlurRadius = floatPreference(GLASS_BLUR_RADIUS, 14f),
            glassPanelOpacity = intPreference(GLASS_PANEL_OPACITY, 35),
            glassCornerRadius = floatPreference(GLASS_CORNER_RADIUS, 24f),
            glassBorderColor = longPreference(GLASS_BORDER_COLOR, WallpaperSettings().glassBorderColor),
            glassBorderWidth = floatPreference(GLASS_BORDER_WIDTH, 2f),
            glassGlowColor = longPreference(GLASS_GLOW_COLOR, WallpaperSettings().glassGlowColor),
            glassGlowRadius = floatPreference(GLASS_GLOW_RADIUS, 20f),
            dynamicClockColor = booleanPreference(DYNAMIC_CLOCK_COLOR, false),
            dynamicDateColor = booleanPreference(DYNAMIC_DATE_COLOR, false),
            fadeTransitionsEnabled = booleanPreference(FADE_TRANSITIONS_ENABLED, true),
            smoothSecondsEnabled = booleanPreference(SMOOTH_SECONDS_ENABLED, true),
            breathingEnabled = booleanPreference(BREATHING_ENABLED, true),
            breathingStrength = floatPreference(BREATHING_STRENGTH, 0.35f),
            zoomEnabled = booleanPreference(ZOOM_ENABLED, false),
            zoomStrength = floatPreference(ZOOM_STRENGTH, 0.5f),
            zoomDurationSeconds = floatPreference(ZOOM_DURATION_SECONDS, 30f),
            zoomDirection = ZoomDirection.entries.firstOrNull { it.name == stringPreference(ZOOM_DIRECTION) }
                ?: WallpaperSettings().zoomDirection,
            lowEnd = LowEndPreference.entries.firstOrNull { it.name == stringPreference(LOW_END) }
                ?: WallpaperSettings().lowEnd
        )
    }

    private fun WallpaperSettings.writeTo(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        prefs[stringPreferencesKey(TIME_FORMAT)] = timeFormat.name
        prefs[booleanPreferencesKey(SHOW_SECONDS)] = showSeconds
        prefs[stringPreferencesKey(DATE_FORMAT)] = dateFormat.name
        prefs[stringPreferencesKey(CLOCK_FONT)] = clockFont.name
        prefs[floatPreferencesKey(CLOCK_FONT_SIZE_SP)] = clockFontSizeSp
        prefs[booleanPreferencesKey(CLOCK_BOLD)] = clockBold
        prefs[booleanPreferencesKey(CLOCK_ITALIC)] = clockItalic
        prefs[longPreferencesKey(CLOCK_COLOR)] = clockColor
        prefs[longPreferencesKey(DATE_COLOR)] = dateColor
        prefs[booleanPreferencesKey(SHADOW_ENABLED)] = shadowEnabled
        prefs[floatPreferencesKey(SHADOW_BLUR_RADIUS)] = shadowBlurRadius
        prefs[floatPreferencesKey(SHADOW_OFFSET_X)] = shadowOffsetX
        prefs[floatPreferencesKey(SHADOW_OFFSET_Y)] = shadowOffsetY
        prefs[longPreferencesKey(SHADOW_COLOR)] = shadowColor
        prefs[intPreferencesKey(TRANSPARENCY)] = transparency
        prefs[stringPreferencesKey(POSITION)] = position.name
        prefs[floatPreferencesKey(POSITION_X_FRACTION)] = positionXFraction
        prefs[floatPreferencesKey(POSITION_Y_FRACTION)] = positionYFraction
        prefs[stringPreferencesKey(BACKGROUND_MODE)] = backgroundMode.name
        prefs[floatPreferencesKey(BACKGROUND_ZOOM)] = backgroundZoom
        prefs[floatPreferencesKey(BACKGROUND_ROTATION_DEGREES)] = backgroundRotationDegrees
        prefs[floatPreferencesKey(BACKGROUND_TRANSLATE_X_FRACTION)] = backgroundTranslateXFraction
        prefs[floatPreferencesKey(BACKGROUND_TRANSLATE_Y_FRACTION)] = backgroundTranslateYFraction
        prefs[booleanPreferencesKey(PARALLAX_ENABLED)] = parallaxEnabled
        prefs[floatPreferencesKey(PARALLAX_SENSITIVITY)] = parallaxSensitivity
        prefs[floatPreferencesKey(PARALLAX_STRENGTH)] = parallaxStrength
        prefs[floatPreferencesKey(PARALLAX_SMOOTHING)] = parallaxSmoothing
        prefs[booleanPreferencesKey(DEPTH_ENABLED)] = depthEnabled
        prefs[booleanPreferencesKey(GLASS_ENABLED)] = glassEnabled
        prefs[floatPreferencesKey(GLASS_BLUR_RADIUS)] = glassBlurRadius
        prefs[intPreferencesKey(GLASS_PANEL_OPACITY)] = glassPanelOpacity
        prefs[floatPreferencesKey(GLASS_CORNER_RADIUS)] = glassCornerRadius
        prefs[longPreferencesKey(GLASS_BORDER_COLOR)] = glassBorderColor
        prefs[floatPreferencesKey(GLASS_BORDER_WIDTH)] = glassBorderWidth
        prefs[longPreferencesKey(GLASS_GLOW_COLOR)] = glassGlowColor
        prefs[floatPreferencesKey(GLASS_GLOW_RADIUS)] = glassGlowRadius
        prefs[booleanPreferencesKey(DYNAMIC_CLOCK_COLOR)] = dynamicClockColor
        prefs[booleanPreferencesKey(DYNAMIC_DATE_COLOR)] = dynamicDateColor
        prefs[booleanPreferencesKey(FADE_TRANSITIONS_ENABLED)] = fadeTransitionsEnabled
        prefs[booleanPreferencesKey(SMOOTH_SECONDS_ENABLED)] = smoothSecondsEnabled
        prefs[booleanPreferencesKey(BREATHING_ENABLED)] = breathingEnabled
        prefs[floatPreferencesKey(BREATHING_STRENGTH)] = breathingStrength
        prefs[booleanPreferencesKey(ZOOM_ENABLED)] = zoomEnabled
        prefs[floatPreferencesKey(ZOOM_STRENGTH)] = zoomStrength
        prefs[floatPreferencesKey(ZOOM_DURATION_SECONDS)] = zoomDurationSeconds
        prefs[stringPreferencesKey(ZOOM_DIRECTION)] = zoomDirection.name
        prefs[stringPreferencesKey(LOW_END)] = lowEnd.name
    }

    private fun Preferences.stringPreference(key: String): String? = this[stringPreferencesKey(key)]
    private fun Preferences.booleanPreference(key: String, default: Boolean): Boolean =
        this[booleanPreferencesKey(key)] ?: default

    private fun Preferences.intPreference(key: String, default: Int): Int =
        this[intPreferencesKey(key)] ?: default

    private fun Preferences.floatPreference(key: String, default: Float): Float =
        this[floatPreferencesKey(key)] ?: default

    private fun Preferences.longPreference(key: String, default: Long): Long =
        this[longPreferencesKey(key)] ?: default

    private companion object {
        const val TIME_FORMAT = "time_format"
        const val SHOW_SECONDS = "show_seconds"
        const val DATE_FORMAT = "date_format"
        const val CLOCK_FONT = "clock_font"
        const val CLOCK_FONT_SIZE_SP = "clock_font_size_sp"
        const val CLOCK_BOLD = "clock_bold"
        const val CLOCK_ITALIC = "clock_italic"
        const val CLOCK_COLOR = "clock_color"
        const val DATE_COLOR = "date_color"
        const val SHADOW_ENABLED = "shadow_enabled"
        const val SHADOW_BLUR_RADIUS = "shadow_blur_radius"
        const val SHADOW_OFFSET_X = "shadow_offset_x"
        const val SHADOW_OFFSET_Y = "shadow_offset_y"
        const val SHADOW_COLOR = "shadow_color"
        const val TRANSPARENCY = "transparency"
        const val POSITION = "position"
        const val POSITION_X_FRACTION = "position_x_fraction"
        const val POSITION_Y_FRACTION = "position_y_fraction"
        const val BACKGROUND_MODE = "background_mode"
        const val BACKGROUND_ZOOM = "background_zoom"
        const val BACKGROUND_ROTATION_DEGREES = "background_rotation_degrees"
        const val BACKGROUND_TRANSLATE_X_FRACTION = "background_translate_x_fraction"
        const val BACKGROUND_TRANSLATE_Y_FRACTION = "background_translate_y_fraction"
        const val PARALLAX_ENABLED = "parallax_enabled"
        const val PARALLAX_SENSITIVITY = "parallax_sensitivity"
        const val PARALLAX_STRENGTH = "parallax_strength"
        const val PARALLAX_SMOOTHING = "parallax_smoothing"
        const val DEPTH_ENABLED = "depth_enabled"
        const val GLASS_ENABLED = "glass_enabled"
        const val GLASS_BLUR_RADIUS = "glass_blur_radius"
        const val GLASS_PANEL_OPACITY = "glass_panel_opacity"
        const val GLASS_CORNER_RADIUS = "glass_corner_radius"
        const val GLASS_BORDER_COLOR = "glass_border_color"
        const val GLASS_BORDER_WIDTH = "glass_border_width"
        const val GLASS_GLOW_COLOR = "glass_glow_color"
        const val GLASS_GLOW_RADIUS = "glass_glow_radius"
        const val DYNAMIC_CLOCK_COLOR = "dynamic_clock_color"
        const val DYNAMIC_DATE_COLOR = "dynamic_date_color"
        const val FADE_TRANSITIONS_ENABLED = "fade_transitions_enabled"
        const val SMOOTH_SECONDS_ENABLED = "smooth_seconds_enabled"
        const val BREATHING_ENABLED = "breathing_enabled"
        const val BREATHING_STRENGTH = "breathing_strength"
        const val ZOOM_ENABLED = "zoom_enabled"
        const val ZOOM_STRENGTH = "zoom_strength"
        const val ZOOM_DURATION_SECONDS = "zoom_duration_seconds"
        const val ZOOM_DIRECTION = "zoom_direction"
        const val LOW_END = "low_end"
    }
}
