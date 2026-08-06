package com.prakash.pwall.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prakash.pwall.data.model.AppTheme
import com.prakash.pwall.data.model.BackgroundMode
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
import com.prakash.pwall.data.repository.SettingsRepository
import com.prakash.pwall.service.render.TimeContainerPreset
import com.prakash.pwall.service.render.TimeContainerPresets
import com.prakash.pwall.service.render.WidgetPreset
import com.prakash.pwall.service.render.WidgetPresets
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

    // --- Clock & Date Engine ---

    fun setClockVisible(visible: Boolean) = update { it.copy(clockVisible = visible) }

    fun setDateVisible(visible: Boolean) = update { it.copy(dateVisible = visible) }

    fun setTimeLayout(layout: TimeLayout) = update { it.copy(timeLayout = layout) }

    fun setDateLayout(layout: DateLayout) = update { it.copy(dateLayout = layout) }

    fun setTimeStyle(style: WidgetStyle) = update { it.copy(timeStyle = style) }

    fun setDateStyle(style: WidgetStyle) = update { it.copy(dateStyle = style) }

    fun setDateLinkedToTime(linked: Boolean) = update { it.copy(dateLinkedToTime = linked) }

    fun setDateGapMultiplier(multiplier: Float) = update {
        it.copy(dateGapMultiplier = multiplier.coerceIn(0.5f, 3f))
    }

    fun setDatePosition(position: PositionPreset) = update { it.copy(datePosition = position) }

    fun setDatePositionFraction(x: Float, y: Float) = update {
        it.copy(
            datePositionXFraction = x.coerceIn(0f, 1f),
            datePositionYFraction = y.coerceIn(0f, 1f)
        )
    }

    fun setDateFont(font: ClockFont) = update { it.copy(dateFont = font) }

    fun setDateFontSize(sizeSp: Float) = update {
        it.copy(dateFontSizeSp = sizeSp.coerceIn(0f, 200f))
    }

    fun setDateBold(bold: Boolean) = update { it.copy(dateBold = bold) }

    fun setDateItalic(italic: Boolean) = update { it.copy(dateItalic = italic) }

    fun setDateShadowEnabled(enabled: Boolean) = update { it.copy(dateShadowEnabled = enabled) }

    fun setDateShadowBlurRadius(radius: Float) = update {
        it.copy(dateShadowBlurRadius = radius.coerceIn(0f, 40f))
    }

    fun setDateShadowOffset(x: Float, y: Float) = update {
        it.copy(dateShadowOffsetX = x, dateShadowOffsetY = y)
    }

    fun setDateShadowColor(color: Color) = update {
        it.copy(dateShadowColor = color.toArgb().toLong())
    }

    fun setDateAnimated(animated: Boolean) = update { it.copy(dateAnimated = animated) }

    /** Applies a one-tap [WidgetPreset] to the whole Clock & Date Engine. */
    fun applyWidgetPreset(preset: WidgetPreset) = update {
        WidgetPresets.apply(it, preset)
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

    // --- Time background (container) ---

    fun setWidgetBackgroundMode(mode: WidgetBackgroundMode) =
        update { it.copy(widgetBackgroundMode = mode) }

    fun setWidgetBackgroundColor(color: Color) =
        update { it.copy(widgetBackgroundColor = color.toArgb().toLong()) }

    fun setWidgetBackgroundColor2(color: Color) =
        update { it.copy(widgetBackgroundColor2 = color.toArgb().toLong()) }

    fun setWidgetGradientDirection(direction: GradientDirection) =
        update { it.copy(widgetGradientDirection = direction) }

    fun setWidgetBackgroundOpacity(percent: Int) = update {
        it.copy(widgetBackgroundOpacity = percent.coerceIn(0, 100))
    }

    fun setWidgetGlassBlur(blur: Float) = update {
        it.copy(widgetGlassBlur = blur.coerceIn(0f, 100f))
    }

    fun setWidgetGlassTintColor(color: Color) =
        update { it.copy(widgetGlassTintColor = color.toArgb().toLong()) }

    fun setWidgetWallpaperBlur(strength: Float) = update {
        it.copy(widgetWallpaperBlurStrength = strength.coerceIn(0f, 100f))
    }

    fun setWidgetWallpaperTintColor(color: Color) =
        update { it.copy(widgetWallpaperTintColor = color.toArgb().toLong()) }

    fun setWidgetImageFit(mode: BackgroundMode) =
        update { it.copy(widgetImageFit = mode) }

    fun setWidgetImagePath(path: String?) =
        update { it.copy(widgetImagePath = path) }

    fun setWidgetShape(shape: ContainerShape) = update { it.copy(widgetShape = shape) }

    fun setWidgetCornerRadius(radius: Float) = update {
        it.copy(widgetCornerRadius = radius.coerceIn(0f, 40f))
    }

    fun setWidgetBorderEnabled(enabled: Boolean) =
        update { it.copy(widgetBorderEnabled = enabled) }

    fun setWidgetBorderColor(color: Color) =
        update { it.copy(widgetBorderColor = color.toArgb().toLong()) }

    fun setWidgetBorderWidth(width: Float) = update {
        it.copy(widgetBorderWidth = width.coerceIn(0f, 20f))
    }

    fun setWidgetBorderOpacity(percent: Int) = update {
        it.copy(widgetBorderOpacity = percent.coerceIn(0, 100))
    }

    fun setWidgetBorderStyle(style: ContainerBorderStyle) =
        update { it.copy(widgetBorderStyle = style) }

    fun setWidgetShadowEnabled(enabled: Boolean) =
        update { it.copy(widgetShadowEnabled = enabled) }

    fun setWidgetShadowColor(color: Color) =
        update { it.copy(widgetShadowColor = color.toArgb().toLong()) }

    fun setWidgetShadowBlur(blur: Float) = update {
        it.copy(widgetShadowBlur = blur.coerceIn(0f, 60f))
    }

    fun setWidgetShadowSpread(spread: Float) = update {
        it.copy(widgetShadowSpread = spread.coerceIn(0f, 40f))
    }

    fun setWidgetShadowOffset(x: Float, y: Float) = update {
        it.copy(
            widgetShadowOffsetX = x.coerceIn(-40f, 40f),
            widgetShadowOffsetY = y.coerceIn(-40f, 40f)
        )
    }

    fun setWidgetPaddingHorizontal(value: Float) = update {
        it.copy(widgetPaddingHorizontal = value.coerceIn(0f, 50f))
    }

    fun setWidgetPaddingVertical(value: Float) = update {
        it.copy(widgetPaddingVertical = value.coerceIn(0f, 50f))
    }

    /** Applies a one-tap [TimeContainerPreset] to the time widget container. */
    fun applyTimeContainerPreset(preset: TimeContainerPreset) = update {
        TimeContainerPresets.apply(it, preset)
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
