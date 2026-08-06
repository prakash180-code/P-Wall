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
import com.prakash.pwall.data.model.AppTheme
import com.prakash.pwall.data.model.ClockFont
import com.prakash.pwall.data.model.ClockLayout
import com.prakash.pwall.data.model.ContainerBorderStyle
import com.prakash.pwall.data.model.ContainerShape
import com.prakash.pwall.data.model.DateFormat
import com.prakash.pwall.data.model.DateLayout
import com.prakash.pwall.data.model.GradientDirection
import com.prakash.pwall.data.model.LowEndPreference
import com.prakash.pwall.data.model.ParallaxSensitivityLevel
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.TimeFormat
import com.prakash.pwall.data.model.TimeLayout
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.model.WidgetBackgroundMode
import com.prakash.pwall.data.model.WidgetStyle
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
    private val imageStore: ImageStore,
    private val widgetImageStore: ImageStore
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

    /** Replaces the whole settings snapshot (restore/backup import). */
    suspend fun applySettings(settings: WallpaperSettings) {
        dataStore.edit { prefs -> settings.writeTo(prefs) }
    }

    /** Restores every preference to its factory default. */
    suspend fun resetToDefaults() {
        dataStore.edit { it.clear() }
    }

    private fun Preferences.toSettings(): WallpaperSettings {
        return WallpaperSettings(
            selectedImagePath = imageStore.imagePath,
            timeFormat = TimeFormat.entries.firstOrNull { it.name == stringPreference(TIME_FORMAT) }
                ?: WallpaperSettings().timeFormat,
            showSeconds = booleanPreference(SHOW_SECONDS, true),
            dateFormat = DateFormat.entries.firstOrNull { it.name == stringPreference(DATE_FORMAT) }
                ?: WallpaperSettings().dateFormat,
            clockLayout = ClockLayout.entries.firstOrNull { it.name == stringPreference(CLOCK_LAYOUT) }
                ?: WallpaperSettings().clockLayout,
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
            clockVisible = booleanPreference(CLOCK_VISIBLE, true),
            dateVisible = booleanPreference(DATE_VISIBLE, true),
            timeLayout = timeLayoutPreference(),
            dateLayout = DateLayout.entries.firstOrNull { it.name == stringPreference(DATE_LAYOUT) }
                ?: WallpaperSettings().dateLayout,
            timeStyle = WidgetStyle.entries.firstOrNull { it.name == stringPreference(TIME_STYLE) }
                ?: WallpaperSettings().timeStyle,
            dateStyle = WidgetStyle.entries.firstOrNull { it.name == stringPreference(DATE_STYLE) }
                ?: WallpaperSettings().dateStyle,
            dateLinkedToTime = booleanPreference(DATE_LINKED_TO_TIME, true),
            dateGapMultiplier = floatPreference(DATE_GAP_MULTIPLIER, 1f),
            datePosition = PositionPreset.entries.firstOrNull { it.name == stringPreference(DATE_POSITION) }
                ?: WallpaperSettings().datePosition,
            datePositionXFraction = floatPreference(DATE_POSITION_X_FRACTION, 0.5f),
            datePositionYFraction = floatPreference(DATE_POSITION_Y_FRACTION, 0.5f),
            dateFont = ClockFont.entries.firstOrNull { it.name == stringPreference(DATE_FONT) }
                ?: WallpaperSettings().dateFont,
            dateFontSizeSp = floatPreference(DATE_FONT_SIZE_SP, 0f),
            dateBold = booleanPreference(DATE_BOLD, false),
            dateItalic = booleanPreference(DATE_ITALIC, false),
            dateShadowEnabled = booleanPreference(DATE_SHADOW_ENABLED, true),
            dateShadowBlurRadius = floatPreference(DATE_SHADOW_BLUR_RADIUS, 6f),
            dateShadowOffsetX = floatPreference(DATE_SHADOW_OFFSET_X, 2f),
            dateShadowOffsetY = floatPreference(DATE_SHADOW_OFFSET_Y, 2f),
            dateShadowColor = longPreference(DATE_SHADOW_COLOR, WallpaperSettings().dateShadowColor),
            dateAnimated = booleanPreference(DATE_ANIMATED, true),
            widgetBackgroundMode =
                WidgetBackgroundMode.entries.firstOrNull {
                    it.name == stringPreference(WIDGET_BACKGROUND_MODE)
                } ?: WallpaperSettings().widgetBackgroundMode,
            widgetBackgroundColor =
                longPreference(WIDGET_BACKGROUND_COLOR, WallpaperSettings().widgetBackgroundColor),
            widgetBackgroundColor2 =
                longPreference(WIDGET_BACKGROUND_COLOR2, WallpaperSettings().widgetBackgroundColor2),
            widgetGradientDirection =
                GradientDirection.entries.firstOrNull {
                    it.name == stringPreference(WIDGET_GRADIENT_DIRECTION)
                } ?: WallpaperSettings().widgetGradientDirection,
            widgetBackgroundOpacity = intPreference(WIDGET_BACKGROUND_OPACITY, 50),
            widgetGlassBlur = floatPreference(WIDGET_GLASS_BLUR, 20f),
            widgetGlassTintColor =
                longPreference(WIDGET_GLASS_TINT_COLOR, WallpaperSettings().widgetGlassTintColor),
            widgetWallpaperBlurStrength = floatPreference(WIDGET_WALLPAPER_BLUR_STRENGTH, 16f),
            widgetWallpaperTintColor =
                longPreference(WIDGET_WALLPAPER_TINT_COLOR, WallpaperSettings().widgetWallpaperTintColor),
            widgetImagePath = widgetImageStore.imagePath,
            widgetImageFit = BackgroundMode.entries.firstOrNull {
                it.name == stringPreference(WIDGET_IMAGE_FIT)
            } ?: WallpaperSettings().widgetImageFit,
            widgetShape = ContainerShape.entries.firstOrNull {
                it.name == stringPreference(WIDGET_SHAPE)
            } ?: WallpaperSettings().widgetShape,
            widgetCornerRadius = floatPreference(WIDGET_CORNER_RADIUS, 24f),
            widgetBorderEnabled = booleanPreference(WIDGET_BORDER_ENABLED, false),
            widgetBorderColor =
                longPreference(WIDGET_BORDER_COLOR, WallpaperSettings().widgetBorderColor),
            widgetBorderWidth = floatPreference(WIDGET_BORDER_WIDTH, 2f),
            widgetBorderOpacity = intPreference(WIDGET_BORDER_OPACITY, 60),
            widgetBorderStyle = ContainerBorderStyle.entries.firstOrNull {
                it.name == stringPreference(WIDGET_BORDER_STYLE)
            } ?: WallpaperSettings().widgetBorderStyle,
            widgetShadowEnabled = booleanPreference(WIDGET_SHADOW_ENABLED, false),
            widgetShadowColor =
                longPreference(WIDGET_SHADOW_COLOR, WallpaperSettings().widgetShadowColor),
            widgetShadowBlur = floatPreference(WIDGET_SHADOW_BLUR, 16f),
            widgetShadowSpread = floatPreference(WIDGET_SHADOW_SPREAD, 0f),
            widgetShadowOffsetX = floatPreference(WIDGET_SHADOW_OFFSET_X, 0f),
            widgetShadowOffsetY = floatPreference(WIDGET_SHADOW_OFFSET_Y, 6f),
            widgetPaddingHorizontal = floatPreference(WIDGET_PADDING_HORIZONTAL, 20f),
            widgetPaddingVertical = floatPreference(WIDGET_PADDING_VERTICAL, 14f),
            backgroundMode = BackgroundMode.entries.firstOrNull { it.name == stringPreference(BACKGROUND_MODE) }
                ?: WallpaperSettings().backgroundMode,
            backgroundZoom = floatPreference(BACKGROUND_ZOOM, 1f),
            backgroundRotationDegrees = floatPreference(BACKGROUND_ROTATION_DEGREES, 0f),
            backgroundTranslateXFraction = floatPreference(BACKGROUND_TRANSLATE_X_FRACTION, 0f),
            backgroundTranslateYFraction = floatPreference(BACKGROUND_TRANSLATE_Y_FRACTION, 0f),
            parallaxEnabled = booleanPreference(PARALLAX_ENABLED, false),
            parallaxSensitivityLevel =
                ParallaxSensitivityLevel.entries.firstOrNull {
                    it.name == stringPreference(PARALLAX_SENSITIVITY_LEVEL)
                } ?: WallpaperSettings().parallaxSensitivityLevel,
            parallaxStrength = floatPreference(PARALLAX_STRENGTH, 0.5f),
            parallaxSmoothing = floatPreference(PARALLAX_SMOOTHING, 0.5f),
            debugParallax = booleanPreference(DEBUG_PARALLAX, false),
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
                ?: WallpaperSettings().lowEnd,
            appTheme = AppTheme.entries.firstOrNull { it.name == stringPreference(APP_THEME) }
                ?: WallpaperSettings().appTheme,
            customPrimaryColor = longPreference(CUSTOM_PRIMARY_COLOR, WallpaperSettings().customPrimaryColor),
            customSecondaryColor = longPreference(CUSTOM_SECONDARY_COLOR, WallpaperSettings().customSecondaryColor),
            customAccentColor = longPreference(CUSTOM_ACCENT_COLOR, WallpaperSettings().customAccentColor)
        )
    }

    private fun WallpaperSettings.writeTo(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        prefs[stringPreferencesKey(TIME_FORMAT)] = timeFormat.name
        prefs[booleanPreferencesKey(SHOW_SECONDS)] = showSeconds
        prefs[stringPreferencesKey(DATE_FORMAT)] = dateFormat.name
        prefs[stringPreferencesKey(CLOCK_LAYOUT)] = clockLayout.name
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
        prefs[booleanPreferencesKey(CLOCK_VISIBLE)] = clockVisible
        prefs[booleanPreferencesKey(DATE_VISIBLE)] = dateVisible
        prefs[stringPreferencesKey(TIME_LAYOUT)] = timeLayout.name
        prefs[stringPreferencesKey(DATE_LAYOUT)] = dateLayout.name
        prefs[stringPreferencesKey(TIME_STYLE)] = timeStyle.name
        prefs[stringPreferencesKey(DATE_STYLE)] = dateStyle.name
        prefs[booleanPreferencesKey(DATE_LINKED_TO_TIME)] = dateLinkedToTime
        prefs[floatPreferencesKey(DATE_GAP_MULTIPLIER)] = dateGapMultiplier
        prefs[stringPreferencesKey(DATE_POSITION)] = datePosition.name
        prefs[floatPreferencesKey(DATE_POSITION_X_FRACTION)] = datePositionXFraction
        prefs[floatPreferencesKey(DATE_POSITION_Y_FRACTION)] = datePositionYFraction
        prefs[stringPreferencesKey(DATE_FONT)] = dateFont.name
        prefs[floatPreferencesKey(DATE_FONT_SIZE_SP)] = dateFontSizeSp
        prefs[booleanPreferencesKey(DATE_BOLD)] = dateBold
        prefs[booleanPreferencesKey(DATE_ITALIC)] = dateItalic
        prefs[booleanPreferencesKey(DATE_SHADOW_ENABLED)] = dateShadowEnabled
        prefs[floatPreferencesKey(DATE_SHADOW_BLUR_RADIUS)] = dateShadowBlurRadius
        prefs[floatPreferencesKey(DATE_SHADOW_OFFSET_X)] = dateShadowOffsetX
        prefs[floatPreferencesKey(DATE_SHADOW_OFFSET_Y)] = dateShadowOffsetY
        prefs[longPreferencesKey(DATE_SHADOW_COLOR)] = dateShadowColor
        prefs[booleanPreferencesKey(DATE_ANIMATED)] = dateAnimated
        prefs[stringPreferencesKey(WIDGET_BACKGROUND_MODE)] = widgetBackgroundMode.name
        prefs[longPreferencesKey(WIDGET_BACKGROUND_COLOR)] = widgetBackgroundColor
        prefs[longPreferencesKey(WIDGET_BACKGROUND_COLOR2)] = widgetBackgroundColor2
        prefs[stringPreferencesKey(WIDGET_GRADIENT_DIRECTION)] = widgetGradientDirection.name
        prefs[intPreferencesKey(WIDGET_BACKGROUND_OPACITY)] = widgetBackgroundOpacity
        prefs[floatPreferencesKey(WIDGET_GLASS_BLUR)] = widgetGlassBlur
        prefs[longPreferencesKey(WIDGET_GLASS_TINT_COLOR)] = widgetGlassTintColor
        prefs[floatPreferencesKey(WIDGET_WALLPAPER_BLUR_STRENGTH)] = widgetWallpaperBlurStrength
        prefs[longPreferencesKey(WIDGET_WALLPAPER_TINT_COLOR)] = widgetWallpaperTintColor
        prefs[stringPreferencesKey(WIDGET_IMAGE_FIT)] = widgetImageFit.name
        prefs[stringPreferencesKey(WIDGET_SHAPE)] = widgetShape.name
        prefs[floatPreferencesKey(WIDGET_CORNER_RADIUS)] = widgetCornerRadius
        prefs[booleanPreferencesKey(WIDGET_BORDER_ENABLED)] = widgetBorderEnabled
        prefs[longPreferencesKey(WIDGET_BORDER_COLOR)] = widgetBorderColor
        prefs[floatPreferencesKey(WIDGET_BORDER_WIDTH)] = widgetBorderWidth
        prefs[intPreferencesKey(WIDGET_BORDER_OPACITY)] = widgetBorderOpacity
        prefs[stringPreferencesKey(WIDGET_BORDER_STYLE)] = widgetBorderStyle.name
        prefs[booleanPreferencesKey(WIDGET_SHADOW_ENABLED)] = widgetShadowEnabled
        prefs[longPreferencesKey(WIDGET_SHADOW_COLOR)] = widgetShadowColor
        prefs[floatPreferencesKey(WIDGET_SHADOW_BLUR)] = widgetShadowBlur
        prefs[floatPreferencesKey(WIDGET_SHADOW_SPREAD)] = widgetShadowSpread
        prefs[floatPreferencesKey(WIDGET_SHADOW_OFFSET_X)] = widgetShadowOffsetX
        prefs[floatPreferencesKey(WIDGET_SHADOW_OFFSET_Y)] = widgetShadowOffsetY
        prefs[floatPreferencesKey(WIDGET_PADDING_HORIZONTAL)] = widgetPaddingHorizontal
        prefs[floatPreferencesKey(WIDGET_PADDING_VERTICAL)] = widgetPaddingVertical
        prefs[stringPreferencesKey(BACKGROUND_MODE)] = backgroundMode.name
        prefs[floatPreferencesKey(BACKGROUND_ZOOM)] = backgroundZoom
        prefs[floatPreferencesKey(BACKGROUND_ROTATION_DEGREES)] = backgroundRotationDegrees
        prefs[floatPreferencesKey(BACKGROUND_TRANSLATE_X_FRACTION)] = backgroundTranslateXFraction
        prefs[floatPreferencesKey(BACKGROUND_TRANSLATE_Y_FRACTION)] = backgroundTranslateYFraction
        prefs[booleanPreferencesKey(PARALLAX_ENABLED)] = parallaxEnabled
        prefs[stringPreferencesKey(PARALLAX_SENSITIVITY_LEVEL)] = parallaxSensitivityLevel.name
        prefs[floatPreferencesKey(PARALLAX_STRENGTH)] = parallaxStrength
        prefs[floatPreferencesKey(PARALLAX_SMOOTHING)] = parallaxSmoothing
        prefs[booleanPreferencesKey(DEBUG_PARALLAX)] = debugParallax
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
        prefs[stringPreferencesKey(APP_THEME)] = appTheme.name
        prefs[longPreferencesKey(CUSTOM_PRIMARY_COLOR)] = customPrimaryColor
        prefs[longPreferencesKey(CUSTOM_SECONDARY_COLOR)] = customSecondaryColor
        prefs[longPreferencesKey(CUSTOM_ACCENT_COLOR)] = customAccentColor
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

    /**
     * Time widget layout. New `time_layout` values win; when absent the legacy
     * `clock_layout` is migrated so existing wallpapers keep their arrangement.
     */
    private fun Preferences.timeLayoutPreference(): TimeLayout {
        val stored = stringPreference(TIME_LAYOUT)?.let { name ->
            TimeLayout.entries.firstOrNull { it.name == name }
        }
        if (stored != null) return stored
        val legacy = stringPreference(CLOCK_LAYOUT)?.let { name ->
            ClockLayout.entries.firstOrNull { it.name == name }
        }
        return if (legacy != null) TimeLayout.fromLegacy(legacy) else TimeLayout.HORIZONTAL
    }

    private companion object {
        const val TIME_FORMAT = "time_format"
        const val SHOW_SECONDS = "show_seconds"
        const val DATE_FORMAT = "date_format"
        const val CLOCK_LAYOUT = "clock_layout"
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
        const val CLOCK_VISIBLE = "clock_visible"
        const val DATE_VISIBLE = "date_visible"
        const val TIME_LAYOUT = "time_layout"
        const val DATE_LAYOUT = "date_layout"
        const val TIME_STYLE = "time_style"
        const val DATE_STYLE = "date_style"
        const val DATE_LINKED_TO_TIME = "date_linked_to_time"
        const val DATE_GAP_MULTIPLIER = "date_gap_multiplier"
        const val DATE_POSITION = "date_position"
        const val DATE_POSITION_X_FRACTION = "date_position_x_fraction"
        const val DATE_POSITION_Y_FRACTION = "date_position_y_fraction"
        const val DATE_FONT = "date_font"
        const val DATE_FONT_SIZE_SP = "date_font_size_sp"
        const val DATE_BOLD = "date_bold"
        const val DATE_ITALIC = "date_italic"
        const val DATE_SHADOW_ENABLED = "date_shadow_enabled"
        const val DATE_SHADOW_BLUR_RADIUS = "date_shadow_blur_radius"
        const val DATE_SHADOW_OFFSET_X = "date_shadow_offset_x"
        const val DATE_SHADOW_OFFSET_Y = "date_shadow_offset_y"
        const val DATE_SHADOW_COLOR = "date_shadow_color"
        const val DATE_ANIMATED = "date_animated"
        const val WIDGET_BACKGROUND_MODE = "widget_background_mode"
        const val WIDGET_BACKGROUND_COLOR = "widget_background_color"
        const val WIDGET_BACKGROUND_COLOR2 = "widget_background_color2"
        const val WIDGET_GRADIENT_DIRECTION = "widget_gradient_direction"
        const val WIDGET_BACKGROUND_OPACITY = "widget_background_opacity"
        const val WIDGET_GLASS_BLUR = "widget_glass_blur"
        const val WIDGET_GLASS_TINT_COLOR = "widget_glass_tint_color"
        const val WIDGET_WALLPAPER_BLUR_STRENGTH = "widget_wallpaper_blur_strength"
        const val WIDGET_WALLPAPER_TINT_COLOR = "widget_wallpaper_tint_color"
        const val WIDGET_IMAGE_FIT = "widget_image_fit"
        const val WIDGET_SHAPE = "widget_shape"
        const val WIDGET_CORNER_RADIUS = "widget_corner_radius"
        const val WIDGET_BORDER_ENABLED = "widget_border_enabled"
        const val WIDGET_BORDER_COLOR = "widget_border_color"
        const val WIDGET_BORDER_WIDTH = "widget_border_width"
        const val WIDGET_BORDER_OPACITY = "widget_border_opacity"
        const val WIDGET_BORDER_STYLE = "widget_border_style"
        const val WIDGET_SHADOW_ENABLED = "widget_shadow_enabled"
        const val WIDGET_SHADOW_COLOR = "widget_shadow_color"
        const val WIDGET_SHADOW_BLUR = "widget_shadow_blur"
        const val WIDGET_SHADOW_SPREAD = "widget_shadow_spread"
        const val WIDGET_SHADOW_OFFSET_X = "widget_shadow_offset_x"
        const val WIDGET_SHADOW_OFFSET_Y = "widget_shadow_offset_y"
        const val WIDGET_PADDING_HORIZONTAL = "widget_padding_horizontal"
        const val WIDGET_PADDING_VERTICAL = "widget_padding_vertical"
        const val BACKGROUND_MODE = "background_mode"
        const val BACKGROUND_ZOOM = "background_zoom"
        const val BACKGROUND_ROTATION_DEGREES = "background_rotation_degrees"
        const val BACKGROUND_TRANSLATE_X_FRACTION = "background_translate_x_fraction"
        const val BACKGROUND_TRANSLATE_Y_FRACTION = "background_translate_y_fraction"
        const val PARALLAX_ENABLED = "parallax_enabled"
        const val PARALLAX_SENSITIVITY_LEVEL = "parallax_sensitivity_level"
        const val PARALLAX_STRENGTH = "parallax_strength"
        const val PARALLAX_SMOOTHING = "parallax_smoothing"
        const val DEBUG_PARALLAX = "debug_parallax"
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
        const val APP_THEME = "app_theme"
        const val CUSTOM_PRIMARY_COLOR = "custom_primary_color"
        const val CUSTOM_SECONDARY_COLOR = "custom_secondary_color"
        const val CUSTOM_ACCENT_COLOR = "custom_accent_color"
    }
}
