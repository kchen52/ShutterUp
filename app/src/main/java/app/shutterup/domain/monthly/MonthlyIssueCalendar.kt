package app.shutterup.domain.monthly

import java.time.LocalDate
import java.time.YearMonth

/**
 * Which calendar months belong in The Monthly, and how a month is sliced.
 *
 * An issue covers a **finished** calendar month in the device's current timezone:
 * generated on or after the 1st of the following month. The current month is
 * never due. A mid-month install therefore produces nothing until the next 1st.
 * A device that was off across the boundary catches up every finished month
 * from the earliest completed day through last month.
 */
object MonthlyIssueCalendar {
    fun window(yearMonth: YearMonth): Pair<LocalDate, LocalDate> =
        yearMonth.atDay(1) to yearMonth.atEndOfMonth()

    fun contains(yearMonth: YearMonth, date: LocalDate): Boolean =
        YearMonth.from(date) == yearMonth

    /**
     * Finished months that may need an issue as of [today], starting at the
     * month of [earliestCompleted]. Empty when there is no history, or when
     * the only completed days sit in the still-open current month.
     */
    fun dueMonths(today: LocalDate, earliestCompleted: LocalDate?): List<YearMonth> {
        if (earliestCompleted == null) return emptyList()
        val lastFinished = YearMonth.from(today).minusMonths(1)
        val first = YearMonth.from(earliestCompleted)
        if (first.isAfter(lastFinished)) return emptyList()
        val months = ArrayList<YearMonth>()
        var cursor = first
        while (!cursor.isAfter(lastFinished)) {
            months += cursor
            cursor = cursor.plusMonths(1)
        }
        return months
    }

    fun isFinished(yearMonth: YearMonth, today: LocalDate): Boolean =
        yearMonth.isBefore(YearMonth.from(today))

    /**
     * Longest run of consecutive calendar days in [dates]. Isolated days
     * yield 1. An empty list yields 0.
     */
    fun longestRun(dates: Collection<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        val sorted = dates.toSortedSet()
        var best = 1
        var run = 1
        var previous: LocalDate? = null
        for (date in sorted) {
            if (previous != null && date == previous.plusDays(1)) {
                run += 1
                if (run > best) best = run
            } else {
                run = 1
            }
            previous = date
        }
        return best
    }
}
