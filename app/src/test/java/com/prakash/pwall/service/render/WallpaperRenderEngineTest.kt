package com.prakash.pwall.service.render

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperRenderEngineTest {

    private class NoOpLayer(override val id: String) : Layer {
        override fun draw(canvas: android.graphics.Canvas, frame: RenderFrame) = Unit
    }

    private class NoOpEffect(override val id: String) : Effect {
        override fun apply(
            canvas: android.graphics.Canvas,
            frame: RenderFrame,
            layer: Layer
        ) = Unit
    }

    @Test
    fun coreModule_registersFiveLayersInZOrder() {
        val engine = WallpaperRenderEngine()
        engine.installModule(WallpaperCoreModule())

        assertEquals(
            listOf("background", "clock", "date", "foreground", "overlay"),
            engine.layerIds
        )
        assertEquals(emptyList<String>(), engine.effectIds)
    }

    @Test
    fun premiumModule_layersStackAboveCore() {
        val engine = WallpaperRenderEngine()
        engine.installModule(WallpaperCoreModule())
        engine.installModule(object : Module {
            override val id: String = "premium"
            override fun register(engine: WallpaperRenderEngine) {
                engine.addLayer(NoOpLayer("premium-fx"))
            }
        })

        assertEquals(
            listOf("background", "clock", "date", "foreground", "overlay", "premium-fx"),
            engine.layerIds
        )
    }

    @Test
    fun duplicateModuleInstall_isIgnored() {
        val engine = WallpaperRenderEngine()
        engine.installModule(WallpaperCoreModule())
        engine.installModule(WallpaperCoreModule())

        assertEquals(5, engine.layerIds.size)
    }

    @Test
    fun module_registersLayersAndEffectsInOrder() {
        val engine = WallpaperRenderEngine()
        engine.installModule(object : Module {
            override val id: String = "test"
            override fun register(engine: WallpaperRenderEngine) {
                engine.addLayer(NoOpLayer("extra-1"))
                engine.addEffect(NoOpEffect("fx-1"))
                engine.addLayer(NoOpLayer("extra-2"))
                engine.addEffect(NoOpEffect("fx-2"))
            }
        })

        assertEquals(listOf("extra-1", "extra-2"), engine.layerIds)
        assertEquals(listOf("fx-1", "fx-2"), engine.effectIds)
    }

    @Test
    fun premiumEffectsModule_placesGlassPanelBelowClock() {
        val engine = WallpaperRenderEngine()
        engine.installModule(WallpaperCoreModule())
        engine.installModule(PremiumEffectsModule())

        assertEquals(
            listOf("background", "glass-panel", "clock", "date", "foreground", "overlay"),
            engine.layerIds
        )
    }

    @Test
    fun insertLayerBefore_missingTarget_doesNotAdd() {
        val engine = WallpaperRenderEngine()
        engine.installModule(WallpaperCoreModule())

        val inserted = engine.insertLayerBefore("missing", NoOpLayer("ghost"))

        assertEquals(false, inserted)
        assertEquals(
            listOf("background", "clock", "date", "foreground", "overlay"),
            engine.layerIds
        )
    }

    @Test
    fun removeLayer_removesById() {
        val engine = WallpaperRenderEngine()
        engine.installModule(WallpaperCoreModule())

        engine.removeLayer("foreground")

        assertEquals(
            listOf("background", "clock", "date", "overlay"),
            engine.layerIds
        )
    }
}
