package com.prakash.pwall.service.render.layers

import android.graphics.Canvas
import android.graphics.Color
import com.prakash.pwall.service.WallpaperRenderer
import com.prakash.pwall.service.motion.ParallaxMath
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.RenderFrame
import kotlin.math.max
import kotlin.math.min

/**
 * Bottom-most layer: draws the selected image (or the placeholder color)
 * covering the canvas. Reads the current bitmap from the frame so image
 * changes are picked up without reinstalling the layer.
 *
 * When 3D parallax motion is present, the image is shifted along its real
 * overflow margins (the pan room computed from its scaled/rotated size), so the
 * background "moves" behind the foreground without ever leaving gaps.
 */
class BackgroundLayer : Layer {

    override val id: String = "background"

    override fun draw(canvas: Canvas, frame: RenderFrame) {
        val settings = frame.settings
        val bitmap = frame.backgroundBitmap
        val w = canvas.width
        val h = canvas.height
        if (bitmap == null) {
            canvas.drawColor(PLACEHOLDER_COLOR)
            return
        }
        val matrix = WallpaperRenderer.backgroundMatrix(
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

        val motion = frame.motion
        if (motion != null) {
            val (maxPanX, maxPanY) = backgroundPanBounds(
                bitmap.width, bitmap.height, w, h, settings
            )
            val userPanX = settings.backgroundTranslateXFraction.coerceIn(-1f, 1f) * maxPanX
            val userPanY = settings.backgroundTranslateYFraction.coerceIn(-1f, 1f) * maxPanY
            val (shiftX, shiftY) = ParallaxMath.backgroundShiftPx(
                tiltX = motion.tiltX,
                tiltY = motion.tiltY,
                sensitivity = settings.parallaxSensitivity,
                strength = settings.parallaxStrength,
                minScreenDim = min(w, h).toFloat(),
                maxPanX = maxPanX,
                maxPanY = maxPanY,
                userPanX = userPanX,
                userPanY = userPanY
            )
            matrix.postTranslate(shiftX, shiftY)
        }

        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(bitmap, matrix, null)
    }

    /**
     * Real horizontal/vertical overflow (pan room in px) for the current mode.
     * Uses the exact scaled size each mode produces, so parallax never moves the
     * image further than it can go without exposing edges.
     */
    internal fun backgroundPanBounds(
        bitmapWidth: Int,
        bitmapHeight: Int,
        targetW: Int,
        targetH: Int,
        settings: com.prakash.pwall.data.model.WallpaperSettings
    ): Pair<Float, Float> {
        if (settings.backgroundMode == com.prakash.pwall.data.model.BackgroundMode.CUSTOM) {
            return WallpaperRenderer.customPanBounds(
                bitmapWidth, bitmapHeight, targetW, targetH,
                settings.backgroundZoom, settings.backgroundRotationDegrees
            )
        }
        val transform = WallpaperRenderer.backgroundTransform(
            bitmapWidth, bitmapHeight, targetW, targetH, settings.backgroundMode
        )
        val scaledW = bitmapWidth * transform.scaleX
        val scaledH = bitmapHeight * transform.scaleY
        return max(0f, (scaledW - targetW) / 2f) to max(0f, (scaledH - targetH) / 2f)
    }

    private companion object {
        const val PLACEHOLDER_COLOR = 0xFF1C1C2A.toInt()
    }
}
