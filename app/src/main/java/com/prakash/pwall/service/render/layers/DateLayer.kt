package com.prakash.pwall.service.render.layers

import android.graphics.Canvas
import android.graphics.Paint
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.RenderFrame

/**
 * Draws the date text below the clock. Shares the per-frame clock block with
 * [ClockLayer] via [RenderFrame.clockBlock] so the combined clock + date block
 * renders exactly where the legacy single-block renderer placed it.
 */
class DateLayer : Layer {

    override val id: String = "date"

    override fun draw(canvas: Canvas, frame: RenderFrame) {
        val block = frame.clockBlock
        block.datePaint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            block.dateText,
            block.layout.x,
            block.layout.dateBaseline,
            block.datePaint
        )
    }
}
