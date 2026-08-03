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
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.RenderFrame

/**
 * Frosted-glass panel behind the clock block (premium "Glass Clock"). Draws a
 * real backdrop blur by sampling the wallpaper region behind the block into a
 * small scratch bitmap and upscaling it, then composites a translucent panel
 * fill and an optional border. The scratch/upscaled bitmaps are cached and
 * reused so per-frame cost stays tiny (no allocations after the first frame).
 *
 * Registers immediately below the clock layer so the depth foreground still
 * renders on top (the clock, panel included, stays behind the subject).
 */
@SuppressLint("UseKtx")
class GlassPanelLayer : Layer {

    override val id: String = "glass-panel"

    private var scratch: Bitmap? = null
    private var blurOut: Bitmap? = null
    private var cachedKey: Pair<Int, Int>? = null

    private val filterPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    override fun draw(canvas: Canvas, frame: RenderFrame) {
        val settings = frame.settings
        if (!settings.glassEnabled) return

        val block = frame.clockBlock
        if (block.blockWidth <= 0f || block.blockHeight <= 0f) return

        val density = frame.displayDensity
        val corner = settings.glassCornerRadius * density
        val padX = 26f * density
        val padY = 22f * density
        val rect = RectF(
            block.layout.x - padX,
            block.layout.y - padY,
            block.layout.x + block.blockWidth + padX,
            block.layout.y + block.blockHeight + padY
        )

        canvas.save()
        val clip = Path().apply {
            addRoundRect(rect, corner, corner, Path.Direction.CW)
        }
        canvas.clipPath(clip)

        if (frame.backgroundBitmap != null && settings.glassBlurRadius > 0f) {
            drawBlurredBackdrop(canvas, frame, rect, settings.glassBlurRadius * density)
        }

        val panelAlpha = (0xFF * settings.glassPanelOpacity.coerceIn(0, 100) / 100)
        val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(panelAlpha, 0xFF, 0xFF, 0xFF)
        }
        canvas.drawRoundRect(rect, corner, corner, panelPaint)

        if (settings.glassBorderWidth > 0f) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = settings.glassBorderWidth * density
                color = settings.glassBorderColor.toInt()
            }
            canvas.drawRoundRect(rect, corner, corner, borderPaint)
        }
        canvas.restore()
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
}
