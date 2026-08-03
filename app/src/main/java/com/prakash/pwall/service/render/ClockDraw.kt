package com.prakash.pwall.service.render

import android.graphics.Canvas
import android.graphics.Paint
import com.prakash.pwall.service.render.layers.ClockLayer
import com.prakash.pwall.service.render.layers.DateLayer

/**
 * Shared drawing helpers for the clock/date layers: two-pass text rendering
 * (glow halo pass + crisp pass) combined with the per-frame alpha that fades
 * text in/out and applies the breathing pulse.
 *
 * Kept out of [ClockLayer]/[DateLayer] so both layers use byte-identical logic.
 */
object ClockDraw {

    /**
     * Draws [text] once (or twice when a glass glow is active) at [alphaScale]
     * (0..1) of the paint's configured alpha. Alpha 0 short-circuits so disabled
     * transition passes cost nothing.
     */
    fun draw(
        canvas: Canvas,
        text: String,
        x: Float,
        baseline: Float,
        paint: Paint,
        glowPaint: Paint?,
        alphaScale: Float
    ) {
        if (text.isEmpty()) return
        val scale = alphaScale.coerceIn(0f, 1f)
        if (scale <= 0f) return
        val alpha = (paint.alpha * scale).toInt().coerceIn(0, 255)
        if (alpha <= 0) return
        if (glowPaint != null) {
            canvas.drawText(text, x, baseline, Paint(glowPaint).apply { this.alpha = alpha })
        }
        canvas.drawText(text, x, baseline, Paint(paint).apply { this.alpha = alpha })
    }
}
