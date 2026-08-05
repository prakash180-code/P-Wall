package com.prakash.pwall.utils

import com.prakash.pwall.data.model.ClockLayout
import com.prakash.pwall.data.model.DateFormat
import com.prakash.pwall.data.model.DateLayout
import com.prakash.pwall.data.model.TimeFormat
import com.prakash.pwall.data.model.TimeLayout
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.util.Locale

class ClockTextFormatterTest {

    private val fixed = LocalDateTime.of(2026, 7, 15, 13, 5, 9)

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun formatTime_24h_withSeconds() {
        assertEquals("13:05:09", ClockTextFormatter.formatTime(fixed, TimeFormat.HOUR_24, true))
    }

    @Test
    fun formatTime_24h_withoutSeconds() {
        assertEquals("13:05", ClockTextFormatter.formatTime(fixed, TimeFormat.HOUR_24, false))
    }

    @Test
    fun formatTime_12h_withSeconds() {
        assertEquals("1:05:09 PM", ClockTextFormatter.formatTime(fixed, TimeFormat.HOUR_12, true))
    }

    @Test
    fun formatTime_12h_withoutSeconds() {
        assertEquals("1:05 PM", ClockTextFormatter.formatTime(fixed, TimeFormat.HOUR_12, false))
    }

    @Test
    fun formatDate_dayMonthYear() {
        assertEquals("15 July 2026", ClockTextFormatter.formatDate(fixed, DateFormat.DAY_MONTH_YEAR))
    }

    @Test
    fun formatDate_numericDmy() {
        assertEquals("15/07/2026", ClockTextFormatter.formatDate(fixed, DateFormat.NUMERIC_DMY))
    }

    @Test
    fun formatDate_dayOnly() {
        assertEquals("Wednesday", ClockTextFormatter.formatDate(fixed, DateFormat.DAY_ONLY))
    }

    @Test
    fun formatDate_dayDate() {
        assertEquals("Wednesday, 15 July", ClockTextFormatter.formatDate(fixed, DateFormat.DAY_DATE))
    }

    @Test
    fun formatClock_horizontal_matchesLegacyTime() {
        assertEquals(
            ClockTextFormatter.formatTime(fixed, TimeFormat.HOUR_24, true),
            ClockTextFormatter.formatClock(fixed, TimeFormat.HOUR_24, true, ClockLayout.HORIZONTAL)
        )
    }

    @Test
    fun formatTimeLines_verticalDigital_24hWithSeconds() {
        assertEquals(
            listOf("13", "05", "09"),
            ClockTextFormatter.formatTimeLines(
                fixed, TimeFormat.HOUR_24, true, ClockLayout.VERTICAL_DIGITAL
            )
        )
    }

    @Test
    fun formatTimeLines_verticalDigital_24hWithoutSeconds() {
        assertEquals(
            listOf("13", "05"),
            ClockTextFormatter.formatTimeLines(
                fixed, TimeFormat.HOUR_24, false, ClockLayout.VERTICAL_DIGITAL
            )
        )
    }

    @Test
    fun formatTimeLines_stackedDigital_includesColon() {
        assertEquals(
            listOf("13", ":", "05", "09"),
            ClockTextFormatter.formatTimeLines(
                fixed, TimeFormat.HOUR_24, true, ClockLayout.STACKED_DIGITAL
            )
        )
    }

    @Test
    fun formatTimeLines_compactVertical_12hUsesAmPm() {
        assertEquals(
            listOf("01", "05", "PM"),
            ClockTextFormatter.formatTimeLines(
                fixed, TimeFormat.HOUR_12, true, ClockLayout.COMPACT_VERTICAL
            )
        )
    }

    @Test
    fun formatTimeLines_compactVertical_24hUsesSeconds() {
        assertEquals(
            listOf("13", "05", "09"),
            ClockTextFormatter.formatTimeLines(
                fixed, TimeFormat.HOUR_24, true, ClockLayout.COMPACT_VERTICAL
            )
        )
    }

    @Test
    fun formatClock_joinsLinesWithNewline() {
        assertEquals(
            "13\n05\n09",
            ClockTextFormatter.formatClock(fixed, TimeFormat.HOUR_24, true, ClockLayout.VERTICAL_DIGITAL)
        )
    }

    // --- Clock & Date Engine: TimeLayout ---

    @Test
    fun formatTimeLines_timeLayout_legacyMirrorsMatch() {
        val layouts = mapOf(
            TimeLayout.HORIZONTAL to ClockLayout.HORIZONTAL,
            TimeLayout.VERTICAL to ClockLayout.VERTICAL_DIGITAL,
            TimeLayout.STACKED to ClockLayout.STACKED_DIGITAL,
            TimeLayout.COMPACT to ClockLayout.COMPACT_VERTICAL
        )
        layouts.forEach { (timeLayout, legacy) ->
            assertEquals(
                ClockTextFormatter.formatTimeLines(fixed, TimeFormat.HOUR_24, true, legacy),
                ClockTextFormatter.formatTimeLines(fixed, TimeFormat.HOUR_24, true, timeLayout)
            )
        }
    }

    @Test
    fun formatTimeLines_split_returnsUnitsInReadingOrder() {
        assertEquals(
            listOf("13", ":", "05", ":", "09"),
            ClockTextFormatter.formatTimeLines(fixed, TimeFormat.HOUR_24, true, TimeLayout.SPLIT)
        )
        assertEquals(
            listOf("13", ":", "05"),
            ClockTextFormatter.formatTimeLines(fixed, TimeFormat.HOUR_24, false, TimeLayout.SPLIT)
        )
    }

    @Test
    fun formatTimeLines_minimal_dropsSeconds() {
        assertEquals(
            listOf("13:05"),
            ClockTextFormatter.formatTimeLines(fixed, TimeFormat.HOUR_24, true, TimeLayout.MINIMAL)
        )
    }

    @Test
    fun formatTimeLines_centered_singleLine() {
        assertEquals(
            listOf("13:05:09"),
            ClockTextFormatter.formatTimeLines(fixed, TimeFormat.HOUR_24, true, TimeLayout.CENTERED)
        )
    }

    @Test
    fun formatTimeIdentity_split_compactsToClockFace() {
        assertEquals(
            "13:05:09",
            ClockTextFormatter.formatTimeIdentity(fixed, TimeFormat.HOUR_24, true, TimeLayout.SPLIT)
        )
    }

    @Test
    fun legacyClockLayout_mapsOnlyLegacyLayouts() {
        assertEquals(ClockLayout.HORIZONTAL, ClockTextFormatter.legacyClockLayout(TimeLayout.HORIZONTAL))
        assertEquals(null, ClockTextFormatter.legacyClockLayout(TimeLayout.SPLIT))
        assertEquals(null, ClockTextFormatter.legacyClockLayout(TimeLayout.MINIMAL))
    }

    // --- Clock & Date Engine: DateLayout ---

    @Test
    fun formatDateLines_horizontal_usesUserDateFormat() {
        assertEquals(
            listOf("15 July 2026"),
            ClockTextFormatter.formatDateLines(fixed, DateLayout.HORIZONTAL, DateFormat.DAY_MONTH_YEAR)
        )
    }

    @Test
    fun formatDateLines_vertical_stacksDayDateYear() {
        assertEquals(
            listOf("Wednesday", "15 July", "2026"),
            ClockTextFormatter.formatDateLines(fixed, DateLayout.VERTICAL)
        )
    }

    @Test
    fun formatDateLines_monthName_monthThenDate() {
        assertEquals(
            listOf("July", "15, 2026"),
            ClockTextFormatter.formatDateLines(fixed, DateLayout.MONTH_NAME)
        )
    }

    @Test
    fun formatDateLines_compact_singleLine() {
        assertEquals(
            listOf("15 Jul"),
            ClockTextFormatter.formatDateLines(fixed, DateLayout.COMPACT)
        )
    }

    @Test
    fun formatDateIdentity_joinsLines() {
        assertEquals(
            "Wednesday\n15 July\n2026",
            ClockTextFormatter.formatDateIdentity(fixed, DateLayout.VERTICAL)
        )
    }
}
