package com.prakash.pwall.service.render.layers

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import com.prakash.pwall.service.render.RenderFrame

/**
 * Shared real backdrop-blur implementation used by the glass panel and the time
 * widget container's GLASS / WALLPAPER_BLUR fills. Samples the wallpaper region
 * behind [rect] into a tiny scratch bitmap (downscale ~= blur radius) and draws
 * it scaled back up — a cheap, allocation-free blur. The scratch/upscaled
 * bitmaps and the filter paint are cached and reused so per-frame cost stays
 * tiny (no allocations after the first frame with identical settings/layout).
 */
@SuppressLint("UseKtx")
class BackdropBlur {

    private var scratch: Bitmap? = null
    private var blurOut: Bitmap? = null
    private var cachedKey: IntArray? = null

    private val filterPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    /**
     * Blurs the wallpaper region behind [rect] onto [canvas]. No-op when there
     * is no background bitmap.
     */
    fun draw(canvas: Canvas, frame: RenderFrame, rect: RectF, blurRadius: Float) {
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
        val key = intArrayOf(sw, sh, dw, dh)
        var sample = scratch
        var output = blurOut
        if (cachedKey?.contentEquals(key) != true || sample == null || output == null) {
            sample = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
            output = Bitmap.createBitmap(dw, dh, Bitmap.Config.ARGB_8888)
            scratch = sample
            blurOut = output
            cachedKey = key
        }
        return (sample to output)
    }

    fun release() {
        scratch?.recycle()
        scratch = null
        blurOut?.recycle()
        blurOut = null
        cachedKey = null
    }
}
