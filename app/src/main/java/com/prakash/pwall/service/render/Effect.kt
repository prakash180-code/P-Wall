package com.prakash.pwall.service.render

import android.graphics.Canvas

/**
 * Post-processing applied to a [Layer] after it draws. Effects are the modular
 * seam for future premium features (particles, vignette, weather, ...) and do
 * nothing by default.
 */
interface Effect {
    val id: String
    fun apply(canvas: Canvas, frame: RenderFrame, layer: Layer)
}
