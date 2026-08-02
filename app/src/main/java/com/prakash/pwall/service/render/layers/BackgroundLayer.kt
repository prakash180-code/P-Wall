package com.prakash.pwall.service.render.layers

import android.graphics.Canvas
import android.graphics.Color
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.RenderFrame

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
        val bitmap = frame.backgroundBitmap
        if (bitmap == null) {
            canvas.drawColor(PLACEHOLDER_COLOR)
            return
        }
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(bitmap, frame.backgroundMatrix, null)
    }

    private companion object {
        const val PLACEHOLDER_COLOR = 0xFF1C1C2A.toInt()
    }
}
