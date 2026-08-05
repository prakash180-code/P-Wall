package com.prakash.pwall.service.render

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.prakash.pwall.data.model.ClockFont
import com.prakash.pwall.data.model.ClockLayout
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.TimeLayout
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.model.WidgetStyle
import com.prakash.pwall.service.WallpaperRenderer
import com.prakash.pwall.service.color.ColorPalette
import com.prakash.pwall.service.color.PremiumColors
import com.prakash.pwall.service.motion.MotionFrame
import com.prakash.pwall.utils.ClockTextFormatter
import com.prakash.pwall.utils.PaintCache
import com.prakash.pwall.utils.clampBlockTopLeft
import com.prakash.pwall.utils.clockTypeface
import java.time.LocalDateTime
import kotlin.math.max

/**
 * Clock & Date Engine: resolves the time and date as two independent widgets
 * that can be styled, laid out and positioned separately.
 *
 * When the settings are a "classic combination" ([WidgetPresets.isClassicCombination])
 * the result is built from the original combined clock block
 * ([ClockBlockLayout.resolve]) so output stays byte-for-byte identical to the
 * v1.0.1 renderer (including legacy vertical layouts). Any customization (new
 * time/date layout, a non-classic style, an unlinked date, ...) switches to the
 * independent widget path; when the date stays linked it is anchored directly
 * below the time with the classic gap, so the composition keeps the familiar
 * single-block feel.
 */
object ClockWidgetLayout {

    enum class Align { LEFT, CENTER, RIGHT }

    /** One absolutely-positioned text unit inside a widget. */
    data class TextChunk(
        val text: String,
        val x: Float,
        val baseline: Float,
        val align: Align = Align.LEFT
    )

    /** Fully resolved widget: draw units, paints and its position box. */
    data class WidgetBlock(
        val chunks: List<TextChunk>,
        val fillPaint: Paint,
        val strokePaint: Paint?,
        val glowPaint: Paint?,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val centerX: Float,
        val centerY: Float,
        val chipRect: RectF? = null,
        val chipCorner: Float = 0f,
        val chipAlpha: Float = 0f
    )

    /** One frame of the engine. Blocks are null when their widget is hidden. */
    data class Result(
        val timeBlock: WidgetBlock?,
        val dateBlock: WidgetBlock?,
        val classic: Boolean
    )

    private data class TextInfo(
        val width: Float,
        val height: Float,
        val textHeight: Float,
        val lineGap: Float,
        val split: Boolean,
        val splitWidths: List<Float>,
        val lineCount: Int
    )

    fun resolve(
        canvasWidth: Float,
        canvasHeight: Float,
        settings: WallpaperSettings,
        displayDensity: Float,
        now: LocalDateTime,
        motion: MotionFrame?,
        palette: ColorPalette?,
        paintCache: PaintCache?
    ): Result {
        if (WidgetPresets.isClassicCombination(settings)) {
            return resolveClassic(
                canvasWidth, canvasHeight, settings, displayDensity, now, motion, palette, paintCache
            )
        }
        return resolveWidgets(
            canvasWidth, canvasHeight, settings, displayDensity, now, motion, palette, paintCache
        )
    }

    // --- Classic path (byte-identical to the legacy combined block) ----------

    private fun resolveClassic(
        canvasWidth: Float,
        canvasHeight: Float,
        settings: WallpaperSettings,
        displayDensity: Float,
        now: LocalDateTime,
        motion: MotionFrame?,
        palette: ColorPalette?,
        paintCache: PaintCache?
    ): Result {
        val legacyLayout = settings.timeLayout.legacyLayout ?: ClockLayout.HORIZONTAL
        val block = ClockBlockLayout.resolve(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            settings = settings.copy(clockLayout = legacyLayout),
            displayDensity = displayDensity,
            now = now,
            motion = motion,
            palette = palette,
            paintCache = paintCache
        )
        val centered = block.layout.timeCenteredX
        val timeChunks = block.timeLines.mapIndexed { index, line ->
            val baseline = block.layout.timeLineBaselines.getOrElse(index) {
                block.layout.timeLineBaselines.last()
            }
            if (centered != null) {
                TextChunk(line, centered, baseline, Align.CENTER)
            } else {
                TextChunk(line, block.layout.x, baseline, Align.LEFT)
            }
        }
        val centerX = block.layout.x + block.blockWidth / 2f
        val centerY = block.layout.y + block.blockHeight / 2f
        val timeBlock = WidgetBlock(
            chunks = timeChunks,
            fillPaint = block.timePaint,
            strokePaint = null,
            glowPaint = block.glowTime,
            x = block.layout.x,
            y = block.layout.y,
            width = block.blockWidth,
            height = block.blockHeight,
            centerX = centerX,
            centerY = centerY
        )
        val dateBlock = WidgetBlock(
            chunks = listOf(TextChunk(block.dateText, block.layout.x, block.layout.dateBaseline)),
            fillPaint = block.datePaint,
            strokePaint = null,
            glowPaint = block.glowDate,
            x = block.layout.x,
            y = block.layout.y,
            width = block.blockWidth,
            height = block.blockHeight,
            centerX = centerX,
            centerY = centerY
        )
        return Result(timeBlock, dateBlock, classic = true)
    }

    // --- Independent widget path ----------------------------------------------

    private fun resolveWidgets(
        canvasWidth: Float,
        canvasHeight: Float,
        settings: WallpaperSettings,
        displayDensity: Float,
        now: LocalDateTime,
        motion: MotionFrame?,
        palette: ColorPalette?,
        paintCache: PaintCache?
    ): Result {
        val timeLines = ClockTextFormatter.formatTimeLines(
            now, settings.timeFormat, settings.showSeconds, settings.timeLayout
        )
        val timePaint = timeFillPaint(settings, displayDensity, palette, paintCache)
        val timeStroke = timeStrokePaint(settings, displayDensity, palette, paintCache)
        val timeGlow = timeGlowPaint(settings, displayDensity, palette, paintCache)
        val timeInfo = textInfo(timeLines, timePaint, settings.timeLayout == TimeLayout.SPLIT)

        val dateLines = ClockTextFormatter.formatDateLines(
            now, settings.dateLayout, settings.dateFormat
        )
        val datePaint = dateFillPaint(settings, displayDensity, palette, paintCache)
        val dateStroke = dateStrokePaint(settings, displayDensity, palette, paintCache)
        val dateGlow = dateGlowPaint(settings, displayDensity, palette, paintCache)
        val dateInfo = textInfo(dateLines, datePaint, split = false)

        val linked = settings.dateLinkedToTime && settings.clockVisible && settings.dateVisible
        val gap = if (linked) timeInfo.textHeight * 0.18f * settings.dateGapMultiplier else 0f

        val shift = ClockBlockLayout.foregroundShiftPx(
            motion, settings, canvasWidth, canvasHeight
        )

        val (timePos, datePos) = if (linked) {
            val combinedWidth = max(timeInfo.width, dateInfo.width)
            val combinedHeight = timeInfo.height + gap + dateInfo.height
            val (baseX, baseY) = WallpaperRenderer.blockTopLeft(
                canvasWidth, canvasHeight, combinedWidth, combinedHeight, settings
            )
            val (x, y) = clampBlockTopLeft(
                x = baseX + shift.first,
                y = baseY + shift.second,
                blockWidth = combinedWidth,
                blockHeight = combinedHeight,
                maxWidth = canvasWidth,
                maxHeight = canvasHeight
            )
            val dateY = y + timeInfo.height + gap
            val (dx, dy) = clampBlockTopLeft(
                x = x,
                y = dateY,
                blockWidth = dateInfo.width,
                blockHeight = dateInfo.height,
                maxWidth = canvasWidth,
                maxHeight = canvasHeight
            )
            (x to y) to (dx to dy)
        } else {
            val time = positionBlock(
                canvasWidth, canvasHeight, timeInfo.width, timeInfo.height,
                settings.position, settings.positionXFraction, settings.positionYFraction,
                shift
            )
            val date = positionBlock(
                canvasWidth, canvasHeight, dateInfo.width, dateInfo.height,
                settings.datePosition, settings.datePositionXFraction,
                settings.datePositionYFraction, shift
            )
            time to date
        }
        val timeX = timePos.first
        val timeY = timePos.second
        val dateX = datePos.first
        val dateY = datePos.second

        val timeBlock = if (settings.clockVisible) {
            val recipe = WidgetStyleRecipe.recipe(settings.timeStyle)
            val chunks = buildChunks(
                timeLines, timePaint, timeInfo, timeX, timeY, timeAlign(settings)
            )
            val chip = chipRect(recipe, timeX, timeY, timeInfo.width, timeInfo.height, timePaint.textSize)
            WidgetBlock(
                chunks = chunks,
                fillPaint = timePaint,
                strokePaint = timeStroke,
                glowPaint = timeGlow,
                x = timeX,
                y = timeY,
                width = timeInfo.width,
                height = timeInfo.height,
                centerX = timeX + timeInfo.width / 2f,
                centerY = timeY + timeInfo.height / 2f,
                chipRect = chip?.first,
                chipCorner = chip?.second ?: 0f,
                chipAlpha = recipe.chipAlpha
            )
        } else {
            null
        }

        val dateBlock = if (settings.dateVisible) {
            val recipe = WidgetStyleRecipe.recipe(settings.dateStyle)
            val align = if (linked) Align.LEFT else Align.CENTER
            val chunks = buildChunks(dateLines, datePaint, dateInfo, dateX, dateY, align)
            val chip = chipRect(recipe, dateX, dateY, dateInfo.width, dateInfo.height, datePaint.textSize)
            WidgetBlock(
                chunks = chunks,
                fillPaint = datePaint,
                strokePaint = dateStroke,
                glowPaint = dateGlow,
                x = dateX,
                y = dateY,
                width = dateInfo.width,
                height = dateInfo.height,
                centerX = dateX + dateInfo.width / 2f,
                centerY = dateY + dateInfo.height / 2f,
                chipRect = chip?.first,
                chipCorner = chip?.second ?: 0f,
                chipAlpha = recipe.chipAlpha
            )
        } else {
            null
        }

        return Result(timeBlock, dateBlock, classic = false)
    }

    private fun positionBlock(
        canvasWidth: Float,
        canvasHeight: Float,
        blockWidth: Float,
        blockHeight: Float,
        position: PositionPreset,
        xFraction: Float,
        yFraction: Float,
        shift: Pair<Float, Float>
    ): Pair<Float, Float> {
        val (baseX, baseY) = WallpaperRenderer.blockTopLeft(
            canvasWidth, canvasHeight, blockWidth, blockHeight, position, xFraction, yFraction
        )
        return clampBlockTopLeft(
            x = baseX + shift.first,
            y = baseY + shift.second,
            blockWidth = blockWidth,
            blockHeight = blockHeight,
            maxWidth = canvasWidth,
            maxHeight = canvasHeight
        )
    }

    private fun timeAlign(settings: WallpaperSettings): Align = when (settings.timeLayout) {
        TimeLayout.CENTERED -> Align.CENTER
        TimeLayout.RIGHT_ALIGNED -> Align.RIGHT
        else -> Align.LEFT
    }

    private fun textInfo(lines: List<String>, paint: Paint, split: Boolean): TextInfo {
        val metrics = paint.fontMetrics
        val textHeight = metrics.descent - metrics.ascent
        val lineGap = textHeight * 0.18f
        if (split) {
            val widths = lines.map { paint.measureText(it) }
            return TextInfo(
                width = widths.sum(),
                height = textHeight,
                textHeight = textHeight,
                lineGap = lineGap,
                split = true,
                splitWidths = widths,
                lineCount = lines.size
            )
        }
        val width = lines.maxOfOrNull { paint.measureText(it) } ?: 0f
        val height = lines.size * textHeight + (lines.size - 1).coerceAtLeast(0) * lineGap
        return TextInfo(
            width = width,
            height = height,
            textHeight = textHeight,
            lineGap = lineGap,
            split = false,
            splitWidths = emptyList(),
            lineCount = lines.size
        )
    }

    private fun buildChunks(
        lines: List<String>,
        paint: Paint,
        info: TextInfo,
        x: Float,
        y: Float,
        align: Align
    ): List<TextChunk> {
        val baseline0 = y - paint.fontMetrics.ascent
        if (info.split) {
            val startX = x + (info.width - info.splitWidths.sum()) / 2f
            var cursor = startX
            return lines.mapIndexed { index, unit ->
                val chunkX = cursor
                cursor += info.splitWidths[index]
                TextChunk(unit, chunkX, baseline0, Align.LEFT)
            }
        }
        return lines.mapIndexed { index, line ->
            val baseline = baseline0 + index * (info.textHeight + info.lineGap)
            val chunkX = when (align) {
                Align.LEFT -> x
                Align.CENTER -> x + info.width / 2f
                Align.RIGHT -> x + info.width
            }
            TextChunk(line, chunkX, baseline, align)
        }
    }

    private fun chipRect(
        recipe: StyleRecipe,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        textSizePx: Float
    ): Pair<RectF, Float>? {
        if (!recipe.chipEnabled) return null
        val pad = textSizePx * recipe.chipPaddingFactor
        val rect = RectF(
            x - pad,
            y - pad,
            x + width + pad,
            y + height + pad
        )
        return rect to pad
    }

    // --- Widget paints ---------------------------------------------------------

    private fun timeFillPaint(
        settings: WallpaperSettings,
        displayDensity: Float,
        palette: ColorPalette?,
        paintCache: PaintCache?
    ): Paint = widgetPaint(
        settings = settings,
        purpose = "time",
        recipe = WidgetStyleRecipe.recipe(settings.timeStyle),
        family = settings.clockFont,
        bold = settings.clockBold,
        italic = settings.clockItalic,
        textSizePx = displayDensity * settings.clockFontSizeSp,
        color = PremiumColors.clockColor(settings, palette),
        shadowEnabled = settings.shadowEnabled,
        shadowBlur = settings.shadowBlurRadius,
        shadowOffX = settings.shadowOffsetX,
        shadowOffY = settings.shadowOffsetY,
        shadowColor = settings.shadowColor,
        stroke = false,
        paintCache = paintCache
    )

    private fun dateFillPaint(
        settings: WallpaperSettings,
        displayDensity: Float,
        palette: ColorPalette?,
        paintCache: PaintCache?
    ): Paint = widgetPaint(
        settings = settings,
        purpose = "date",
        recipe = WidgetStyleRecipe.recipe(settings.dateStyle),
        family = settings.dateFont,
        bold = settings.dateBold,
        italic = settings.dateItalic,
        textSizePx = dateTextSizePx(settings, displayDensity),
        color = PremiumColors.dateColor(settings, palette),
        shadowEnabled = settings.dateShadowEnabled,
        shadowBlur = settings.dateShadowBlurRadius,
        shadowOffX = settings.dateShadowOffsetX,
        shadowOffY = settings.dateShadowOffsetY,
        shadowColor = settings.dateShadowColor,
        stroke = false,
        paintCache = paintCache
    )

    private fun timeStrokePaint(
        settings: WallpaperSettings,
        displayDensity: Float,
        palette: ColorPalette?,
        paintCache: PaintCache?
    ): Paint? {
        val recipe = WidgetStyleRecipe.recipe(settings.timeStyle)
        if (!recipe.strokeEnabled) return null
        return widgetPaint(
            settings = settings,
            purpose = "time",
            recipe = recipe,
            family = settings.clockFont,
            bold = settings.clockBold,
            italic = settings.clockItalic,
            textSizePx = displayDensity * settings.clockFontSizeSp,
            color = PremiumColors.clockColor(settings, palette),
            shadowEnabled = false,
            shadowBlur = 0f,
            shadowOffX = 0f,
            shadowOffY = 0f,
            shadowColor = 0L,
            stroke = true,
            paintCache = paintCache
        )
    }

    private fun dateStrokePaint(
        settings: WallpaperSettings,
        displayDensity: Float,
        palette: ColorPalette?,
        paintCache: PaintCache?
    ): Paint? {
        val recipe = WidgetStyleRecipe.recipe(settings.dateStyle)
        if (!recipe.strokeEnabled) return null
        return widgetPaint(
            settings = settings,
            purpose = "date",
            recipe = recipe,
            family = settings.dateFont,
            bold = settings.dateBold,
            italic = settings.dateItalic,
            textSizePx = dateTextSizePx(settings, displayDensity),
            color = PremiumColors.dateColor(settings, palette),
            shadowEnabled = false,
            shadowBlur = 0f,
            shadowOffX = 0f,
            shadowOffY = 0f,
            shadowColor = 0L,
            stroke = true,
            paintCache = paintCache
        )
    }

    private fun timeGlowPaint(
        settings: WallpaperSettings,
        displayDensity: Float,
        palette: ColorPalette?,
        paintCache: PaintCache?
    ): Paint? {
        val color = PremiumColors.clockColor(settings, palette)
        val textSizePx = displayDensity * settings.clockFontSizeSp
        val recipeGlow = recipeGlow(
            WidgetStyleRecipe.recipe(settings.timeStyle), settings, textSizePx, color,
            "time", paintCache
        )
        return recipeGlow ?: ClockBlockLayout.glowPaint(
            Paint(Paint.ANTI_ALIAS_FLAG), settings, displayDensity, color, "clock", paintCache
        )
    }

    private fun dateGlowPaint(
        settings: WallpaperSettings,
        displayDensity: Float,
        palette: ColorPalette?,
        paintCache: PaintCache?
    ): Paint? {
        val color = PremiumColors.dateColor(settings, palette)
        val textSizePx = dateTextSizePx(settings, displayDensity)
        val recipeGlow = recipeGlow(
            WidgetStyleRecipe.recipe(settings.dateStyle), settings, textSizePx, color,
            "date", paintCache
        )
        return recipeGlow ?: ClockBlockLayout.glowPaint(
            Paint(Paint.ANTI_ALIAS_FLAG), settings, displayDensity, color, "date", paintCache
        )
    }

    private fun recipeGlow(
        recipe: StyleRecipe,
        settings: WallpaperSettings,
        textSizePx: Float,
        color: Int,
        purpose: String,
        paintCache: PaintCache?
    ): Paint? {
        if (recipe.glowRadiusFactor <= 0f) return null
        val radius = textSizePx * recipe.glowRadiusFactor
        val glowColor = WallpaperRenderer.colorWithTransparency(
            (recipe.glowColor ?: color.toLong()).toLong(), settings.transparency
        )
        val key = PaintKey.widgetGlow(purpose, color, textSizePx, radius, glowColor)
        fun build(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            setShadowLayer(radius, 0f, 0f, glowColor)
        }
        return if (paintCache != null) paintCache.get(key) { build() } else build()
    }

    private fun dateTextSizePx(settings: WallpaperSettings, displayDensity: Float): Float =
        if (settings.dateFontSizeSp > 0f) {
            displayDensity * settings.dateFontSizeSp
        } else {
            displayDensity * settings.clockFontSizeSp * 0.36f
        }

    private fun widgetPaint(
        settings: WallpaperSettings,
        purpose: String,
        recipe: StyleRecipe,
        family: ClockFont,
        bold: Boolean,
        italic: Boolean,
        textSizePx: Float,
        color: Int,
        shadowEnabled: Boolean,
        shadowBlur: Float,
        shadowOffX: Float,
        shadowOffY: Float,
        shadowColor: Long,
        stroke: Boolean,
        paintCache: PaintCache?
    ): Paint {
        val familyName = recipe.family?.name ?: family.name
        val effectiveBold = recipe.bold ?: bold
        val effectiveItalic = recipe.italic ?: italic
        val strokeWidth = if (stroke && recipe.strokeEnabled) {
            textSizePx * recipe.strokeWidthFactor
        } else {
            0f
        }
        val key = PaintKey.widget(
            purpose = purpose,
            familyName = familyName,
            bold = effectiveBold,
            italic = effectiveItalic,
            textSizePx = textSizePx,
            color = color,
            transparency = settings.transparency,
            shadowEnabled = shadowEnabled,
            shadowBlurRadius = shadowBlur,
            shadowOffsetX = shadowOffX,
            shadowOffsetY = shadowOffY,
            shadowColor = shadowColor.toInt(),
            letterSpacing = recipe.letterSpacing,
            strokeEnabled = stroke,
            strokeWidthPx = strokeWidth
        )
        fun build(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            val typeface: Typeface = clockTypeface(
                ClockFont.entries.firstOrNull { it.name == familyName } ?: ClockFont.DEFAULT,
                effectiveBold,
                effectiveItalic
            )
            this.typeface = typeface
            this.textSize = textSizePx
            this.color = WallpaperRenderer.colorWithTransparency(color.toLong(), settings.transparency)
            this.letterSpacing = recipe.letterSpacing
            if (stroke) {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth.coerceAtLeast(1f)
                strokeJoin = Paint.Join.ROUND
            }
            if (shadowEnabled) {
                setShadowLayer(
                    shadowBlur,
                    shadowOffX,
                    shadowOffY,
                    shadowColor.toInt()
                )
            }
        }
        return if (paintCache != null) paintCache.get(key) { build() } else build()
    }
}
