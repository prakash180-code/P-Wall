package com.prakash.pwall.utils

import com.prakash.pwall.data.model.ClockLayout
import com.prakash.pwall.data.model.DateFormat
import com.prakash.pwall.data.model.DateLayout
import com.prakash.pwall.data.model.TimeFormat
import com.prakash.pwall.data.model.TimeLayout
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

    /**
     * Formatters are expensive to build and are requested at least once per
     * second from the render thread and the preview. They are immutable and
     * thread-safe, so cache them by pattern instead of rebuilding. The cache is
     * small (a handful of patterns per locale) and unbounded-safe in practice.
     */
    private val formatterCache = mutableMapOf<String, DateTimeFormatter>()

    private fun formatterFor(pattern: String): DateTimeFormatter =
        formatterCache.getOrPut(pattern) {
            DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        }

    private fun timeFormatter(timeFormat: TimeFormat, showSeconds: Boolean): DateTimeFormatter {
        val pattern = when {
            timeFormat.is24Hour && showSeconds -> "HH:mm:ss"
            timeFormat.is24Hour -> "HH:mm"
            showSeconds -> "h:mm:ss a"
            else -> "h:mm a"
        }
        return formatterFor(pattern)
    }

    fun formatTime(
        now: LocalDateTime,
        timeFormat: TimeFormat,
        showSeconds: Boolean
    ): String = timeFormatter(timeFormat, showSeconds).format(now)

    fun formatDate(now: LocalDateTime, dateFormat: DateFormat): String =
        formatterFor(dateFormat.pattern).format(now)

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
     * Layout-aware clock text for the Clock & Date Engine. Returns the canonical
     * identity string used for transitions, frame signatures and change
     * detection; for the split layout this is a single `HH:MM`-style string.
     */
    fun formatClock(
        now: LocalDateTime,
        timeFormat: TimeFormat,
        showSeconds: Boolean,
        layout: TimeLayout
    ): String = formatTimeIdentity(now, timeFormat, showSeconds, layout)

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
            formatterFor("HH")
        } else {
            formatterFor("hh")
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

    /**
     * Render lines for a [TimeLayout] widget. SPLIT returns the hour/minute
     * (and optional second) units in reading order so the renderer can lay them
     * out side by side; all other layouts return stacked or single lines.
     */
    fun formatTimeLines(
        now: LocalDateTime,
        timeFormat: TimeFormat,
        showSeconds: Boolean,
        layout: TimeLayout
    ): List<String> {
        val legacy = legacyClockLayout(layout)
        if (legacy != null) {
            return formatTimeLines(now, timeFormat, showSeconds, legacy)
        }
        return when (layout) {
            TimeLayout.MINIMAL ->
                listOf(formatterFor(if (timeFormat.is24Hour) "HH:mm" else "h:mm").format(now))
            TimeLayout.SPLIT -> {
                val hh = if (timeFormat.is24Hour) {
                    formatterFor("HH").format(now)
                } else {
                    formatterFor("hh").format(now)
                }
                val mm = now.minute.toString().padStart(2, '0')
                val ss = now.second.toString().padStart(2, '0')
                if (showSeconds) listOf(hh, ":", mm, ":", ss) else listOf(hh, ":", mm)
            }
            TimeLayout.HORIZONTAL,
            TimeLayout.CENTERED,
            TimeLayout.LEFT_ALIGNED,
            TimeLayout.RIGHT_ALIGNED -> listOf(formatTime(now, timeFormat, showSeconds))
            TimeLayout.VERTICAL,
            TimeLayout.STACKED,
            TimeLayout.COMPACT -> emptyList() // unreachable (legacy mapping)
        }
    }

    /** Canonical time identity string for a [TimeLayout] (change detection). */
    fun formatTimeIdentity(
        now: LocalDateTime,
        timeFormat: TimeFormat,
        showSeconds: Boolean,
        layout: TimeLayout
    ): String = when (layout) {
        TimeLayout.SPLIT -> formatTimeLines(now, timeFormat, showSeconds, layout).joinToString("")
        else -> formatTimeLines(now, timeFormat, showSeconds, layout).joinToString("\n")
    }

    /** Maps a [TimeLayout] to its legacy [ClockLayout], or null when new. */
    fun legacyClockLayout(layout: TimeLayout): ClockLayout? = layout.legacyLayout

    /**
     * Render lines for a [DateLayout] widget. HORIZONTAL uses the user's chosen
     * [DateFormat]; the other layouts use fixed patterns so the look is
     * predictable. Returns the lines to stack for the widget.
     */
    fun formatDateLines(now: LocalDateTime, dateLayout: DateLayout): List<String> =
        formatDateLines(now, dateLayout, DateFormat.DAY_MONTH_YEAR)

    fun formatDateLines(
        now: LocalDateTime,
        dateLayout: DateLayout,
        dateFormat: DateFormat
    ): List<String> = when (dateLayout) {
        DateLayout.HORIZONTAL -> listOf(formatDate(now, dateFormat))
        DateLayout.VERTICAL -> listOf(
            formatterFor("EEEE").format(now),
            formatterFor("d MMMM").format(now),
            formatterFor("yyyy").format(now)
        )
        DateLayout.MONTH_NAME -> listOf(
            formatterFor("MMMM").format(now),
            formatterFor("d, yyyy").format(now)
        )
        DateLayout.LONG -> listOf(formatterFor("EEEE, d MMMM yyyy").format(now))
        DateLayout.SHORT -> listOf(formatterFor("EEE d MMM").format(now))
        DateLayout.DAY_FIRST -> listOf(
            formatterFor("d MMMM").format(now),
            formatterFor("yyyy").format(now)
        )
        DateLayout.COMPACT -> listOf(formatterFor("d MMM").format(now))
    }

    /** Canonical date identity string for a [DateLayout] (change detection). */
    fun formatDateIdentity(now: LocalDateTime, dateLayout: DateLayout): String =
        formatDateIdentity(now, dateLayout, DateFormat.DAY_MONTH_YEAR)

    fun formatDateIdentity(now: LocalDateTime, dateLayout: DateLayout, dateFormat: DateFormat): String =
        formatDateLines(now, dateLayout, dateFormat).joinToString("\n")
}
