package com.prakash.pwall.service.render

import android.graphics.Canvas

/**
 * Ordered z-stack of [Layer]s. Layers draw in registration order so later
 * layers render on top of earlier ones.
 */
class LayerSystem : Iterable<Layer> {

    private val layers = mutableListOf<Layer>()

    fun add(layer: Layer): Boolean = layers.add(layer)

    /**
     * Inserts [layer] immediately before the layer with [targetId]. Returns
     * false when the target does not exist, so premium modules can safely slot
     * layers (e.g. the glass panel) beneath a core layer.
     */
    fun insertBefore(targetId: String, layer: Layer): Boolean {
        val index = layers.indexOfFirst { it.id == targetId }
        if (index < 0) return false
        layers.add(index, layer)
        return true
    }

    fun remove(id: String): Layer? {
        val index = layers.indexOfFirst { it.id == id }
        return if (index >= 0) layers.removeAt(index) else null
    }

    fun get(id: String): Layer? = layers.firstOrNull { it.id == id }

    val ids: List<String> get() = layers.map { it.id }

    val size: Int get() = layers.size

    fun draw(canvas: Canvas, frame: RenderFrame) {
        for (layer in layers) {
            layer.draw(canvas, frame)
        }
    }

    override fun iterator(): Iterator<Layer> = layers.iterator()
}
