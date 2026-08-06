package com.prakash.pwall.service.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.prakash.pwall.data.model.BackgroundMode
import com.prakash.pwall.data.model.ContainerBorderStyle
import com.prakash.pwall.data.model.ContainerShape
import com.prakash.pwall.data.model.GradientDirection
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.model.WidgetBackgroundMode

/**
 * Named bundles that configure the whole time widget container in one tap. Each
 * preset sets the container mode, fill colors, shape, border and shadow while
 * leaving the time text/style/position untouched. Pure data, JVM-testable.
 */
enum class TimeContainerPreset(val displayName: String) {
    TRANSPARENT("Transparent"),
    MINIMAL("Minimal"),
    GLASS("Glass"),
    FROSTED_GLASS("Frosted Glass"),
    DARK_CARD("Dark Card"),
    LIGHT_CARD("Light Card"),
    ROUNDED("Rounded"),
    CAPSULE("Capsule"),
    ELEGANT("Elegant"),
    CUSTOM("Custom")
}

object TimeContainerPresets {

    /**
     * The exact container configuration a [TimeContainerPreset] applies.
     * Compares against the current settings so the editor can highlight the
     * active preset (or CUSTOM once the user tweaks any container value).
     */
    private data class ContainerConfig(
        val mode: WidgetBackgroundMode,
        val color: Long,
        val color2: Long,
        val direction: GradientDirection,
        val opacity: Int,
        val glassBlur: Float,
        val glassTint: Long,
        val wallpaperBlur: Float,
        val wallpaperTint: Long,
        val imageFit: BackgroundMode,
        val shape: ContainerShape,
        val corner: Float,
        val borderEnabled: Boolean,
        val borderColor: Long,
        val borderWidth: Float,
        val borderOpacity: Int,
        val borderStyle: ContainerBorderStyle,
        val shadowEnabled: Boolean,
        val shadowColor: Long,
        val shadowBlur: Float,
        val shadowSpread: Float,
        val shadowX: Float,
        val shadowY: Float,
        val padH: Float,
        val padV: Float
    )

    private fun config(settings: WallpaperSettings): ContainerConfig = ContainerConfig(
        mode = settings.widgetBackgroundMode,
        color = settings.widgetBackgroundColor,
        color2 = settings.widgetBackgroundColor2,
        direction = settings.widgetGradientDirection,
        opacity = settings.widgetBackgroundOpacity,
        glassBlur = settings.widgetGlassBlur,
        glassTint = settings.widgetGlassTintColor,
        wallpaperBlur = settings.widgetWallpaperBlurStrength,
        wallpaperTint = settings.widgetWallpaperTintColor,
        imageFit = settings.widgetImageFit,
        shape = settings.widgetShape,
        corner = settings.widgetCornerRadius,
        borderEnabled = settings.widgetBorderEnabled,
        borderColor = settings.widgetBorderColor,
        borderWidth = settings.widgetBorderWidth,
        borderOpacity = settings.widgetBorderOpacity,
        borderStyle = settings.widgetBorderStyle,
        shadowEnabled = settings.widgetShadowEnabled,
        shadowColor = settings.widgetShadowColor,
        shadowBlur = settings.widgetShadowBlur,
        shadowSpread = settings.widgetShadowSpread,
        shadowX = settings.widgetShadowOffsetX,
        shadowY = settings.widgetShadowOffsetY,
        padH = settings.widgetPaddingHorizontal,
        padV = settings.widgetPaddingVertical
    )

    /** True when [settings] matches exactly the container [preset] applies. */
    fun matches(settings: WallpaperSettings, preset: TimeContainerPreset): Boolean {
        if (preset == TimeContainerPreset.CUSTOM) return false
        return config(apply(settings, preset)) == config(settings)
    }

    /** Applies a [TimeContainerPreset] to the container fields of [settings]. */
    fun apply(settings: WallpaperSettings, preset: TimeContainerPreset): WallpaperSettings {
        if (preset == TimeContainerPreset.CUSTOM) return settings
        if (preset == TimeContainerPreset.TRANSPARENT) {
            return settings.copy(widgetBackgroundMode = WidgetBackgroundMode.TRANSPARENT)
        }

        val border = Border(
            enabled = true,
            color = Color(0xE6FFFFFF).toArgb().toLong(),
            width = 1.5f,
            opacity = 50,
            style = ContainerBorderStyle.SOLID
        )
        val shadow = Shadow(
            enabled = true,
            color = Color(0x66000000).toArgb().toLong(),
            blur = 14f,
            spread = 0f,
            x = 0f,
            y = 5f
        )
        val c = when (preset) {
            TimeContainerPreset.TRANSPARENT,
            TimeContainerPreset.CUSTOM -> return settings
            TimeContainerPreset.MINIMAL -> Apply(
                mode = WidgetBackgroundMode.SOLID,
                color = Color(0xFF000000).toArgb().toLong(),
                opacity = 22,
                shape = ContainerShape.ROUNDED,
                corner = 16f,
                padH = 16f,
                padV = 12f
            )
            TimeContainerPreset.GLASS -> Apply(
                mode = WidgetBackgroundMode.GLASS,
                glassBlur = 20f,
                glassTint = Color(0xFFFFFFFF).toArgb().toLong(),
                opacity = 18,
                shape = ContainerShape.ROUNDED,
                corner = 24f,
                border = border.copy(color = Color(0x66FFFFFF).toArgb().toLong(), opacity = 40, width = 1.5f),
                padH = 22f,
                padV = 16f
            )
            TimeContainerPreset.FROSTED_GLASS -> Apply(
                mode = WidgetBackgroundMode.GLASS,
                glassBlur = 32f,
                glassTint = Color(0xFFFFFFFF).toArgb().toLong(),
                opacity = 26,
                shape = ContainerShape.ROUNDED,
                corner = 28f,
                border = border.copy(color = Color(0x8CFFFFFF).toArgb().toLong(), opacity = 55, width = 2f),
                padH = 24f,
                padV = 18f
            )
            TimeContainerPreset.DARK_CARD -> Apply(
                mode = WidgetBackgroundMode.SOLID,
                color = Color(0xFF000000).toArgb().toLong(),
                opacity = 55,
                shape = ContainerShape.ROUNDED,
                corner = 24f,
                border = border.copy(color = Color(0x4DFFFFFF).toArgb().toLong(), opacity = 30, width = 1f),
                shadow = shadow
            )
            TimeContainerPreset.LIGHT_CARD -> Apply(
                mode = WidgetBackgroundMode.SOLID,
                color = Color(0xFFFFFFFF).toArgb().toLong(),
                opacity = 45,
                shape = ContainerShape.ROUNDED,
                corner = 24f,
                border = border.copy(color = Color(0x99FFFFFF).toArgb().toLong(), opacity = 60, width = 1f),
                shadow = shadow
            )
            TimeContainerPreset.ROUNDED -> Apply(
                mode = WidgetBackgroundMode.SOLID,
                color = Color(0xFF000000).toArgb().toLong(),
                opacity = 40,
                shape = ContainerShape.ROUNDED,
                corner = 32f,
                shadow = shadow,
                padH = 22f,
                padV = 16f
            )
            TimeContainerPreset.CAPSULE -> Apply(
                mode = WidgetBackgroundMode.SOLID,
                color = Color(0xFF000000).toArgb().toLong(),
                opacity = 60,
                shape = ContainerShape.CAPSULE,
                border = border.copy(color = Color(0x73FFFFFF).toArgb().toLong(), opacity = 45, width = 2f),
                shadow = shadow,
                padH = 22f,
                padV = 12f
            )
            TimeContainerPreset.ELEGANT -> Apply(
                mode = WidgetBackgroundMode.GRADIENT,
                color = Color(0xFF1A1A2E).toArgb().toLong(),
                color2 = Color(0xFF16213E).toArgb().toLong(),
                direction = GradientDirection.LEFT_TO_RIGHT,
                opacity = 55,
                shape = ContainerShape.ROUNDED,
                corner = 28f,
                border = border.copy(color = Color(0xFFD4AF37).toArgb().toLong(), opacity = 50, width = 1.5f),
                shadow = shadow,
                padH = 22f,
                padV = 16f
            )
            TimeContainerPreset.CUSTOM -> return settings
        }

        return settings.copy(
            widgetBackgroundMode = c.mode,
            widgetBackgroundColor = c.color,
            widgetBackgroundColor2 = c.color2,
            widgetGradientDirection = c.direction,
            widgetBackgroundOpacity = c.opacity,
            widgetGlassBlur = c.glassBlur,
            widgetGlassTintColor = c.glassTint,
            widgetWallpaperBlurStrength = c.wallpaperBlur,
            widgetWallpaperTintColor = c.wallpaperTint,
            widgetImageFit = c.imageFit,
            widgetShape = c.shape,
            widgetCornerRadius = c.corner,
            widgetBorderEnabled = c.border.enabled,
            widgetBorderColor = c.border.color,
            widgetBorderWidth = c.border.width,
            widgetBorderOpacity = c.border.opacity,
            widgetBorderStyle = c.border.style,
            widgetShadowEnabled = c.shadow.enabled,
            widgetShadowColor = c.shadow.color,
            widgetShadowBlur = c.shadow.blur,
            widgetShadowSpread = c.shadow.spread,
            widgetShadowOffsetX = c.shadow.x,
            widgetShadowOffsetY = c.shadow.y,
            widgetPaddingHorizontal = c.padH,
            widgetPaddingVertical = c.padV
        )
    }

    private data class Apply(
        val mode: WidgetBackgroundMode = WidgetBackgroundMode.TRANSPARENT,
        val color: Long = 0xFF1A1A1A.toLong(),
        val color2: Long = 0xFF3949AB.toLong(),
        val direction: GradientDirection = GradientDirection.LEFT_TO_RIGHT,
        val opacity: Int = 50,
        val glassBlur: Float = 20f,
        val glassTint: Long = 0xFFFFFFFF.toLong(),
        val wallpaperBlur: Float = 16f,
        val wallpaperTint: Long = 0xFF000000.toLong(),
        val imageFit: BackgroundMode = BackgroundMode.CENTER_CROP,
        val shape: ContainerShape = ContainerShape.ROUNDED,
        val corner: Float = 24f,
        val border: Border = Border(enabled = false),
        val shadow: Shadow = Shadow(enabled = false),
        val padH: Float = 20f,
        val padV: Float = 14f
    )

    private data class Border(
        val enabled: Boolean = false,
        val color: Long = 0xFFFFFFFF.toLong(),
        val width: Float = 2f,
        val opacity: Int = 60,
        val style: ContainerBorderStyle = ContainerBorderStyle.SOLID
    )

    private data class Shadow(
        val enabled: Boolean = false,
        val color: Long = 0x66000000.toLong(),
        val blur: Float = 16f,
        val spread: Float = 0f,
        val x: Float = 0f,
        val y: Float = 6f
    )
}
