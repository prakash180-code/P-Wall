package com.prakash.pwall.service.render.layers

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.model.WidgetBackgroundMode
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.Releasable
import com.prakash.pwall.service.render.RenderFrame
import com.prakash.pwall.service.render.TimeContainer
import com.prakash.pwall.utils.ImageLoader

/**
 * Draws the time widget container (v1.1.3): the user-controlled background
 * behind the time text. Registered directly below the clock layer so the text
 * always renders on top. A no-op whenever the container is TRANSPARENT (the
 * default), so the classic render stays byte-for-byte identical to the core
 * stack.
 *
 * Modes: SOLID fill, GRADIENT (two colors + direction), GLASS / WALLPAPER_BLUR
 * (real backdrop blur via [BackdropBlur] plus a tint), and IMAGE (a custom
 * bitmap fitted with the chosen fit). A rounded shape, an optional dashed/dotted
 * border and an optional soft shadow are composited on top. Paints, the clip
 * path and the image bitmap are cached and reused so per-frame cost stays tiny.
 */
@SuppressLint("UseKtx")
class TimeContainerLayer : Layer, Releasable {

    override val id: String = "time-container"

    private val blur = BackdropBlur()

    private var clipPath: Path? = null
    private var clipKey: String? = null

    private var imageBitmap: Bitmap? = null
    private var imageKey: String? = null

    override fun draw(canvas: Canvas, frame: RenderFrame) {
        val settings = frame.settings
        if (TimeContainer.isInactive(settings)) return
        val time = frame.widgetEngine.timeBlock ?: return
        if (time.width <= 0f || time.height <= 0f) return

        val density = frame.displayDensity
        val geom = TimeContainer.geometry(
            time.x, time.y, time.width, time.height, settings, density
        )
        val corner = TimeContainer.cornerRadius(
            settings.widgetShape, settings.widgetCornerRadius,
            density, geom.width, geom.height
        )
        val rect = RectF(geom.left, geom.top, geom.right, geom.bottom)

        if (settings.widgetShadowEnabled) {
            drawShadow(canvas, frame, geom, corner, settings, density)
        }

        canvas.save()
        canvas.clipPath(clipPath(rect, corner))

        when (settings.widgetBackgroundMode) {
            WidgetBackgroundMode.SOLID -> drawSolid(canvas, frame, rect, settings)
            WidgetBackgroundMode.GRADIENT -> drawGradient(canvas, frame, rect, geom, settings)
            WidgetBackgroundMode.GLASS -> drawGlass(canvas, frame, rect, settings, density)
            WidgetBackgroundMode.WALLPAPER_BLUR -> drawWallpaperBlur(canvas, frame, rect, settings, density)
            WidgetBackgroundMode.IMAGE -> drawImage(canvas, frame, rect, geom, settings)
            WidgetBackgroundMode.TRANSPARENT -> Unit
        }

        canvas.restore()

        if (settings.widgetBorderEnabled && settings.widgetBorderWidth > 0f) {
            drawBorder(canvas, frame, rect, corner, settings, density)
        }
    }

    // --- Fills ---------------------------------------------------------------

    private fun drawSolid(
        canvas: Canvas,
        frame: RenderFrame,
        rect: RectF,
        settings: WallpaperSettings
    ) {
        val alpha = alphaOf(settings.widgetBackgroundOpacity)
        if (alpha <= 0) return
        val color = withAlpha(settings.widgetBackgroundColor, alpha)
        val paint = frame.paintCache?.get("tc|solid|$color|${rect.left}|${rect.top}|${rect.right}|${rect.bottom}") {
            Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        } ?: Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawRect(rect, paint)
    }

    private fun drawGradient(
        canvas: Canvas,
        frame: RenderFrame,
        rect: RectF,
        geom: TimeContainer.ContainerRect,
        settings: WallpaperSettings
    ) {
        val alpha = alphaOf(settings.widgetBackgroundOpacity)
        if (alpha <= 0) return
        val startColor = withAlpha(settings.widgetBackgroundColor, alpha)
        val endColor = withAlpha(settings.widgetBackgroundColor2, alpha)
        val (start, end) = TimeContainer.gradientEndpoints(settings.widgetGradientDirection, geom)
        val shader = LinearGradient(
            start.first, start.second, end.first, end.second,
            startColor, endColor, Shader.TileMode.CLAMP
        )
        val key = "tc|gradient|${startColor}|${endColor}|${start.first}|${start.second}|${end.first}|${end.second}|${rect.left}|${rect.top}|${rect.right}|${rect.bottom}"
        val paint = frame.paintCache?.get(key) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        } ?: Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        canvas.drawRect(rect, paint)
    }

    private fun drawGlass(
        canvas: Canvas,
        frame: RenderFrame,
        rect: RectF,
        settings: WallpaperSettings,
        density: Float
    ) {
        drawBlurredBackdrop(canvas, frame, rect, settings.widgetGlassBlur * density)
        drawTintFill(canvas, frame, rect, settings.widgetGlassTintColor, settings.widgetBackgroundOpacity)
    }

    private fun drawWallpaperBlur(
        canvas: Canvas,
        frame: RenderFrame,
        rect: RectF,
        settings: WallpaperSettings,
        density: Float
    ) {
        drawBlurredBackdrop(canvas, frame, rect, settings.widgetWallpaperBlurStrength * density)
        drawTintFill(canvas, frame, rect, settings.widgetWallpaperTintColor, settings.widgetBackgroundOpacity)
    }

    private fun drawImage(
        canvas: Canvas,
        frame: RenderFrame,
        rect: RectF,
        geom: TimeContainer.ContainerRect,
        settings: WallpaperSettings
    ) {
        val bitmap = widgetImage(settings) ?: return
        val drawRect = TimeContainer.imageDrawRect(
            bitmap.width, bitmap.height, geom, settings.widgetImageFit
        ) ?: return
        val alpha = alphaOf(settings.widgetBackgroundOpacity)
        if (alpha <= 0) return
        val key = "tc|image|$alpha|${drawRect.left}|${drawRect.top}|${drawRect.right}|${drawRect.bottom}"
        val paint = frame.paintCache?.get(key) {
            Paint(Paint.FILTER_BITMAP_FLAG).apply { this.alpha = alpha }
        } ?: Paint(Paint.FILTER_BITMAP_FLAG).apply { this.alpha = alpha }
        canvas.drawBitmap(
            bitmap,
            null,
            RectF(drawRect.left, drawRect.top, drawRect.right, drawRect.bottom),
            paint
        )
    }

    // --- Layers on top of the fill -------------------------------------------

    private fun drawBlurredBackdrop(
        canvas: Canvas,
        frame: RenderFrame,
        rect: RectF,
        blurRadius: Float
    ) {
        if (frame.backgroundBitmap == null || blurRadius <= 0f) return
        blur.draw(canvas, frame, rect, blurRadius)
    }

    private fun drawTintFill(
        canvas: Canvas,
        frame: RenderFrame,
        rect: RectF,
        tintColor: Long,
        opacityPercent: Int
    ) {
        val alpha = alphaOf(opacityPercent)
        if (alpha <= 0) return
        val color = withAlpha(tintColor, alpha)
        val key = "tc|tint|$color|${rect.left}|${rect.top}|${rect.right}|${rect.bottom}"
        val paint = frame.paintCache?.get(key) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        } ?: Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawRect(rect, paint)
    }

    private fun drawBorder(
        canvas: Canvas,
        frame: RenderFrame,
        rect: RectF,
        corner: Float,
        settings: WallpaperSettings,
        density: Float
    ) {
        val width = settings.widgetBorderWidth * density
        val alpha = alphaOf(settings.widgetBorderOpacity)
        if (alpha <= 0) return
        val color = withAlpha(settings.widgetBorderColor, alpha)
        val dash = TimeContainer.borderDash(settings.widgetBorderStyle, width)
        val key = "tc|border|$color|$width|${settings.widgetBorderStyle.name}|${rect.left}|${rect.top}|${rect.right}|${rect.bottom}|$corner"
        val paint = frame.paintCache?.get(key) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = width
                this.color = color
                if (dash != null) pathEffect = DashPathEffect(dash, 0f)
            }
        } ?: Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = width
            this.color = color
            if (dash != null) pathEffect = DashPathEffect(dash, 0f)
        }
        canvas.drawRoundRect(rect, corner, corner, paint)
    }

    private fun drawShadow(
        canvas: Canvas,
        frame: RenderFrame,
        geom: TimeContainer.ContainerRect,
        corner: Float,
        settings: WallpaperSettings,
        density: Float
    ) {
        val blurPx = settings.widgetShadowBlur * density
        if (blurPx <= 0f) return
        val dx = settings.widgetShadowOffsetX * density
        val dy = settings.widgetShadowOffsetY * density
        val shadowRect = geom
            .inflated(settings.widgetShadowSpread * density)
            .shifted(dx, dy)
        val color = settings.widgetShadowColor.toInt()
        val key = "tc|shadow|$color|$blurPx|$dx|$dy|${shadowRect.left}|${shadowRect.top}|${shadowRect.right}|${shadowRect.bottom}|$corner"
        val paint = frame.paintCache?.get(key) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.TRANSPARENT
                setShadowLayer(blurPx, dx, dy, color)
            }
        } ?: Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.TRANSPARENT
            setShadowLayer(blurPx, dx, dy, color)
        }
        canvas.drawRoundRect(
            RectF(shadowRect.left, shadowRect.top, shadowRect.right, shadowRect.bottom),
            corner, corner, paint
        )
    }

    // --- Helpers ---------------------------------------------------------------

    private fun clipPath(rect: RectF, corner: Float): Path {
        val key = "clip|${rect.left}|${rect.top}|${rect.right}|${rect.bottom}|$corner"
        val existing = clipPath
        if (existing != null && key == clipKey) return existing
        val path = Path().apply {
            addRoundRect(rect, corner, corner, Path.Direction.CW)
        }
        clipPath = path
        clipKey = key
        return path
    }

    /** Cached, recycled decode of the custom image (falls back to the wallpaper). */
    private fun widgetImage(settings: WallpaperSettings): Bitmap? {
        val path = settings.widgetImagePath ?: settings.selectedImagePath ?: return null
        if (path != imageKey) {
            val decoded = ImageLoader.cached(path) ?: ImageLoader.decodeSampled(path)
            if (decoded != null && decoded != imageBitmap) {
                imageBitmap?.recycle()
                imageBitmap = decoded
                imageKey = path
            }
        }
        return imageBitmap
    }

    private fun alphaOf(opacityPercent: Int): Int =
        (0xFF * opacityPercent.coerceIn(0, 100) / 100)

    private fun withAlpha(color: Long, alpha: Int): Int {
        val rgb = color.toInt() and 0xFFFFFF
        return Color.argb(alpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb))
    }

    override fun release() {
        blur.release()
        clipPath = null
        clipKey = null
        imageBitmap?.recycle()
        imageBitmap = null
        imageKey = null
    }
}
