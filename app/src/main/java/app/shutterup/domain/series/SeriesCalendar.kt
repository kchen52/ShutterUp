package app.shutterup.domain.series

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Seven consecutive dates, inclusive. */
object SeriesCalendar {
    const val LENGTH = 7

    fun dates(start: LocalDate): List<LocalDate> =
        (0 until LENGTH).map { start.plusDays(it.toLong()) }

    /** 1-based index of [date] in a series starting at [start]. */
    fun indexOf(start: LocalDate, date: LocalDate): Int =
        ChronoUnit.DAYS.between(start, date).toInt() + 1

    /**
     * A new series begins on an empty date when the setting is on and that
     * date is not already inside a series. Enabling the setting starts that
     * run tomorrow rather than rewriting today.
     * Turning the setting off never starts a series; existing series days
     * stay put.
     */
    fun shouldStartSeries(seriesEnabled: Boolean, alreadyInSeries: Boolean): Boolean =
        seriesEnabled && !alreadyInSeries

    /**
     * First date on or after [today] that has no prompt. Subsequent series
     * begin the day after the previous one ends because those seven days
     * already have prompts.
     */
    fun nextEmptyDate(
        today: LocalDate,
        hasPrompt: (LocalDate) -> Boolean,
        searchLimitDays: Int = 21,
    ): LocalDate? {
        var date = today
        val last = today.plusDays(searchLimitDays.toLong())
        while (!date.isAfter(last)) {
            if (!hasPrompt(date)) return date
            date = date.plusDays(1)
        }
        return null
    }
}

/** Derive a library series title from a theme label. */
fun librarySeriesTitle(theme: String): String {
    val trimmed = theme.trim()
    if (trimmed.isEmpty()) return "A Week of Light"
    if (trimmed.startsWith("A Week of ", ignoreCase = true)) {
        return trimmed.take(40)
    }
    val prefixed = "A Week of $trimmed"
    return if (prefixed.length <= 40) prefixed else trimmed.take(40)
}
