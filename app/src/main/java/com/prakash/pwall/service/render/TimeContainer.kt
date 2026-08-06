package com.prakash.pwall.service.render

import com.prakash.pwall.data.model.BackgroundMode
import com.prakash.pwall.data.model.ContainerBorderStyle
import com.prakash.pwall.data.model.ContainerShape
import com.prakash.pwall.data.model.GradientDirection
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.model.WidgetBackgroundMode
import kotlin.math.min

/**
 * Pure geometry + config math for the time widget container (v1.1.3). No
 * Android types, so everything here is JVM-testable: the layer converts the
 * [ContainerRect] results into [android.graphics.RectF] / [android.graphics.Path]
 * only when drawing.
 *
 * The container wraps the time widget's text box with the user's padding and
 * follows the chosen shape / corner radius. All values are already in pixels
 * once a density is provided.
 */
object TimeContainer {

    /** A rect expressed in pixels, free of Android types. */
    data class ContainerRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top

        /** Inflated by [spreadPx] on every side (used for shadow growth). */
        fun inflated(spreadPx: Float): ContainerRect = ContainerRect(
            left - spreadPx,
            top - spreadPx,
            right + spreadPx,
            bottom + spreadPx
        )

        /** Shifted by [dx]/[dy] (used for shadow offset). */
        fun shifted(dx: Float, dy: Float): ContainerRect = ContainerRect(
            left + dx,
            top + dy,
            right + dx,
            bottom + dy
        )
    }

    /** True when the container should draw nothing at all. */
    fun isInactive(settings: WallpaperSettings): Boolean =
        settings.widgetBackgroundMode == WidgetBackgroundMode.TRANSPARENT

    /** Resolved fill/tint opacity as a 0..1 fraction. */
    fun fillAlpha(settings: WallpaperSettings): Float =
        (settings.widgetBackgroundOpacity.coerceIn(0, 100) / 100f)

    /** Container box around the time text box, including the user's padding. */
    fun geometry(
        timeX: Float,
        timeY: Float,
        timeWidth: Float,
        timeHeight: Float,
        settings: WallpaperSettings,
        density: Float
    ): ContainerRect {
        val padH = settings.widgetPaddingHorizontal * density
        val padV = settings.widgetPaddingVertical * density
        return ContainerRect(
            timeX - padH,
            timeY - padV,
            timeX + timeWidth + padH,
            timeY + timeHeight + padV
        )
    }

    /**
     * Corner radius (px) for a shape on a box of [width] x [height]. Capsule
     * and Circle/Oval collapse to a stadium (half the shorter side); Rounded
     * uses the user's radius; None/Rectangle are sharp.
     */
    fun cornerRadius(
        shape: ContainerShape,
        cornerDp: Float,
        density: Float,
        width: Float,
        height: Float
    ): Float = when (shape) {
        ContainerShape.NONE, ContainerShape.RECTANGLE -> 0f
        ContainerShape.ROUNDED -> (cornerDp.coerceIn(0f, 100f) * density)
        ContainerShape.CAPSULE, ContainerShape.CIRCLE ->
            min(width, height) / 2f
    }

    /**
     * Where the custom image lands inside the container for a [BackgroundMode]
     * fit. Returns null when the source is empty or the container has no area.
     * FIT letterboxes, FILL and CENTER_CROP cover (cropping overflow), and
     * STRETCH fills exactly (allowing distortion).
     */
    fun imageDrawRect(
        srcWidth: Int,
        srcHeight: Int,
        container: ContainerRect,
        fit: BackgroundMode
    ): ContainerRect? {
        if (srcWidth <= 0 || srcHeight <= 0) return null
        val dstW = container.width
        val dstH = container.height
        if (dstW <= 0f || dstH <= 0f) return null
        val centerX = (container.left + container.right) / 2f
        val centerY = (container.top + container.bottom) / 2f

        fun centered(w: Float, h: Float): ContainerRect =
            ContainerRect(centerX - w / 2f, centerY - h / 2f, centerX + w / 2f, centerY + h / 2f)

        return when (fit) {
            BackgroundMode.FIT -> {
                val scale = min(dstW / srcWidth, dstH / srcHeight)
                centered(srcWidth * scale, srcHeight * scale)
            }
            BackgroundMode.FILL, BackgroundMode.CENTER_CROP -> {
                val scale = maxOf(dstW / srcWidth, dstH / srcHeight)
                centered(srcWidth * scale, srcHeight * scale)
            }
            BackgroundMode.STRETCH -> container
            BackgroundMode.CUSTOM -> centered(dstW, dstH)
        }
    }

    /** Start/end points (px) for the gradient shader of a [GradientDirection]. */
    fun gradientEndpoints(
        direction: GradientDirection,
        rect: ContainerRect
    ): Pair<Pair<Float, Float>, Pair<Float, Float>> {
        val (l, t, r, b) = rect
        return when (direction) {
            GradientDirection.LEFT_TO_RIGHT -> (l to t) to (r to t)
            GradientDirection.RIGHT_TO_LEFT -> (r to t) to (l to t)
            GradientDirection.TOP_TO_BOTTOM -> (l to t) to (l to b)
            GradientDirection.BOTTOM_TO_TOP -> (l to b) to (l to t)
            GradientDirection.DIAGONAL -> (l to b) to (r to t)
        }
    }

    /**
     * Dash intervals for a [ContainerBorderStyle] with a [widthPx] stroke.
     * Solid returns null (no dash); dashed is 2x dash / 1.5x gap; dotted is
     * 1x dot / 1.5x gap.
     */
    fun borderDash(style: ContainerBorderStyle, widthPx: Float): FloatArray? =
        when (style) {
            ContainerBorderStyle.SOLID -> null
            ContainerBorderStyle.DASHED -> floatArrayOf(widthPx * 2f, widthPx * 1.5f)
            ContainerBorderStyle.DOTTED -> floatArrayOf(widthPx, widthPx * 1.5f)
        }
}
