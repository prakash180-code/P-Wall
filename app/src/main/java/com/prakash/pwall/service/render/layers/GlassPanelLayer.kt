package com.prakash.pwall.service.render.layers

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.prakash.pwall.service.render.ClockWidgetLayout
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.Releasable
import com.prakash.pwall.service.render.RenderFrame
import com.prakash.pwall.service.render.TimeContainer
import kotlin.math.max
import kotlin.math.min

/**
 * Frosted-glass panel behind the clock block (premium "Glass Clock"). Draws a
 * real backdrop blur by sampling the wallpaper region behind the block into a
 * small scratch bitmap and upscaling it, then composites a translucent panel
 * fill and an optional border. The scratch/upscaled bitmaps, the paints and the
 * clip path are all cached and reused so per-frame cost stays tiny (no
 * allocations after the first frame with identical settings/layout).
 *
 * Registers immediately below the clock layer so the depth foreground still
 * renders on top (the clock, panel included, stays behind the subject).
 *
 * The panel is suppressed entirely while the time container is TRANSPARENT:
 * the Time Background setting is the single owner of anything drawn behind the
 * clock, so "Transparent" always means no box at all.
 */
@SuppressLint("UseKtx")
class GlassPanelLayer : Layer, Releasable {

    override val id: String = "glass-panel"

    private val blur = BackdropBlur()

    private var clipPath: Path? = null
    private var clipKey: String? = null

    override fun draw(canvas: Canvas, frame: RenderFrame) {
        val settings = frame.settings
        if (!settings.glassEnabled) return
        // The Time Background container is the single source of truth for what
        // sits behind the clock: when it is explicitly TRANSPARENT, no box at
        // all may render, so the legacy glass panel is suppressed too.
        if (TimeContainer.isInactive(settings)) return

        val engine = frame.widgetEngine
        val density = frame.displayDensity
        val corner = settings.glassCornerRadius * density
        val padX = 26f * density
        val padY = 22f * density
        val union = unionRect(engine.timeBlock, engine.dateBlock) ?: return
        if (union.width() <= 0f || union.height() <= 0f) return
        val rect = RectF(
            union.left - padX,
            union.top - padY,
            union.right + padX,
            union.bottom + padY
        )

        canvas.save()
        val clip = clipPath(rect, corner)
        canvas.clipPath(clip)

        if (frame.backgroundBitmap != null && settings.glassBlurRadius > 0f) {
            blur.draw(canvas, frame, rect, settings.glassBlurRadius * density)
        }

        val panelAlpha = (0xFF * settings.glassPanelOpacity.coerceIn(0, 100) / 100)
        val panelPaint = frame.paintCache?.get("glass-panel|fill|$panelAlpha") {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(panelAlpha, 0xFF, 0xFF, 0xFF)
            }
        } ?: Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(panelAlpha, 0xFF, 0xFF, 0xFF)
        }
        canvas.drawRoundRect(rect, corner, corner, panelPaint)

        if (settings.glassBorderWidth > 0f) {
            val borderWidth = settings.glassBorderWidth * density
            val borderKey = "glass-panel|border|$borderWidth|${settings.glassBorderColor.toInt()}"
            val borderPaint = frame.paintCache?.get(borderKey) {
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = borderWidth
                    color = settings.glassBorderColor.toInt()
                }
            } ?: Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = borderWidth
                color = settings.glassBorderColor.toInt()
            }
            canvas.drawRoundRect(rect, corner, corner, borderPaint)
        }
        canvas.restore()
    }

    /**
     * Reuses the rounded-rect clip path while the panel geometry is unchanged;
     * a fresh path is built only when the block moved or a setting changed.
     */
    private fun clipPath(rect: RectF, corner: Float): Path {
        val key = "clip|${rect.left}|${rect.top}|${rect.right}|${rect.bottom}|$corner"
        val existing = clipPath
        if (existing != null && key == clipKey) return existing
        val path = Path().apply {
            addRoundRect(rect, corner, corner, Path.Direction.CW)
        }
        clipPath = path
        clipKey = key
        return path
    }

    /** Bounding box covering both widgets (or the single visible one). */
    private fun unionRect(
        time: ClockWidgetLayout.WidgetBlock?,
        date: ClockWidgetLayout.WidgetBlock?
    ): RectF? {
        val list = listOfNotNull(time, date)
        if (list.isEmpty()) return null
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        for (block in list) {
            left = min(left, block.x)
            top = min(top, block.y)
            right = max(right, block.x + block.width)
            bottom = max(bottom, block.y + block.height)
        }
        return RectF(left, top, right, bottom)
    }

    override fun release() {
        blur.release()
        clipPath = null
        clipKey = null
    }
}
