package com.prakash.pwall.service.render

import android.graphics.Canvas

/**
 * One drawable unit of the wallpaper. Layers are drawn in registration order,
 * so later layers render on top of earlier ones.
 */
interface Layer {
    val id: String
    fun draw(canvas: Canvas, frame: RenderFrame)
}

/**
 * Layers holding large native resources (scratch bitmaps) implement this so the
 * engine can ask them to free memory when the wallpaper is hidden or destroyed.
 */
interface Releasable {
    fun release()
}
