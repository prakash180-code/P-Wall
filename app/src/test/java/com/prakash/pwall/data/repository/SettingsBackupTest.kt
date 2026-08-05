package com.prakash.pwall.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.prakash.pwall.data.model.AppTheme
import com.prakash.pwall.data.model.ClockFont
import com.prakash.pwall.data.model.ClockLayout
import com.prakash.pwall.data.model.DateLayout
import com.prakash.pwall.data.model.ParallaxSensitivityLevel
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.TimeFormat
import com.prakash.pwall.data.model.TimeLayout
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.model.WidgetStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupTest {

    private val sample = WallpaperSettings(
        timeFormat = TimeFormat.HOUR_12,
        showSeconds = false,
        clockLayout = ClockLayout.STACKED_DIGITAL,
        clockVisible = true,
        dateVisible = false,
        timeLayout = TimeLayout.VERTICAL,
        dateLayout = DateLayout.MONTH_NAME,
        timeStyle = WidgetStyle.NEON,
        dateStyle = WidgetStyle.GLASS,
        dateLinkedToTime = false,
        dateGapMultiplier = 1.75f,
        datePosition = PositionPreset.TOP_RIGHT,
        datePositionXFraction = 0.2f,
        datePositionYFraction = 0.3f,
        dateFont = ClockFont.MONOSPACE,
        dateFontSizeSp = 24f,
        dateBold = true,
        dateItalic = true,
        dateShadowEnabled = false,
        dateShadowBlurRadius = 9f,
        dateShadowOffsetX = -3f,
        dateShadowOffsetY = 5f,
        dateShadowColor = 0xFFABCDEF.toLong(),
        dateAnimated = false,
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

    @Test
    fun decode_legacyBackup_migratesClockLayoutToTimeLayout() {
        // A v1.0.1 backup: only the legacy clock_layout key, no widget keys.
        val legacyJson =
            "{\"clock_layout\":\"STACKED_DIGITAL\",\"time_format\":\"HOUR_24\",\"show_seconds\":false}"
        val decoded = SettingsBackup.decode(legacyJson)
        assertEquals(TimeLayout.STACKED, decoded?.timeLayout)
        // New widget fields fall back to classic defaults.
        assertEquals(WidgetStyle.CLASSIC, decoded?.timeStyle)
        assertEquals(WidgetStyle.CLASSIC, decoded?.dateStyle)
        assertEquals(TimeLayout.HORIZONTAL, WallpaperSettings().timeLayout)
    }

    @Test
    fun decode_timeLayoutKeyWinsOverLegacy() {
        val json = SettingsBackup.encode(
            WallpaperSettings(
                clockLayout = ClockLayout.STACKED_DIGITAL,
                timeLayout = TimeLayout.SPLIT
            )
        )
        assertEquals(TimeLayout.SPLIT, SettingsBackup.decode(json)?.timeLayout)
    }
}
