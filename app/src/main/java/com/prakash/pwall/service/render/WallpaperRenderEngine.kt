package com.prakash.pwall.service.render

import android.graphics.Canvas
import com.prakash.pwall.service.motion.MotionSource

/**
 * Composes layers and effects into a single wallpaper frame. Install the core
 * module for the stock layers; premium features are added as extra modules.
 * When a [MotionSource] is provided (3D parallax), the current tilt is injected
 * into the frame before drawing so layers move without knowing about sensors.
 */
class WallpaperRenderEngine(
    private val motionSource: MotionSource? = null
) {

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

    /**
     * Inserts [layer] right before the layer with [targetId] (e.g. glass panel
     * below the clock). Returns false when the target layer is not installed.
     */
    fun insertLayerBefore(targetId: String, layer: Layer): Boolean =
        layerSystem.insertBefore(targetId, layer)

    fun removeLayer(id: String): Layer? = layerSystem.remove(id)

    fun addEffect(effect: Effect) {
        effectManager.add(effect)
    }

    fun removeEffect(id: String): Effect? = effectManager.remove(id)

    fun render(canvas: Canvas, frame: RenderFrame) {
        val frameWithMotion = motionSource?.currentMotion()?.let { motion ->
            frame.copy(motion = motion)
        } ?: frame
        for (layer in layerSystem) {
            layer.draw(canvas, frameWithMotion)
            effectManager.apply(canvas, frameWithMotion, layer)
        }
    }
}
