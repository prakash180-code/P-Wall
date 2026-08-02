package com.prakash.pwall.service.render.layers

import android.graphics.Canvas
import android.graphics.Paint
import com.prakash.pwall.service.render.ClockBlockLayout
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.RenderFrame
import com.prakash.pwall.utils.ClockTextFormatter

/**
 * Draws the clock time text. Shares block positioning with [DateLayer] via
 * [ClockBlockLayout] so the combined clock + date block renders exactly where
 * the legacy single-block renderer placed it.
 */
class ClockLayer : Layer {

    override val id: String = "clock"

    override fun draw(canvas: Canvas, frame: RenderFrame) {
        val settings = frame.settings
        val timeText = ClockTextFormatter.formatTime(
            frame.now, settings.timeFormat, settings.showSeconds
        )
        val dateText = ClockTextFormatter.formatDate(frame.now, settings.dateFormat)

        val timePaint = ClockBlockLayout.timePaint(settings, frame.displayDensity)
        val datePaint = ClockBlockLayout.datePaint(settings, frame.displayDensity)
        val layout = ClockBlockLayout.compute(
            canvasWidth = canvas.width.toFloat(),
            canvasHeight = canvas.height.toFloat(),
            timeText = timeText,
            dateText = dateText,
            timePaint = timePaint,
            datePaint = datePaint,
            settings = settings
        )

        timePaint.textAlign = Paint.Align.LEFT
        canvas.drawText(timeText, layout.x, layout.timeBaseline, timePaint)
    }
}
