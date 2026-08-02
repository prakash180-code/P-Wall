package com.prakash.pwall.service.render.layers

import android.graphics.Canvas
import android.graphics.Paint
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.RenderFrame

/**
 * Draws the clock time text. Shares the per-frame clock block with [DateLayer]
 * via [RenderFrame.clockBlock] so the combined clock + date block renders
 * exactly where the legacy single-block renderer placed it.
 */
class ClockLayer : Layer {

    override val id: String = "clock"

    override fun draw(canvas: Canvas, frame: RenderFrame) {
        val block = frame.clockBlock
        block.timePaint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            block.timeText,
            block.layout.x,
            block.layout.timeBaseline,
            block.timePaint
        )
    }
}
