package com.prakash.pwall.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.prakash.pwall.data.model.AppTheme
import com.prakash.pwall.data.model.ClockLayout
import com.prakash.pwall.data.model.ParallaxSensitivityLevel
import com.prakash.pwall.data.model.TimeFormat
import com.prakash.pwall.data.model.WallpaperSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupTest {

    private val sample = WallpaperSettings(
        timeFormat = TimeFormat.HOUR_12,
        showSeconds = false,
        clockLayout = ClockLayout.STACKED_DIGITAL,
        parallaxEnabled = true,
        parallaxSensitivityLevel = ParallaxSensitivityLevel.HIGH,
        parallaxStrength = 0.8f,
        parallaxSmoothing = 0.2f,
        glassEnabled = true,
        glassPanelOpacity = 60,
        zoomEnabled = true,
        zoomDirection = com.prakash.pwall.data.model.ZoomDirection.ZOOM_IN,
        appTheme = AppTheme.CUSTOM,
        customPrimaryColor = Color(0xFF123456).toArgb().toLong(),
        customAccentColor = 0xFFABCDEF.toLong(),
        transparency = 75
    )

    @Test
    fun roundTrip_preservesEveryField() {
        val json = SettingsBackup.encode(sample)
        assertTrue(json.startsWith("{") && json.endsWith("}"))

        val decoded = SettingsBackup.decode(json)
        assertEquals(sample, decoded)
    }

    @Test
    fun decode_missingFields_fallBackToDefaults() {
        val decoded = SettingsBackup.decode("{}")
        assertEquals(WallpaperSettings(), decoded)
    }

    @Test
    fun decode_invalidPayload_returnsNull() {
        assertNull(SettingsBackup.decode("not json"))
        assertNull(SettingsBackup.decode("{oops}"))
        assertNull(SettingsBackup.decode("[1,2,3]"))
    }

    @Test
    fun decode_toleratesUnknownKeys() {
        val json = SettingsBackup.encode(sample).replace(
            "{",
            "{\"unknown_future_key\":\"value\","
        )
        assertEquals(sample, SettingsBackup.decode(json))
    }
}
