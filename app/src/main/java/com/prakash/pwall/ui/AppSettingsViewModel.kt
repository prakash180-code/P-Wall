package com.prakash.pwall.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.prakash.pwall.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for the editor screens (clock, effects, customize,
 * settings). Exposes the persisted [WallpaperSettings] and a setter per option;
 * every setter persists via [SettingsRepository] so the preview, the app theme
 * and the live wallpaper all update instantly.
 */
class AppSettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<WallpaperSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WallpaperSettings()
        )

    // --- Clock page ---

    fun setTimeFormat(format: TimeFormat) = update { it.copy(timeFormat = format) }

    fun setShowSeconds(show: Boolean) = update { it.copy(showSeconds = show) }

    fun setDateFormat(format: DateFormat) = update { it.copy(dateFormat = format) }

    fun setClockLayout(layout: ClockLayout) = update { it.copy(clockLayout = layout) }

    fun setClockFont(font: ClockFont) = update { it.copy(clockFont = font) }

    fun setClockFontSize(sizeSp: Float) = update { it.copy(clockFontSizeSp = sizeSp) }

    fun setClockBold(bold: Boolean) = update { it.copy(clockBold = bold) }

    fun setClockItalic(italic: Boolean) = update { it.copy(clockItalic = italic) }

    fun setClockColor(color: Color) = update { it.copy(clockColor = color.toArgb().toLong()) }

    fun setDateColor(color: Color) = update { it.copy(dateColor = color.toArgb().toLong()) }

    fun setShadowEnabled(enabled: Boolean) = update { it.copy(shadowEnabled = enabled) }

    fun setShadowBlurRadius(radius: Float) = update { it.copy(shadowBlurRadius = radius) }

    fun setShadowOffset(x: Float, y: Float) = update {
        it.copy(shadowOffsetX = x, shadowOffsetY = y)
    }

    fun setShadowColor(color: Color) = update { it.copy(shadowColor = color.toArgb().toLong()) }

    fun setTransparency(percent: Int) = update { it.copy(transparency = percent.coerceIn(0, 100)) }

    fun setPosition(position: PositionPreset) = update { it.copy(position = position) }

    fun setPositionFraction(x: Float, y: Float) = update {
        it.copy(
            positionXFraction = x.coerceIn(0f, 1f),
            positionYFraction = y.coerceIn(0f, 1f)
        )
    }

    // --- Effects page ---

    fun setParallaxEnabled(enabled: Boolean) = update { it.copy(parallaxEnabled = enabled) }

    fun setParallaxSensitivityLevel(level: ParallaxSensitivityLevel) =
        update { it.copy(parallaxSensitivityLevel = level) }

    fun setParallaxStrength(value: Float) = update {
        it.copy(parallaxStrength = value.coerceIn(0f, 1f))
    }

    fun setParallaxSmoothing(value: Float) = update {
        it.copy(parallaxSmoothing = value.coerceIn(0f, 1f))
    }

    fun setDebugParallax(enabled: Boolean) = update { it.copy(debugParallax = enabled) }

    fun setDepthEnabled(enabled: Boolean) = update { it.copy(depthEnabled = enabled) }

    fun setGlassEnabled(enabled: Boolean) = update { it.copy(glassEnabled = enabled) }

    fun setGlassBlurRadius(radius: Float) = update { it.copy(glassBlurRadius = radius) }

    fun setGlassPanelOpacity(percent: Int) = update {
        it.copy(glassPanelOpacity = percent.coerceIn(0, 100))
    }

    fun setGlassCornerRadius(radius: Float) = update { it.copy(glassCornerRadius = radius) }

    fun setGlassBorderWidth(width: Float) = update { it.copy(glassBorderWidth = width) }

    fun setGlassBorderColor(color: Color) = update {
        it.copy(glassBorderColor = color.toArgb().toLong())
    }

    fun setGlassGlowRadius(radius: Float) = update { it.copy(glassGlowRadius = radius) }

    fun setGlassGlowColor(color: Color) = update {
        it.copy(glassGlowColor = color.toArgb().toLong())
    }

    fun setDynamicClockColor(enabled: Boolean) = update { it.copy(dynamicClockColor = enabled) }

    fun setDynamicDateColor(enabled: Boolean) = update { it.copy(dynamicDateColor = enabled) }

    fun setFadeTransitionsEnabled(enabled: Boolean) = update {
        it.copy(fadeTransitionsEnabled = enabled)
    }

    fun setSmoothSecondsEnabled(enabled: Boolean) = update {
        it.copy(smoothSecondsEnabled = enabled)
    }

    fun setBreathingEnabled(enabled: Boolean) = update { it.copy(breathingEnabled = enabled) }

    fun setBreathingStrength(value: Float) = update {
        it.copy(breathingStrength = value.coerceIn(0f, 1f))
    }

    fun setZoomEnabled(enabled: Boolean) = update { it.copy(zoomEnabled = enabled) }

    fun setZoomStrength(value: Float) = update {
        it.copy(zoomStrength = value.coerceIn(0f, 1f))
    }

    fun setZoomDurationSeconds(value: Float) = update {
        it.copy(zoomDurationSeconds = value.coerceIn(5f, 120f))
    }

    fun setZoomDirection(direction: ZoomDirection) = update { it.copy(zoomDirection = direction) }

    fun setLowEnd(preference: LowEndPreference) = update { it.copy(lowEnd = preference) }

    // --- Customize page (background) ---

    fun setBackgroundMode(mode: BackgroundMode) = update { it.copy(backgroundMode = mode) }

    fun setBackgroundZoom(zoom: Float) = update { it.copy(backgroundZoom = zoom) }

    fun setBackgroundRotation(degrees: Float) = update {
        it.copy(backgroundRotationDegrees = degrees)
    }

    fun setBackgroundTranslation(xFraction: Float, yFraction: Float) = update {
        it.copy(
            backgroundTranslateXFraction = xFraction.coerceIn(-1f, 1f),
            backgroundTranslateYFraction = yFraction.coerceIn(-1f, 1f)
        )
    }

    // --- Settings page (app theme) ---

    fun setAppTheme(theme: AppTheme) = update { it.copy(appTheme = theme) }

    fun setCustomPrimaryColor(color: Color) = update {
        it.copy(customPrimaryColor = color.toArgb().toLong())
    }

    fun setCustomSecondaryColor(color: Color) = update {
        it.copy(customSecondaryColor = color.toArgb().toLong())
    }

    fun setCustomAccentColor(color: Color) = update {
        it.copy(customAccentColor = color.toArgb().toLong())
    }

    private fun update(transform: (WallpaperSettings) -> WallpaperSettings) {
        viewModelScope.launch { settingsRepository.updateSettings(transform) }
    }
}
