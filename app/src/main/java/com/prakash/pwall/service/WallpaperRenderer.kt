package com.prakash.pwall.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import com.prakash.pwall.data.model.BackgroundMode
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.utils.ClockTextFormatter
import com.prakash.pwall.utils.clockTypeface
import com.prakash.pwall.utils.clampBlockTopLeft
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.max

/**
 * Pure Android-View drawing of the wallpaper frame. Both the live wallpaper
 * engine and (conceptually) any future engine reuse this so the preview and
 * the real wallpaper always match.
 */
object WallpaperRenderer {

    /** Placeholder color used when no image has been selected. */
    private const val PLACEHOLDER_COLOR = 0xFF1C1C2A.toInt()

    private const val EDGE_PADDING_PX = 28f

    /** Pure scale/offset result of fitting a source into a target rectangle. */
    data class BackgroundTransform(
        val scaleX: Float,
        val scaleY: Float,
        val offsetX: Float,
        val offsetY: Float
    )

    /**
     * Maximum pan offsets (px) available in the custom background mode, based
     * on the scaled+rotated image's bounding box. [translateFraction] is a
     * normalized [-1, 1] multiplier over these bounds.
     */
    fun customPanBounds(
        bitmapWidth: Int,
        bitmapHeight: Int,
        targetW: Int,
        targetH: Int,
        zoom: Float,
        rotationDegrees: Float
    ): Pair<Float, Float> {
        val cover = maxOf(
            targetW.toFloat() / bitmapWidth,
            targetH.toFloat() / bitmapHeight
        )
        val scale = cover * zoom.coerceAtLeast(1f)
        val radians = Math.toRadians(rotationDegrees.toDouble())
        val cos = abs(Math.cos(radians)).toFloat()
        val sin = abs(Math.sin(radians)).toFloat()
        val halfW = (bitmapWidth * scale * cos + bitmapHeight * scale * sin) / 2f
        val halfH = (bitmapWidth * scale * sin + bitmapHeight * scale * cos) / 2f
        val maxPanX = max(0f, halfW - targetW / 2f)
        val maxPanY = max(0f, halfH - targetH / 2f)
        return maxPanX to maxPanY
    }

    /**
     * Builds the [Matrix] for the custom background mode: cover-scale the
     * source, then zoom, rotate around the center, and pan within the bounds
     * computed by [customPanBounds]. Mirrors the Compose preview exactly.
     */
    fun customBackgroundMatrix(
        bitmapWidth: Int,
        bitmapHeight: Int,
        targetW: Int,
        targetH: Int,
        zoom: Float,
        rotationDegrees: Float,
        translateXFraction: Float,
        translateYFraction: Float
    ): Matrix {
        val cover = maxOf(
            targetW.toFloat() / bitmapWidth,
            targetH.toFloat() / bitmapHeight
        )
        val scale = cover * zoom.coerceAtLeast(1f)
        val (maxPanX, maxPanY) = customPanBounds(
            bitmapWidth, bitmapHeight, targetW, targetH, zoom, rotationDegrees
        )
        val panX = translateXFraction.coerceIn(-1f, 1f) * maxPanX
        val panY = translateYFraction.coerceIn(-1f, 1f) * maxPanY
        val centerX = targetW / 2f + panX
        val centerY = targetH / 2f + panY

        val matrix = Matrix()
        matrix.postTranslate(-bitmapWidth / 2f, -bitmapHeight / 2f)
        matrix.postScale(scale, scale)
        matrix.postRotate(rotationDegrees)
        matrix.postTranslate(centerX, centerY)
        return matrix
    }

    /**
     * Pure-logic transform (no Android types) so it can be unit tested.
     * Centered and axis-aligned; never crops the source.
     */
    fun backgroundTransform(
        bitmapWidth: Int,
        bitmapHeight: Int,
        targetW: Int,
        targetH: Int,
        mode: BackgroundMode
    ): BackgroundTransform {
        val default = BackgroundTransform(1f, 1f, 0f, 0f)
        if (bitmapWidth <= 0 || bitmapHeight <= 0 || targetW <= 0 || targetH <= 0) return default

        return when (mode) {
            BackgroundMode.CUSTOM -> default

            BackgroundMode.STRETCH -> BackgroundTransform(
                targetW.toFloat() / bitmapWidth,
                targetH.toFloat() / bitmapHeight,
                0f,
                0f
            )

            BackgroundMode.FIT -> {
                val scale = minOf(
                    targetW.toFloat() / bitmapWidth,
                    targetH.toFloat() / bitmapHeight
                )
                BackgroundTransform(
                    scale,
                    scale,
                    (targetW - bitmapWidth * scale) / 2f,
                    (targetH - bitmapHeight * scale) / 2f
                )
            }

            BackgroundMode.FILL,
            BackgroundMode.CENTER_CROP -> {
                val scale = maxOf(
                    targetW.toFloat() / bitmapWidth,
                    targetH.toFloat() / bitmapHeight
                )
                BackgroundTransform(
                    scale,
                    scale,
                    (targetW - bitmapWidth * scale) / 2f,
                    (targetH - bitmapHeight * scale) / 2f
                )
            }
        }
    }

    /** Builds the [Matrix] for [backgroundTransform]. */
    fun backgroundMatrix(
        bitmapWidth: Int,
        bitmapHeight: Int,
        targetW: Int,
        targetH: Int,
        mode: BackgroundMode,
        zoom: Float = 1f,
        rotationDegrees: Float = 0f,
        translateXFraction: Float = 0f,
        translateYFraction: Float = 0f
    ): Matrix {
        if (mode == BackgroundMode.CUSTOM) {
            return customBackgroundMatrix(
                bitmapWidth = bitmapWidth,
                bitmapHeight = bitmapHeight,
                targetW = targetW,
                targetH = targetH,
                zoom = zoom,
                rotationDegrees = rotationDegrees,
                translateXFraction = translateXFraction,
                translateYFraction = translateYFraction
            )
        }
        val transform = backgroundTransform(bitmapWidth, bitmapHeight, targetW, targetH, mode)
        val matrix = Matrix()
        matrix.setScale(transform.scaleX, transform.scaleY)
        matrix.postTranslate(transform.offsetX, transform.offsetY)
        return matrix
    }

    /** Draws the selected image (or placeholder) into [canvas]. */
    fun drawBackground(
        canvas: Canvas,
        bitmap: android.graphics.Bitmap?,
        settings: WallpaperSettings
    ) {
        val w = canvas.width
        val h = canvas.height
        if (bitmap == null) {
            canvas.drawColor(PLACEHOLDER_COLOR)
            return
        }
        val matrix = backgroundMatrix(
            bitmapWidth = bitmap.width,
            bitmapHeight = bitmap.height,
            targetW = w,
            targetH = h,
            mode = settings.backgroundMode,
            zoom = settings.backgroundZoom,
            rotationDegrees = settings.backgroundRotationDegrees,
            translateXFraction = settings.backgroundTranslateXFraction,
            translateYFraction = settings.backgroundTranslateYFraction
        )
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(bitmap, matrix, null)
    }

    /**
     * Draws the clock + date block. Positioning matches the Compose preview:
     * presets anchor the block to an edge/corner, CUSTOM centers it on a
     * fractional point of the canvas.
     *
     * @param displayDensity scaled density used to convert sp -> px.
     */
    fun drawClock(
        canvas: Canvas,
        settings: WallpaperSettings,
        now: LocalDateTime = LocalDateTime.now(),
        displayDensity: Float
    ) {
        val timeText = ClockTextFormatter.formatTime(now, settings.timeFormat, settings.showSeconds)
        val dateText = ClockTextFormatter.formatDate(now, settings.dateFormat)

        val typeface: Typeface = clockTypeface(settings.clockFont, settings.clockBold, settings.clockItalic)

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = displayDensity * settings.clockFontSizeSp
            color = colorWithTransparency(settings.clockColor, settings.transparency)
            if (settings.shadowEnabled) {
                setShadowLayer(
                    settings.shadowBlurRadius,
                    settings.shadowOffsetX,
                    settings.shadowOffsetY,
                    settings.shadowColor.toInt()
                )
            }
        }

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = timePaint.textSize * 0.36f
            color = colorWithTransparency(settings.dateColor, settings.transparency)
            if (settings.shadowEnabled) {
                setShadowLayer(
                    settings.shadowBlurRadius,
                    settings.shadowOffsetX,
                    settings.shadowOffsetY,
                    settings.shadowColor.toInt()
                )
            }
        }

        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        val timeWidth = timePaint.measureText(timeText)
        val dateWidth = datePaint.measureText(dateText)
        val blockWidth = max(timeWidth, dateWidth)

        val timeMetrics = timePaint.fontMetrics
        val timeHeight = timeMetrics.descent - timeMetrics.ascent
        val gap = timeHeight * 0.18f
        val dateMetrics = datePaint.fontMetrics
        val dateHeight = dateMetrics.descent - dateMetrics.ascent
        val blockHeight = timeHeight + gap + dateHeight

        val (x, y) = blockTopLeft(
            canvasWidth = width,
            canvasHeight = height,
            blockWidth = blockWidth,
            blockHeight = blockHeight,
            settings = settings
        )

        timePaint.textAlign = Paint.Align.LEFT
        datePaint.textAlign = Paint.Align.LEFT

        val timeBaseline = y - timeMetrics.ascent
        canvas.drawText(timeText, x, timeBaseline, timePaint)

        val dateBaseline = timeBaseline + timeHeight + gap - dateMetrics.ascent
        canvas.drawText(dateText, x, dateBaseline, datePaint)
    }

    /**
     * Returns the block's top-left (x, y) for the given position preset /
     * fractions, matching the Compose preview. The block is always drawn
     * left-aligned at that point.
     */
    internal fun blockTopLeft(
        canvasWidth: Float,
        canvasHeight: Float,
        blockWidth: Float,
        blockHeight: Float,
        settings: WallpaperSettings
    ): Pair<Float, Float> {
        val pad = EDGE_PADDING_PX

        return when (settings.position) {
            PositionPreset.CENTER ->
                Pair(canvasWidth / 2f - blockWidth / 2f, canvasHeight / 2f - blockHeight / 2f)
            PositionPreset.TOP_LEFT -> Pair(pad, pad)
            PositionPreset.TOP_RIGHT -> Pair(canvasWidth - pad - blockWidth, pad)
            PositionPreset.BOTTOM_LEFT -> Pair(pad, canvasHeight - pad - blockHeight)
            PositionPreset.BOTTOM_RIGHT -> Pair(canvasWidth - pad - blockWidth, canvasHeight - pad - blockHeight)
            PositionPreset.BOTTOM_CENTER ->
                Pair(canvasWidth / 2f - blockWidth / 2f, canvasHeight - pad - blockHeight)
            PositionPreset.CUSTOM -> {
                val cx = settings.positionXFraction.coerceIn(0f, 1f) * canvasWidth
                val cy = settings.positionYFraction.coerceIn(0f, 1f) * canvasHeight
                clampBlockTopLeft(
                    x = cx - blockWidth / 2f,
                    y = cy - blockHeight / 2f,
                    blockWidth = blockWidth,
                    blockHeight = blockHeight,
                    maxWidth = canvasWidth,
                    maxHeight = canvasHeight
                )
            }
        }
    }

    private fun colorWithTransparency(argbLong: Long, transparencyPercent: Int): Int {
        val argb = argbLong.toInt()
        val alpha = (argb ushr 24) and 0xFF
        val newAlpha = (alpha * transparencyPercent.coerceIn(0, 100)) / 100
        return (newAlpha shl 24) or (argb and 0xFFFFFF)
    }
}
