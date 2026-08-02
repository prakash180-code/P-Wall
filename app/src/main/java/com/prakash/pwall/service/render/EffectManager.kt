package com.prakash.pwall.service.render

import android.graphics.Canvas

/**
 * Applies registered [Effect]s after each layer draws. No-op by default; future
 * premium modules register their effects here.
 */
class EffectManager {

    private val effects = mutableListOf<Effect>()

    fun add(effect: Effect): Boolean = effects.add(effect)

    fun remove(id: String): Effect? {
        val index = effects.indexOfFirst { it.id == id }
        return if (index >= 0) effects.removeAt(index) else null
    }

    val ids: List<String> get() = effects.map { it.id }

    val size: Int get() = effects.size

    fun apply(canvas: Canvas, frame: RenderFrame, layer: Layer) {
        for (effect in effects) {
            effect.apply(canvas, frame, layer)
        }
    }
}
