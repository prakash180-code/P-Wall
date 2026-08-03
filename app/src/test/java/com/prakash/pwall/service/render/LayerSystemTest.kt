package com.prakash.pwall.service.render

import com.prakash.pwall.data.model.WallpaperSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class LayerSystemTest {

    private class RecordingLayer(
        override val id: String,
        private val log: MutableList<String>? = null
    ) : Layer {
        override fun draw(canvas: android.graphics.Canvas, frame: RenderFrame) {
            log?.add(id)
        }
    }

    @Test
    fun add_preservesRegistrationOrder() {
        val system = LayerSystem()
        system.add(RecordingLayer("a"))
        system.add(RecordingLayer("b"))
        system.add(RecordingLayer("c"))

        assertEquals(listOf("a", "b", "c"), system.ids)
    }

    @Test
    fun iterate_matchesRegistrationOrder() {
        val system = LayerSystem()
        system.add(RecordingLayer("a"))
        system.add(RecordingLayer("b"))
        system.add(RecordingLayer("c"))

        val visited = mutableListOf<String>()
        for (layer in system) {
            visited.add(layer.id)
        }

        assertEquals(listOf("a", "b", "c"), visited)
    }

    @Test
    fun get_returnsLayerById() {
        val system = LayerSystem()
        val layer = RecordingLayer("x")
        system.add(layer)

        assertSame(layer, system.get("x"))
        assertNull(system.get("missing"))
    }

    @Test
    fun remove_removesByIdAndUpdatesOrder() {
        val system = LayerSystem()
        system.add(RecordingLayer("a"))
        system.add(RecordingLayer("b"))
        system.add(RecordingLayer("c"))

        system.remove("b")

        assertEquals(listOf("a", "c"), system.ids)
        assertEquals(2, system.size)
    }

    @Test
    fun insertBefore_placesLayerAheadOfTarget() {
        val system = LayerSystem()
        system.add(RecordingLayer("background"))
        system.add(RecordingLayer("clock"))
        system.add(RecordingLayer("date"))

        val inserted = system.insertBefore("clock", RecordingLayer("glass"))

        assertEquals(true, inserted)
        assertEquals(listOf("background", "glass", "clock", "date"), system.ids)
    }

    @Test
    fun insertBefore_missingTarget_returnsFalseWithoutChanging() {
        val system = LayerSystem()
        system.add(RecordingLayer("background"))
        system.add(RecordingLayer("clock"))

        val inserted = system.insertBefore("missing", RecordingLayer("glass"))

        assertEquals(false, inserted)
        assertEquals(listOf("background", "clock"), system.ids)
    }

    @Test
    fun renderFrame_providesDefaults() {
        val frame = RenderFrame(settings = WallpaperSettings())
        assertEquals(null, frame.backgroundBitmap)
    }
}
