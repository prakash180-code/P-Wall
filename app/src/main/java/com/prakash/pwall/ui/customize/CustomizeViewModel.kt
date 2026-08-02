package com.prakash.pwall.ui.customize

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prakash.pwall.data.model.BackgroundMode
import com.prakash.pwall.data.model.ClockFont
import com.prakash.pwall.data.model.DateFormat
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.TimeFormat
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the customize screen. Every setter persists the change via
 * [SettingsRepository] so the preview and the live wallpaper update instantly.
 */
class CustomizeViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<WallpaperSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WallpaperSettings()
        )

    fun setTimeFormat(format: TimeFormat) = update { it.copy(timeFormat = format) }

    fun setShowSeconds(show: Boolean) = update { it.copy(showSeconds = show) }

    fun setDateFormat(format: DateFormat) = update { it.copy(dateFormat = format) }

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

    fun setParallaxEnabled(enabled: Boolean) = update { it.copy(parallaxEnabled = enabled) }

    fun setParallaxSensitivity(value: Float) = update {
        it.copy(parallaxSensitivity = value.coerceIn(0f, 1f))
    }

    fun setParallaxStrength(value: Float) = update {
        it.copy(parallaxStrength = value.coerceIn(0f, 1f))
    }

    fun setParallaxSmoothing(value: Float) = update {
        it.copy(parallaxSmoothing = value.coerceIn(0f, 1f))
    }

    private fun update(transform: (WallpaperSettings) -> WallpaperSettings) {
        viewModelScope.launch { settingsRepository.updateSettings(transform) }
    }
}
