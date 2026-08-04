package com.prakash.pwall.service.render.layers

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import com.prakash.pwall.service.render.ClockDraw
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.RenderFrame

/**
 * Draws the clock time text. Shares the per-frame clock block with [DateLayer]
 * via [RenderFrame.clockBlock] so the combined clock + date block renders
 * exactly where the legacy single-block renderer placed it.
 *
 * The time may be a single line (horizontal layout) or several stacked lines
 * (vertical layouts); every line is drawn individually at its own baseline,
 * left-aligned for the horizontal layout or centered for the vertical ones.
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
        val centeredX = block.layout.timeCenteredX

        canvas.save()
        frame.breathing?.let { breathing ->
            val cx = block.layout.x + block.blockWidth / 2f
            val cy = block.layout.y + block.blockHeight / 2f
            canvas.scale(breathing.scale, breathing.scale, cx, cy)
        }
        val baseAlpha = frame.breathing?.alpha ?: 1f
        val glow = block.glowTime
        val transition = frame.timeTransition
        val newAlpha = baseAlpha * (transition?.progress ?: 1f)
        val oldLines = transition?.oldTimeText?.split("\n") ?: emptyList()

        val lines = block.timeLines
        val baselines = block.layout.timeLineBaselines
        for (i in lines.indices) {
            val baseline = baselines.getOrElse(i) { baselines.last() }
            val drawX = if (centeredX != null) {
                block.timePaint.textAlign = Paint.Align.CENTER
                centeredX
            } else {
                block.timePaint.textAlign = Paint.Align.LEFT
                block.layout.x
            }
            if (transition != null) {
                oldLines.getOrNull(i)?.let { old ->
                    ClockDraw.draw(
                        canvas, old,
                        drawX, baseline,
                        block.timePaint, glow, baseAlpha * (1f - transition.progress)
                    )
                }
            }
            ClockDraw.draw(
                canvas, lines[i],
                drawX, baseline,
                block.timePaint, glow, newAlpha
            )
        }
        canvas.restore()
    }
}
