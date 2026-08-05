package com.prakash.pwall.service.render.layers

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import com.prakash.pwall.service.render.ClockWidgetLayout
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.Releasable
import com.prakash.pwall.service.render.RenderFrame
import kotlin.math.max
import kotlin.math.min

/**
 * Frosted-glass panel behind the clock block (premium "Glass Clock"). Draws a
 * real backdrop blur by sampling the wallpaper region behind the block into a
 * small scratch bitmap and upscaling it, then composites a translucent panel
 * fill and an optional border. The scratch/upscaled bitmaps, the paints and the
 * clip path are all cached and reused so per-frame cost stays tiny (no
 * allocations after the first frame with identical settings/layout).
 *
 * Registers immediately below the clock layer so the depth foreground still
 * renders on top (the clock, panel included, stays behind the subject).
 */
@SuppressLint("UseKtx")
class GlassPanelLayer : Layer, Releasable {

    override val id: String = "glass-panel"

    private var scratch: Bitmap? = null
    private var blurOut: Bitmap? = null
    private var cachedKey: Pair<Int, Int>? = null

    private val filterPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    private var clipPath: Path? = null
    private var clipKey: String? = null

    override fun draw(canvas: Canvas, frame: RenderFrame) {
        val settings = frame.settings
        if (!settings.glassEnabled) return

        val engine = frame.widgetEngine
        val density = frame.displayDensity
        val corner = settings.glassCornerRadius * density
        val padX = 26f * density
        val padY = 22f * density
        val union = unionRect(engine.timeBlock, engine.dateBlock) ?: return
        if (union.width() <= 0f || union.height() <= 0f) return
        val rect = RectF(
            union.left - padX,
            union.top - padY,
            union.right + padX,
            union.bottom + padY
        )

        canvas.save()
        val clip = clipPath(rect, corner)
        canvas.clipPath(clip)

        if (frame.backgroundBitmap != null && settings.glassBlurRadius > 0f) {
            drawBlurredBackdrop(canvas, frame, rect, settings.glassBlurRadius * density)
        }

        val panelAlpha = (0xFF * settings.glassPanelOpacity.coerceIn(0, 100) / 100)
        val panelPaint = frame.paintCache?.get("glass-panel|fill|$panelAlpha") {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(panelAlpha, 0xFF, 0xFF, 0xFF)
            }
        } ?: Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(panelAlpha, 0xFF, 0xFF, 0xFF)
        }
        canvas.drawRoundRect(rect, corner, corner, panelPaint)

        if (settings.glassBorderWidth > 0f) {
            val borderWidth = settings.glassBorderWidth * density
            val borderKey = "glass-panel|border|$borderWidth|${settings.glassBorderColor.toInt()}"
            val borderPaint = frame.paintCache?.get(borderKey) {
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = borderWidth
                    color = settings.glassBorderColor.toInt()
                }
            } ?: Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = borderWidth
                color = settings.glassBorderColor.toInt()
            }
            canvas.drawRoundRect(rect, corner, corner, borderPaint)
        }
        canvas.restore()
    }

    /**
     * Reuses the rounded-rect clip path while the panel geometry is unchanged;
     * a fresh path is built only when the block moved or a setting changed.
     */
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

    /** Bounding box covering both widgets (or the single visible one). */
    private fun unionRect(
        time: ClockWidgetLayout.WidgetBlock?,
        date: ClockWidgetLayout.WidgetBlock?
    ): RectF? {
        val list = listOfNotNull(time, date)
        if (list.isEmpty()) return null
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        for (block in list) {
            left = min(left, block.x)
            top = min(top, block.y)
            right = max(right, block.x + block.width)
            bottom = max(bottom, block.y + block.height)
        }
        return RectF(left, top, right, bottom)
    }

    /**
     * Samples the wallpaper behind [rect] into a tiny bitmap (downscale ~= blur
     * radius), then draws it scaled back up — a cheap, allocation-free blur.
     */
    private fun drawBlurredBackdrop(
        canvas: Canvas,
        frame: RenderFrame,
        rect: RectF,
        blurRadius: Float
    ) {
        val background = frame.backgroundBitmap ?: return
        val scale = (1f / (1f + blurRadius / 3f)).coerceIn(0.08f, 1f)
        val sw = (rect.width() * scale).toInt().coerceAtLeast(1)
        val sh = (rect.height() * scale).toInt().coerceAtLeast(1)
        val dw = rect.width().toInt().coerceAtLeast(1)
        val dh = rect.height().toInt().coerceAtLeast(1)
        if (sw * sh <= 0) return

        val (sample, output) = obtain(sw, sh, dw, dh)

        val sampleCanvas = Canvas(sample)
        sampleCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        sampleCanvas.save()
        sampleCanvas.scale(scale, scale)
        sampleCanvas.translate(-rect.left, -rect.top)
        sampleCanvas.drawBitmap(background, frame.backgroundMatrix, null)
        sampleCanvas.restore()

        val outCanvas = Canvas(output)
        outCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        outCanvas.drawBitmap(
            sample,
            null,
            Rect(0, 0, dw, dh),
            filterPaint
        )

        canvas.drawBitmap(output, rect.left, rect.top, null)
    }

    private fun obtain(sw: Int, sh: Int, dw: Int, dh: Int): Pair<Bitmap, Bitmap> {
        val key = sw to sh
        var sample = scratch
        var output = blurOut
        if (cachedKey != key || sample == null || output == null) {
            sample = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
            output = Bitmap.createBitmap(dw, dh, Bitmap.Config.ARGB_8888)
            scratch = sample
            blurOut = output
            cachedKey = key
        }
        return (sample to output)
    }

    override fun release() {
        scratch?.recycle()
        scratch = null
        blurOut?.recycle()
        blurOut = null
        cachedKey = null
        clipPath = null
        clipKey = null
    }
}
