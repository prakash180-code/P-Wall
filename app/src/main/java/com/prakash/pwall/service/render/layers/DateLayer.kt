package com.prakash.pwall.service.render.layers

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.prakash.pwall.service.render.ClockDraw
import com.prakash.pwall.service.render.ClockWidgetLayout
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.RenderFrame

/**
 * Draws the date widget from the per-frame engine result
 * ([RenderFrame.widgetEngine]). Linked to the time (classic behavior) or at its
 * own position; applies the same glow / stroke / chip / cross-fade / breathing
 * effects as the clock layer so the widgets stay visually consistent.
 */
class DateLayer : Layer {

    override val id: String = "date"

    @SuppressLint("UseKtx")
    override fun draw(canvas: Canvas, frame: RenderFrame) {
        val block = frame.widgetEngine.dateBlock ?: return

        val animated = frame.settings.dateAnimated
        canvas.save()
        if (animated) {
            frame.breathing?.let { breathing ->
                canvas.scale(breathing.scale, breathing.scale, block.centerX, block.centerY)
            }
        }
        val baseAlpha = if (animated) frame.breathing?.alpha ?: 1f else 1f
        val transition = if (animated) frame.timeTransition else null
        val newAlpha = baseAlpha * (transition?.progress ?: 1f)
        val oldLines = if (animated && frame.widgetEngine.classic) {
            listOfNotNull(transition?.oldDateText)
        } else if (animated) {
            transition?.oldDateText?.split("\n") ?: emptyList()
        } else {
            emptyList()
        }

        block.chipRect?.let { rect ->
            val chipAlpha = (0xFF * block.chipAlpha.coerceIn(0f, 1f)).toInt()
            val chipPaint = frame.paintCache?.get(
                "widget-chip|date|$chipAlpha|${rect.left}|${rect.top}|${rect.right}|${rect.bottom}|${block.chipCorner}"
            ) {
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(chipAlpha, 0xFF, 0xFF, 0xFF)
                }
            } ?: Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(chipAlpha, 0xFF, 0xFF, 0xFF)
            }
            canvas.drawRoundRect(rect, block.chipCorner, block.chipCorner, chipPaint)
        }

        val chunks = block.chunks
        for (i in chunks.indices) {
            val chunk = chunks[i]
            val paint = block.fillPaint
            paint.textAlign = when (chunk.align) {
                ClockWidgetLayout.Align.LEFT -> Paint.Align.LEFT
                ClockWidgetLayout.Align.CENTER -> Paint.Align.CENTER
                ClockWidgetLayout.Align.RIGHT -> Paint.Align.RIGHT
            }
            if (transition != null) {
                oldLines.getOrNull(i)?.let { old ->
                    ClockDraw.draw(
                        canvas, old,
                        chunk.x, chunk.baseline,
                        paint, block.glowPaint,
                        baseAlpha * (1f - transition.progress),
                        block.strokePaint
                    )
                }
            }
            ClockDraw.draw(
                canvas, chunk.text,
                chunk.x, chunk.baseline,
                paint, block.glowPaint, newAlpha,
                block.strokePaint
            )
        }
        canvas.restore()
    }
}
