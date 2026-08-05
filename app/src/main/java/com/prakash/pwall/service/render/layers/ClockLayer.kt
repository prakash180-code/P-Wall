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
 * Draws the time widget. Uses the per-frame engine result
 * ([RenderFrame.widgetEngine]) so the time renders independently of the date
 * (any layout, style, size or position) while the classic combination stays
 * byte-for-byte identical to the legacy single-block renderer.
 *
 * Premium effects: the glass glow / style glow render a soft halo pass beneath
 * the crisp text, outline styles draw a stroke pass first, glass styles draw a
 * frosted chip, time changes cross-fade via [RenderFrame.timeTransition], and
 * the breathing pulse gently scales the block.
 */
class ClockLayer : Layer {

    override val id: String = "clock"

    @SuppressLint("UseKtx")
    override fun draw(canvas: Canvas, frame: RenderFrame) {
        val block = frame.widgetEngine.timeBlock ?: return

        canvas.save()
        frame.breathing?.let { breathing ->
            canvas.scale(breathing.scale, breathing.scale, block.centerX, block.centerY)
        }
        val baseAlpha = frame.breathing?.alpha ?: 1f
        val transition = frame.timeTransition
        val newAlpha = baseAlpha * (transition?.progress ?: 1f)
        val oldLines = transition?.oldTimeText?.split("\n") ?: emptyList()

        block.chipRect?.let { rect ->
            val chipAlpha = (0xFF * block.chipAlpha.coerceIn(0f, 1f)).toInt()
            val chipPaint = frame.paintCache?.get(
                "widget-chip|clock|$chipAlpha|${rect.left}|${rect.top}|${rect.right}|${rect.bottom}|${block.chipCorner}"
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
