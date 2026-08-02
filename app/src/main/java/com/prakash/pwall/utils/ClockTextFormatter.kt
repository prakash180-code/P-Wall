package com.prakash.pwall.utils

import com.prakash.pwall.data.model.DateFormat
import com.prakash.pwall.data.model.TimeFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats the current time/date for both the live wallpaper and the preview,
 * guaranteeing identical output on both surfaces.
 */
object ClockTextFormatter {

    private fun timeFormatter(timeFormat: TimeFormat, showSeconds: Boolean): DateTimeFormatter {
        val pattern = when {
            timeFormat.is24Hour && showSeconds -> "HH:mm:ss"
            timeFormat.is24Hour -> "HH:mm"
            showSeconds -> "h:mm:ss a"
            else -> "h:mm a"
        }
        return DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    }

    fun formatTime(
        now: LocalDateTime,
        timeFormat: TimeFormat,
        showSeconds: Boolean
    ): String = timeFormatter(timeFormat, showSeconds).format(now)

    fun formatDate(now: LocalDateTime, dateFormat: DateFormat): String =
        DateTimeFormatter.ofPattern(dateFormat.pattern, Locale.getDefault()).format(now)
}
