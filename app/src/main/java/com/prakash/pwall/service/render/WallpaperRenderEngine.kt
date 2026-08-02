package com.prakash.pwall.service.render

import android.graphics.Canvas

/**
 * Composes layers and effects into a single wallpaper frame. Install the core
 * module for the stock layers; premium features are added as extra modules.
 */
class WallpaperRenderEngine {

    private val layerSystem = LayerSystem()
    private val effectManager = EffectManager()
    private val moduleSystem = ModuleSystem()

    val layerIds: List<String> get() = layerSystem.ids

    val effectIds: List<String> get() = effectManager.ids

    fun installModule(module: Module) {
        if (moduleSystem.add(module)) {
            module.register(this)
        }
    }

    fun addLayer(layer: Layer) {
        layerSystem.add(layer)
    }

    fun removeLayer(id: String): Layer? = layerSystem.remove(id)

    fun addEffect(effect: Effect) {
        effectManager.add(effect)
    }

    fun removeEffect(id: String): Effect? = effectManager.remove(id)

    fun render(canvas: Canvas, frame: RenderFrame) {
        for (layer in layerSystem) {
            layer.draw(canvas, frame)
            effectManager.apply(canvas, frame, layer)
        }
    }
}
