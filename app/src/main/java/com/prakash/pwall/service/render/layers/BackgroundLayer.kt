package com.prakash.pwall.service.render.layers

import android.graphics.Canvas
import android.graphics.Color
import com.prakash.pwall.service.WallpaperRenderer
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.RenderFrame

/**
 * Bottom-most layer: draws the selected image (or the placeholder color)
 * covering the canvas. Reads the current bitmap from the frame so image
 * changes are picked up without reinstalling the layer.
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
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(bitmap, matrix, null)
    }

    private companion object {
        const val PLACEHOLDER_COLOR = 0xFF1C1C2A.toInt()
    }
}
