package app.shutterup.ui.calendar

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** One cell in the month grid (leading/trailing days included). */
data class CalendarCell(
    val date: LocalDate,
    val inMonth: Boolean,
    val status: DayStatus?,
    val frozen: Boolean,
    val thumbPath: String?,
    val isToday: Boolean,
    val isFuture: Boolean,
)

/**
 * Monday-first month grid covering the visible weeks that contain [month].
 */
fun monthCells(
    month: YearMonth,
    days: Map<LocalDate, DayPrompt>,
    thumbs: Map<LocalDate, String>,
    today: LocalDate,
): List<CalendarCell> {
    val first = month.atDay(1)
    val start = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val end = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val cells = mutableListOf<CalendarCell>()
    var date = start
    while (!date.isAfter(end)) {
        val prompt = days[date]
        cells += CalendarCell(
            date = date,
            inMonth = date.month == month.month && date.year == month.year,
            status = prompt?.status,
            frozen = prompt?.frozen == true,
            thumbPath = thumbs[date],
            isToday = date == today,
            isFuture = date.isAfter(today),
        )
        date = date.plusDays(1)
    }
    return cells
}

private val cellDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH)

/** Spoken date, e.g. `19 September` (DESIGN §11). */
fun spokenDate(date: LocalDate): String = date.format(cellDateFormatter)

/**
 * Spoken status for a cell (DESIGN §11): "19 September, completed".
 */
fun calendarCellDescription(cell: CalendarCell): String {
    val datePart = spokenDate(cell.date)
    if (!cell.inMonth) return datePart
    val status = when {
        cell.isFuture -> null
        cell.status == DayStatus.COMPLETED || cell.status == DayStatus.COMPLETED_NO_PHOTO -> "completed"
        cell.status == DayStatus.MISSED -> "missed"
        cell.status == DayStatus.SKIPPED -> "skipped"
        cell.status == DayStatus.PAUSED -> "paused"
        cell.isToday && (cell.status == DayStatus.PENDING || cell.status == null) -> "pending"
        else -> null
    }
    val frozen = if (cell.frozen && (cell.status == DayStatus.MISSED || cell.status == DayStatus.SKIPPED)) {
        ", streak frozen"
    } else {
        ""
    }
    return if (status != null) "$datePart, $status$frozen" else datePart
}
