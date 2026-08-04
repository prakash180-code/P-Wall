package com.prakash.pwall.utils

import com.prakash.pwall.data.model.ClockLayout
import com.prakash.pwall.data.model.DateFormat
import com.prakash.pwall.data.model.TimeFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats the current time/date for both the live wallpaper and the preview,
 * guaranteeing identical output on both surfaces.
 *
 * [formatTime] keeps the classic single-line output used by the horizontal
 * layout; [formatTimeLines] returns the stacked lines for the vertical layouts
 * (2-digit padded hours/minutes so the digits always align, with an optional
 * seconds line and, in the compact vertical layout, an AM/PM marker).
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

    /**
     * Layout-aware clock text. For [ClockLayout.HORIZONTAL] this matches
     * [formatTime]; vertical layouts return the stacked lines joined by "\n".
     * Used as the stable identity string for transitions and frame signatures.
     */
    fun formatClock(
        now: LocalDateTime,
        timeFormat: TimeFormat,
        showSeconds: Boolean,
        layout: ClockLayout
    ): String = formatTimeLines(now, timeFormat, showSeconds, layout).joinToString("\n")

    /**
     * The time split into stacked lines for a [ClockLayout]. Hours/minutes (and
     * seconds) are 2-digit zero-padded so the vertical layouts always align.
     */
    fun formatTimeLines(
        now: LocalDateTime,
        timeFormat: TimeFormat,
        showSeconds: Boolean,
        layout: ClockLayout
    ): List<String> {
        val hourFormatter = if (timeFormat.is24Hour) {
            DateTimeFormatter.ofPattern("HH", Locale.getDefault())
        } else {
            DateTimeFormatter.ofPattern("hh", Locale.getDefault())
        }
        val hh = hourFormatter.format(now)
        val mm = now.minute.toString().padStart(2, '0')
        val ss = now.second.toString().padStart(2, '0')

        return when (layout) {
            ClockLayout.HORIZONTAL -> listOf(formatTime(now, timeFormat, showSeconds))
            ClockLayout.VERTICAL_DIGITAL -> buildList {
                add(hh)
                add(mm)
                if (showSeconds) add(ss)
            }
            ClockLayout.STACKED_DIGITAL -> buildList {
                add(hh)
                add(":")
                add(mm)
                if (showSeconds) add(ss)
            }
            ClockLayout.COMPACT_VERTICAL -> buildList {
                add(hh)
                add(mm)
                if (timeFormat.is24Hour) {
                    if (showSeconds) add(ss)
                } else {
                    add(if (now.hour < 12) "AM" else "PM")
                }
            }
        }
    }
}
