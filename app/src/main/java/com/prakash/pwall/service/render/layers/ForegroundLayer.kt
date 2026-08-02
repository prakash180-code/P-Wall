package com.prakash.pwall.service.render.layers

import android.graphics.Canvas
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.RenderFrame

/**
 * Draws the AI-depth foreground subject (people/pets/objects) on top of the
 * clock and date, so the clock appears *behind* the subject. The subject is a
 * same-size bitmap produced by the depth pipeline and is drawn with the exact
 * same [RenderFrame.backgroundMatrix] as the background image, keeping it
 * pixel-aligned even while 3D parallax moves the scene.
 *
 * When no foreground is available (feature off, unsupported, or still
 * segmenting) this layer draws nothing, so the frame is identical to the
 * plain renderer.
 */
class ForegroundLayer : Layer {

    override val id: String = "foreground"

    override fun draw(canvas: Canvas, frame: RenderFrame) {
        val subject = frame.foregroundBitmap ?: return
        if (frame.backgroundBitmap == null) return
        canvas.drawBitmap(subject, frame.backgroundMatrix, null)
    }
}
