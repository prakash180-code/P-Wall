package com.prakash.pwall.service.render

import com.prakash.pwall.service.render.layers.GlassPanelLayer
import com.prakash.pwall.service.render.layers.TimeContainerLayer

/**
 * Premium effects module: adds the glass-clock panel and the time widget
 * container directly beneath the clock layer. Both are no-ops whenever their
 * feature is disabled (glass off / container transparent), so the default
 * render stays byte-for-byte identical to the core stack.
 */
class PremiumEffectsModule : Module {

    override val id: String = "premium-effects"

    override fun register(engine: WallpaperRenderEngine) {
        engine.insertLayerBefore("clock", GlassPanelLayer())
        engine.insertLayerBefore("clock", TimeContainerLayer())
    }
}
