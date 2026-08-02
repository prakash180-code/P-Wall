package com.prakash.pwall.service.render.layers

import android.graphics.Canvas
import com.prakash.pwall.service.render.Layer
import com.prakash.pwall.service.render.RenderFrame

/**
 * Reserved top-most layer for future overlay effects (weather, battery, ...).
 * Currently a no-op so the default render is byte-for-byte identical to the
 * legacy renderer.
 */
class OverlayLayer : Layer {

    override val id: String = "overlay"

    override fun draw(canvas: Canvas, frame: RenderFrame) {
        // Reserved for future premium features.
    }
}
