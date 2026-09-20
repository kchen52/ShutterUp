package app.shutterup.domain.take

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * Factual interval between two takes. The user compares the photographs;
 * the app does not.
 *
 * Thresholds: one day; a calendar month when the span is 28–44 days;
 * a year at 365 days; otherwise the day count (so 83 days stays "83 days on").
 */
object TakeInterval {
    private val DATE: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH)
    private val DATE_KICKER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH)

    fun phrase(from: LocalDate, to: LocalDate): String {
        val later = if (to.isBefore(from)) from else to
        val earlier = if (to.isBefore(from)) to else from
        val days = ChronoUnit.DAYS.between(earlier, later).absoluteValue
        val months = ChronoUnit.MONTHS.between(earlier, later).absoluteValue
        val years = ChronoUnit.YEARS.between(earlier, later).absoluteValue
        return when {
            years >= 2L -> "$years years on"
            years == 1L -> "a year on"
            days == 0L -> "the same day"
            days == 1L -> "a day on"
            months == 1L && days in 28L..44L -> "a month on"
            else -> "$days days on"
        }
    }

    fun dateKicker(date: LocalDate): String =
        date.format(DATE_KICKER).uppercase(Locale.ENGLISH)

    fun friendlyDate(date: LocalDate): String = date.format(DATE)

    fun laterTakesLine(later: List<LocalDate>): String? {
        if (later.isEmpty()) return null
        val first = friendlyDate(later.first())
        return when (later.size) {
            1 -> "Shot again on $first."
            2 -> "Shot again on $first and ${friendlyDate(later[1])}."
            else -> "Shot again on $first and ${later.size - 1} more."
        }
    }
}
