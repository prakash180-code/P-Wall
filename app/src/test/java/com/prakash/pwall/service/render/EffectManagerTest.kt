package com.prakash.pwall.service.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class EffectManagerTest {

    private class RecordingEffect(
        override val id: String
    ) : Effect {
        override fun apply(
            canvas: android.graphics.Canvas,
            frame: RenderFrame,
            layer: Layer
        ) {
            // No-op; ordering is asserted via ids.
        }
    }

    @Test
    fun add_preservesRegistrationOrder() {
        val manager = EffectManager()
        manager.add(RecordingEffect("a"))
        manager.add(RecordingEffect("b"))

        assertEquals(listOf("a", "b"), manager.ids)
        assertEquals(2, manager.size)
    }

    @Test
    fun remove_removesById() {
        val manager = EffectManager()
        val effect = RecordingEffect("x")
        manager.add(effect)
        manager.add(RecordingEffect("y"))

        assertSame(effect, manager.remove("x"))

        assertEquals(listOf("y"), manager.ids)
        assertNull(manager.remove("missing"))
    }

    @Test
    fun initiallyEmpty() {
        val manager = EffectManager()
        assertEquals(0, manager.size)
        assertEquals(emptyList<String>(), manager.ids)
    }
}
