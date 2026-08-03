package com.prakash.pwall.service.render.layers

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import com.prakash.pwall.service.render.ClockBlockLayout
import com.prakash.pwall.service.render.ClockDraw
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.RenderFrame

/**
 * Draws the clock time text. Shares the per-frame clock block with [DateLayer]
 * via [RenderFrame.clockBlock] so the combined clock + date block renders
 * exactly where the legacy single-block renderer placed it.
 *
 * Premium effects: the glass glow renders a soft halo pass beneath the crisp
 * text, time changes cross-fade via [RenderFrame.timeTransition], and the
 * breathing pulse gently scales the whole block via [RenderFrame.breathing].
 */
class ClockLayer : Layer {

    override val id: String = "clock"

    @SuppressLint("UseKtx")
    override fun draw(canvas: Canvas, frame: RenderFrame) {
        val block = frame.clockBlock
        block.timePaint.textAlign = Paint.Align.LEFT

        canvas.save()
        frame.breathing?.let { breathing ->
            val cx = block.layout.x + block.blockWidth / 2f
            val cy = block.layout.y + block.blockHeight / 2f
            canvas.scale(breathing.scale, breathing.scale, cx, cy)
        }
        val baseAlpha = frame.breathing?.alpha ?: 1f
        val glow = ClockBlockLayout.glowPaint(block.timePaint, frame.settings, frame.displayDensity)
        val transition = frame.timeTransition

        val newAlpha = baseAlpha * (transition?.progress ?: 1f)
        if (transition != null && transition.oldTimeText != null) {
            ClockDraw.draw(
                canvas, transition.oldTimeText,
                block.layout.x, block.layout.timeBaseline,
                block.timePaint, glow, baseAlpha * (1f - transition.progress)
            )
        }
        ClockDraw.draw(
            canvas, block.timeText,
            block.layout.x, block.layout.timeBaseline,
            block.timePaint, glow, newAlpha
        )
        canvas.restore()
    }
}
