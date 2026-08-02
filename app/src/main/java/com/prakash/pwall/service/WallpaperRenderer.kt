package com.prakash.pwall.service

import android.graphics.Canvas
import android.graphics.Matrix
import com.prakash.pwall.data.model.BackgroundMode
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.service.render.ClockBlockLayout
import com.prakash.pwall.service.render.RenderFrame
import com.prakash.pwall.service.render.layers.BackgroundLayer
import com.prakash.pwall.service.render.layers.ClockLayer
import com.prakash.pwall.service.render.layers.DateLayer
import com.prakash.pwall.utils.clampBlockTopLeft
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.max

/**
 * Pure Android-View drawing of the wallpaper frame. Holds the shared math used
 * by both the live wallpaper engine and the Compose preview so they always
 * match, and delegates the actual drawing to the modular render engine's
 * layers. Both the live wallpaper engine and (conceptually) any future engine
 * reuse this so the preview and the real wallpaper always match.
 */
object WallpaperRenderer {

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
        BackgroundLayer().draw(
            canvas,
            RenderFrame(settings = settings, backgroundBitmap = bitmap)
        )
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
        val frame = RenderFrame(
            settings = settings,
            now = now,
            displayDensity = displayDensity
        )
        ClockLayer().draw(canvas, frame)
        DateLayer().draw(canvas, frame)
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

    internal fun colorWithTransparency(argbLong: Long, transparencyPercent: Int): Int {
        val argb = argbLong.toInt()
        val alpha = (argb ushr 24) and 0xFF
        val newAlpha = (alpha * transparencyPercent.coerceIn(0, 100)) / 100
        return (newAlpha shl 24) or (argb and 0xFFFFFF)
    }
}
