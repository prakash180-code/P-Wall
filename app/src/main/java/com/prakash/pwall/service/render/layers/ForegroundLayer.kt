package com.prakash.pwall.service.render.layers

import android.graphics.Canvas
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.RenderFrame

/**
 * Reserved layer for future foreground compositing. Currently a no-op so the
 * default render is byte-for-byte identical to the legacy renderer.
 */
class ForegroundLayer : Layer {

    override val id: String = "foreground"

    override fun draw(canvas: Canvas, frame: RenderFrame) {
        // Reserved for future premium features.
    }
}
