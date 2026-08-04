package com.prakash.pwall.service.render

import android.graphics.Paint
import android.graphics.Typeface
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.service.WallpaperRenderer
import com.prakash.pwall.service.color.PremiumColors
import com.prakash.pwall.service.motion.MotionFrame
import com.prakash.pwall.service.motion.ParallaxMath
import com.prakash.pwall.utils.ClockTextFormatter
import com.prakash.pwall.utils.PaintCache
import com.prakash.pwall.utils.clampBlockTopLeft
import com.prakash.pwall.utils.clockTypeface
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min

/**
 * Shared layout math for the clock + date block. The block is positioned as a
 * single unit so the time and date land in exactly the same place the legacy
 * single-block renderer produced. [ClockLayer] and [DateLayer] both draw from
 * the same resolved [Block] (computed once per frame via [RenderFrame.clockBlock]),
 * so output is byte-for-byte identical to the legacy renderer with no per-layer
 * duplication.
 *
 * When parallax motion is present the whole block shifts slightly in the
 * opposite direction to the background (foreground depth), re-clamped so it
 * never leaves the screen.
 */
object ClockBlockLayout {

    /** Final block top-left and absolute text baselines after any motion. */
    data class Layout(
        val x: Float,
        val y: Float,
        val timeBaseline: Float,
        val dateBaseline: Float
    )

    /** Fully resolved clock block: formatted texts, paints, and positions. */
    data class Block(
        val timeText: String,
        val dateText: String,
        val timePaint: Paint,
        val datePaint: Paint,
        val glowTime: Paint?,
        val glowDate: Paint?,
        val layout: Layout,
        val blockWidth: Float,
        val blockHeight: Float
    )

    /**
     * Resolves everything both clock layers need for one frame. Called once per
     * frame (see [RenderFrame.clockBlock]). Paints are reused from [paintCache]
     * (keyed by an exact configuration signature) instead of being re-created
     * every frame; when no cache is supplied fresh paints are allocated, so the
     * legacy [WallpaperRenderer] and preview paths stay byte-identical.
     * [palette] feeds the dynamic colors when the user has enabled them.
     */
    fun resolve(
        canvasWidth: Float,
        canvasHeight: Float,
        settings: WallpaperSettings,
        displayDensity: Float,
        now: LocalDateTime,
        motion: MotionFrame?,
        palette: com.prakash.pwall.service.color.ColorPalette? = null,
        paintCache: PaintCache? = null
    ): Block {
        val timeText = ClockTextFormatter.formatTime(
            now, settings.timeFormat, settings.showSeconds
        )
        val dateText = ClockTextFormatter.formatDate(now, settings.dateFormat)

        val clockColor = PremiumColors.clockColor(settings, palette)
        val dateColor = PremiumColors.dateColor(settings, palette)
        val timePaint = timePaint(settings, displayDensity, clockColor, paintCache)
        val datePaint = datePaint(settings, displayDensity, dateColor, paintCache)
        val glowTime = glowPaint(timePaint, settings, displayDensity, clockColor, "clock", paintCache)
        val glowDate = glowPaint(datePaint, settings, displayDensity, dateColor, "date", paintCache)
        val (layout, blockWidth, blockHeight) = compute(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            timeText = timeText,
            dateText = dateText,
            timePaint = timePaint,
            datePaint = datePaint,
            settings = settings,
            motion = motion
        )
        return Block(
            timeText = timeText,
            dateText = dateText,
            timePaint = timePaint,
            datePaint = datePaint,
            glowTime = glowTime,
            glowDate = glowDate,
            layout = layout,
            blockWidth = blockWidth,
            blockHeight = blockHeight
        )
    }

    fun timePaint(
        settings: WallpaperSettings,
        displayDensity: Float,
        color: Int = settings.clockColor.toInt(),
        paintCache: PaintCache? = null
    ): Paint =
        clockPaint(
            settings = settings,
            textSize = displayDensity * settings.clockFontSizeSp,
            color = color,
            paintCache = paintCache
        )

    fun datePaint(
        settings: WallpaperSettings,
        displayDensity: Float,
        color: Int = settings.dateColor.toInt(),
        paintCache: PaintCache? = null
    ): Paint =
        clockPaint(
            settings = settings,
            textSize = displayDensity * settings.clockFontSizeSp * 0.36f,
            color = color,
            paintCache = paintCache
        )

    /**
     * The soft glow (halo) paint drawn under the crisp text when the glass
     * feature is enabled. Derived from a base text paint; cached alongside it so
     * the halo pass costs nothing after the first frame. Returns null when the
     * glow is not configured so callers keep the single-pass fast path.
     */
    fun glowPaint(
        paint: Paint,
        settings: WallpaperSettings,
        density: Float,
        color: Int = paint.color,
        purpose: String = "text",
        paintCache: PaintCache? = null
    ): Paint? {
        val radius = settings.glassGlowRadius * density
        if (!settings.glassEnabled || radius <= 0f) return null
        val key = PaintKey.glow(settings, paint.textSize, color, radius, purpose)
        if (paintCache != null) {
            return paintCache.get(key) {
                Paint(paint).apply {
                    setShadowLayer(radius, 0f, 0f, settings.glassGlowColor.toInt())
                }
            }
        }
        return Paint(paint).apply {
            setShadowLayer(radius, 0f, 0f, settings.glassGlowColor.toInt())
        }
    }

    private fun clockPaint(
        settings: WallpaperSettings,
        textSize: Float,
        color: Int,
        paintCache: PaintCache? = null
    ): Paint {
        if (paintCache != null) {
            val key = PaintKey.textKey("clock", settings, textSize, color)
            return paintCache.get(key) {
                buildClockPaint(settings, textSize, color)
            }
        }
        return buildClockPaint(settings, textSize, color)
    }

    private fun buildClockPaint(
        settings: WallpaperSettings,
        textSize: Float,
        color: Int
    ): Paint {
        val typeface: Typeface = clockTypeface(
            settings.clockFont,
            settings.clockBold,
            settings.clockItalic
        )
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = textSize
            this.color = WallpaperRenderer.colorWithTransparency(
                color.toLong(), settings.transparency
            )
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
        settings: WallpaperSettings,
        motion: MotionFrame?
    ): Triple<Layout, Float, Float> {
        val timeWidth = timePaint.measureText(timeText)
        val dateWidth = datePaint.measureText(dateText)
        val blockWidth = max(timeWidth, dateWidth)

        val timeMetrics = timePaint.fontMetrics
        val timeHeight = timeMetrics.descent - timeMetrics.ascent
        val gap = timeHeight * 0.18f
        val dateMetrics = datePaint.fontMetrics
        val dateHeight = dateMetrics.descent - dateMetrics.ascent
        val blockHeight = timeHeight + gap + dateHeight

        val (baseX, baseY) = WallpaperRenderer.blockTopLeft(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            blockWidth = blockWidth,
            blockHeight = blockHeight,
            settings = settings
        )

        val motionShift = foregroundShiftPx(motion, settings, canvasWidth, canvasHeight)
        val (x, y) = clampBlockTopLeft(
            x = baseX + motionShift.first,
            y = baseY + motionShift.second,
            blockWidth = blockWidth,
            blockHeight = blockHeight,
            maxWidth = canvasWidth,
            maxHeight = canvasHeight
        )

        val timeBaseline = y - timeMetrics.ascent
        val dateBaseline = timeBaseline + timeHeight + gap - dateMetrics.ascent
        return Triple(
            Layout(
                x = x,
                y = y,
                timeBaseline = timeBaseline,
                dateBaseline = dateBaseline
            ),
            blockWidth,
            blockHeight
        )
    }

    /**
     * Subtle foreground (clock) pixel shift derived from the current tilt,
     * opposite to the background movement for depth. Zero when no motion.
     */
    fun foregroundShiftPx(
        motion: MotionFrame?,
        settings: WallpaperSettings,
        canvasWidth: Float,
        canvasHeight: Float
    ): Pair<Float, Float> {
        if (motion == null) return 0f to 0f
        val minDim = min(canvasWidth, canvasHeight)
        val bgNormX = ParallaxMath.backgroundTilt(
            motion.tiltX, settings.parallaxSensitivity, settings.parallaxStrength
        )
        val bgNormY = ParallaxMath.backgroundTilt(
            motion.tiltY, settings.parallaxSensitivity, settings.parallaxStrength
        )
        return ParallaxMath.foregroundTilt(bgNormX) * minDim to
            ParallaxMath.foregroundTilt(bgNormY) * minDim
    }
}
