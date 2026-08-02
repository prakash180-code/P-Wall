package com.prakash.pwall.service.render

import android.graphics.Paint
import android.graphics.Typeface
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.service.WallpaperRenderer
import com.prakash.pwall.utils.clockTypeface
import kotlin.math.max

/**
 * Shared layout math for the clock + date block. Both [ClockLayer] and
 * [DateLayer] call [compute] with identical inputs, so the time and date land
 * in exactly the same place the legacy single-block renderer produced. The
 * block is positioned as one unit and the two layers each draw their own text
 * at the resulting baselines.
 */
object ClockBlockLayout {

    data class Layout(
        val x: Float,
        val timeBaseline: Float,
        val dateBaseline: Float
    )

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

    fun compute(
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
