package com.prakash.pwall.service.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleSystemTest {

    private class TestModule(
        override val id: String
    ) : Module {
        val registerCount = java.util.concurrent.atomic.AtomicInteger(0)
        override fun register(engine: WallpaperRenderEngine) {
            registerCount.incrementAndGet()
        }
    }

    @Test
    fun add_rejectsDuplicateIds() {
        val system = ModuleSystem()
        assertTrue(system.add(TestModule("m")))
        assertFalse(system.add(TestModule("m")))
        assertTrue(system.add(TestModule("n")))

        assertEquals(listOf("m", "n"), system.ids)
        assertEquals(2, system.size)
    }

    @Test
    fun installAll_registersEveryModuleOnce() {
        val system = ModuleSystem()
        val first = TestModule("a")
        val second = TestModule("b")
        system.add(first)
        system.add(second)

        system.installAll(WallpaperRenderEngine())

        assertEquals(1, first.registerCount.get())
        assertEquals(1, second.registerCount.get())
    }
}
