package com.prakash.pwall.service.render.layers

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import com.prakash.pwall.service.render.ClockDraw
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.RenderFrame

/**
 * Draws the date text below the clock. Shares the per-frame clock block with
 * [ClockLayer] via [RenderFrame.clockBlock] so the combined clock + date block
 * renders exactly where the legacy single-block renderer placed it.
 *
 * Applies the same glass glow, cross-fade and breathing effects as the clock
 * layer (glow halo pass + crisp pass) so the date stays visually consistent.
 */
class DateLayer : Layer {

    override val id: String = "date"

    @SuppressLint("UseKtx")
    override fun draw(canvas: Canvas, frame: RenderFrame) {
        val block = frame.clockBlock
        block.datePaint.textAlign = Paint.Align.LEFT

        canvas.save()
        frame.breathing?.let { breathing ->
            val cx = block.layout.x + block.blockWidth / 2f
            val cy = block.layout.y + block.blockHeight / 2f
            canvas.scale(breathing.scale, breathing.scale, cx, cy)
        }
        val baseAlpha = frame.breathing?.alpha ?: 1f
        val glow = block.glowDate
        val transition = frame.timeTransition

        val newAlpha = baseAlpha * (transition?.progress ?: 1f)
        if (transition != null && transition.oldDateText != null) {
            ClockDraw.draw(
                canvas, transition.oldDateText,
                block.layout.x, block.layout.dateBaseline,
                block.datePaint, glow, baseAlpha * (1f - transition.progress)
            )
        }
        ClockDraw.draw(
            canvas, block.dateText,
            block.layout.x, block.layout.dateBaseline,
            block.datePaint, glow, newAlpha
        )
        canvas.restore()
    }
}
