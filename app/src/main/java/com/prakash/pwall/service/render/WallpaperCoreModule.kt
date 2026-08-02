package com.prakash.pwall.service.render

import com.prakash.pwall.service.render.layers.BackgroundLayer
import com.prakash.pwall.service.render.layers.ClockLayer
import com.prakash.pwall.service.render.layers.DateLayer
import com.prakash.pwall.service.render.layers.ForegroundLayer
import com.prakash.pwall.service.render.layers.OverlayLayer

/**
 * Stock module: registers the default wallpaper layers in z-order (background,
 * clock, date, foreground, overlay).
 */
class WallpaperCoreModule : Module {

    override val id: String = "core"

    override fun register(engine: WallpaperRenderEngine) {
        engine.addLayer(BackgroundLayer())
        engine.addLayer(ClockLayer())
        engine.addLayer(DateLayer())
        engine.addLayer(ForegroundLayer())
        engine.addLayer(OverlayLayer())
    }
}
