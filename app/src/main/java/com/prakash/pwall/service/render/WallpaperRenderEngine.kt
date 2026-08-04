package com.prakash.pwall.service.render

import android.graphics.Canvas
import com.prakash.pwall.service.motion.MotionSource
import com.prakash.pwall.utils.PWallLog
import com.prakash.pwall.utils.PaintCache

/**
 * Composes layers and effects into a single wallpaper frame. Install the core
 * module for the stock layers; premium features are added as extra modules.
 * When a [MotionSource] is provided (3D parallax), the current tilt is injected
 * into the frame before drawing so layers move without knowing about sensors.
 *
 * Production hardening:
 * - Each layer draw is guarded by a [RenderGuard] circuit breaker: a layer that
 *   throws repeatedly is dropped (and logged) so one bad layer cannot crash the
 *   render thread.
 * - A [PaintCache] is owned per engine so per-frame paint reuse never races
 *   across the preview (main thread) and the wallpaper (render thread).
 * - [release] frees native resources held by [Releasable] layers.
 */
class WallpaperRenderEngine(
    private val motionSource: MotionSource? = null
) {

    /** Per-engine paint reuse; pass into [RenderFrame.paintCache] for this engine. */
    val paintCache: PaintCache = PaintCache()

    private val layerSystem = LayerSystem()
    private val effectManager = EffectManager()
    private val moduleSystem = ModuleSystem()
    private val guard = RenderGuard()

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
        val frameWithMotion = if (frame.motion != null) {
            frame
        } else {
            motionSource?.currentMotion()?.let { motion ->
                frame.copy(motion = motion)
            } ?: frame
        }
        val toDrop = mutableListOf<String>()
        for (layer in layerSystem.toList()) {
            try {
                layer.draw(canvas, frameWithMotion)
                effectManager.apply(canvas, frameWithMotion, layer)
                guard.reset(layer.id)
            } catch (t: Throwable) {
                PWallLog.e("Layer '${layer.id}' failed", t)
                if (guard.onFailure(layer.id)) {
                    PWallLog.w("Dropping layer '${layer.id}' after repeated failures")
                    toDrop.add(layer.id)
                }
            }
        }
        toDrop.forEach(layerSystem::remove)
    }

    /** Frees native resources held by layers (scratch bitmaps, ...). */
    fun release() {
        for (layer in layerSystem) {
            if (layer is Releasable) {
                runCatching { layer.release() }
            }
        }
    }
}
