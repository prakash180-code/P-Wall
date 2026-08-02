package com.prakash.pwall.service.render

import android.graphics.Paint
import android.graphics.Typeface
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.service.WallpaperRenderer
import com.prakash.pwall.utils.ClockTextFormatter
import com.prakash.pwall.utils.clockTypeface
import java.time.LocalDateTime
import kotlin.math.max

/**
 * Shared layout math for the clock + date block. The block is positioned as a
 * single unit so the time and date land in exactly the same place the legacy
 * single-block renderer produced. [ClockLayer] and [DateLayer] both draw from
 * the same resolved [Block] (computed once per frame via [RenderFrame.clockBlock]),
 * so output is byte-for-byte identical to the legacy renderer with no per-layer
 * duplication.
 */
object ClockBlockLayout {

    /** Positions within the canvas for the two text baselines. */
    data class Layout(
        val x: Float,
        val timeBaseline: Float,
        val dateBaseline: Float
    )

    /** Fully resolved clock block: formatted texts, paints, and positions. */
    data class Block(
        val timeText: String,
        val dateText: String,
        val timePaint: Paint,
        val datePaint: Paint,
        val layout: Layout
    )

    /**
     * Resolves everything both clock layers need for one frame. Called once per
     * frame (see [RenderFrame.clockBlock]) so paint allocations match the legacy
     * renderer's two Paints per frame.
     */
    fun resolve(
        canvasWidth: Float,
        canvasHeight: Float,
        settings: WallpaperSettings,
        displayDensity: Float,
        now: LocalDateTime
    ): Block {
        val timeText = ClockTextFormatter.formatTime(
            now, settings.timeFormat, settings.showSeconds
        )
        val dateText = ClockTextFormatter.formatDate(now, settings.dateFormat)

        val timePaint = timePaint(settings, displayDensity)
        val datePaint = datePaint(settings, displayDensity)
        val layout = compute(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            timeText = timeText,
            dateText = dateText,
            timePaint = timePaint,
            datePaint = datePaint,
            settings = settings
        )
        return Block(
            timeText = timeText,
            dateText = dateText,
            timePaint = timePaint,
            datePaint = datePaint,
            layout = layout
        )
    }

    fun timePaint(settings: WallpaperSettings, displayDensity: Float): Paint =
        clockPaint(
            settings = settings,
            textSize = displayDensity * settings.clockFontSizeSp,
            color = settings.clockColor
        )

    fun datePaint(settings: WallpaperSettings, displayDensity: Float): Paint =
        clockPaint(
            settings = settings,
            textSize = displayDensity * settings.clockFontSizeSp * 0.36f,
            color = settings.dateColor
        )

    private fun clockPaint(
        settings: WallpaperSettings,
        textSize: Float,
        color: Long
    ): Paint {
        val typeface: Typeface = clockTypeface(
            settings.clockFont,
            settings.clockBold,
            settings.clockItalic
        )
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = textSize
            this.color = WallpaperRenderer.colorWithTransparency(color, settings.transparency)
            if (settings.shadowEnabled) {
                setShadowLayer(
                    settings.shadowBlurRadius,
                    settings.shadowOffsetX,
                    settings.shadowOffsetY,
                    settings.shadowColor.toInt()
                )
            }
        }
    }

    private fun compute(
        canvasWidth: Float,
        canvasHeight: Float,
        timeText: String,
        dateText: String,
        timePaint: Paint,
        datePaint: Paint,
        settings: WallpaperSettings
    ): Layout {
        val timeWidth = timePaint.measureText(timeText)
        val dateWidth = datePaint.measureText(dateText)
        val blockWidth = max(timeWidth, dateWidth)

        val timeMetrics = timePaint.fontMetrics
        val timeHeight = timeMetrics.descent - timeMetrics.ascent
        val gap = timeHeight * 0.18f
        val dateMetrics = datePaint.fontMetrics
        val dateHeight = dateMetrics.descent - dateMetrics.ascent
        val blockHeight = timeHeight + gap + dateHeight

        val (x, y) = WallpaperRenderer.blockTopLeft(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            blockWidth = blockWidth,
            blockHeight = blockHeight,
            settings = settings
        )

        val timeBaseline = y - timeMetrics.ascent
        val dateBaseline = timeBaseline + timeHeight + gap - dateMetrics.ascent
        return Layout(x = x, timeBaseline = timeBaseline, dateBaseline = dateBaseline)
    }
}
