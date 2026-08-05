package com.prakash.pwall.service.performance

import com.prakash.pwall.data.model.LowEndPreference
import com.prakash.pwall.data.model.WallpaperSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LowEndDeviceTest {

    @Test
    fun on_alwaysLowEnd() {
        assertTrue(LowEndDevice.decide(4096, false, LowEndPreference.ON))
        assertTrue(LowEndDevice.decide(64, false, LowEndPreference.ON))
    }

    @Test
    fun off_neverLowEnd() {
        assertFalse(LowEndDevice.decide(64, true, LowEndPreference.OFF))
        assertFalse(LowEndDevice.decide(4096, true, LowEndPreference.OFF))
    }

    @Test
    fun auto_lowRamDevice_isLowEnd() {
        assertTrue(LowEndDevice.decide(256, true, LowEndPreference.AUTO))
    }

    @Test
    fun auto_lowMemoryClass_isLowEnd() {
        assertTrue(LowEndDevice.decide(128, false, LowEndPreference.AUTO))
        assertTrue(LowEndDevice.decide(160, false, LowEndPreference.AUTO))
    }

    @Test
    fun auto_capableDevice_isNotLowEnd() {
        assertFalse(LowEndDevice.decide(256, false, LowEndPreference.AUTO))
    }

    @Test
    fun optimizedSettings_unchangedWhenNotLowEnd() {
        val settings = WallpaperSettings()
        assertSame(settings, LowEndDevice.optimizedSettings(settings, lowEnd = false))
    }

    @Test
    fun optimizedSettings_disablesHeavyEffectsOnly() {
        val settings = WallpaperSettings(
            glassEnabled = true,
            breathingEnabled = true,
            zoomEnabled = true,
            smoothSecondsEnabled = true,
            fadeTransitionsEnabled = true,
            parallaxEnabled = true,
            shadowEnabled = true
        )
        val optimized = LowEndDevice.optimizedSettings(settings, lowEnd = true)

        assertFalse(optimized.glassEnabled)
        assertFalse(optimized.breathingEnabled)
        assertFalse(optimized.zoomEnabled)
        assertFalse(optimized.smoothSecondsEnabled)
        assertFalse(optimized.fadeTransitionsEnabled)
        assertFalse(optimized.parallaxEnabled)
        assertFalse(optimized.shadowEnabled)

        // Non-performance identity is preserved.
        assertEquals(settings.position, optimized.position)
        assertEquals(settings.clockFont, optimized.clockFont)
        assertEquals(settings.clockColor, optimized.clockColor)
    }

    @Test
    fun optimizedSettings_lowEndDisablesDateEffects() {
        val settings = WallpaperSettings(
            dateAnimated = true,
            dateShadowEnabled = true,
            dateBold = true
        )
        val optimized = LowEndDevice.optimizedSettings(settings, lowEnd = true)
        assertFalse(optimized.dateAnimated)
        assertFalse(optimized.dateShadowEnabled)
        // Typography is not a per-frame cost, so it is preserved.
        assertTrue(optimized.dateBold)
    }
}
