package app.shutterup.ui.home

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import java.time.LocalDate
import java.time.YearMonth

/**
 * Home month ring: completed / (days elapsed − paused) (SPEC §6.3).
 */
data class MonthProgress(
    val completed: Int,
    val eligible: Int,
)

/** Eligible days are elapsed days in [month] up through [today], minus [DayStatus.PAUSED]. */
fun monthProgress(
    days: List<DayPrompt>,
    today: LocalDate,
    month: YearMonth,
): MonthProgress {
    val start = month.atDay(1)
    val lastElapsed = minOf(today, month.atEndOfMonth())
    if (lastElapsed.isBefore(start)) return MonthProgress(0, 0)
    val byDate = days.associateBy { it.date }
    var completed = 0
    var paused = 0
    var elapsed = 0
    var date = start
    while (!date.isAfter(lastElapsed)) {
        elapsed++
        when (byDate[date]?.status) {
            DayStatus.COMPLETED, DayStatus.COMPLETED_NO_PHOTO -> completed++
            DayStatus.PAUSED -> paused++
            else -> Unit
        }
        date = date.plusDays(1)
    }
    return MonthProgress(completed = completed, eligible = (elapsed - paused).coerceAtLeast(0))
}
