package com.prakash.pwall.utils

import com.prakash.pwall.data.model.DateFormat
import com.prakash.pwall.data.model.TimeFormat
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
}
