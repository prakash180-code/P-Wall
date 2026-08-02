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
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.TimeFormat
import com.prakash.pwall.data.model.WallpaperSettings
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
            backgroundTranslateYFraction = floatPreference(BACKGROUND_TRANSLATE_Y_FRACTION, 0f)
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
    }
}
