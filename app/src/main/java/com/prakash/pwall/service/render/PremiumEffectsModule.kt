package com.prakash.pwall.service.render

import com.prakash.pwall.service.render.layers.GlassPanelLayer

/**
 * Premium effects module: adds the glass-clock panel directly beneath the clock
 * layer. The panel is a no-op whenever the glass feature is disabled, so the
 * default render stays byte-for-byte identical to the core stack.
 */
class PremiumEffectsModule : Module {

    override val id: String = "premium-effects"

    override fun register(engine: WallpaperRenderEngine) {
        engine.insertLayerBefore("clock", GlassPanelLayer())
    }
}
