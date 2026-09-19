package app.shutterup.domain.calendar

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Visual mark for one day in the year grid. Paused days use [ABSENT] so they
 * read as empty rather than missed (DESIGN.md §4.4, reduced form).
 */
enum class YearCellMark {
    /** No entry, future, or paused — hairline cell, not a hole. */
    ABSENT,
    /** Completed (with or without a photo) — theme tint. */
    COMPLETED,
    /** Skipped — reduced ring. */
    SKIPPED,
    /** Missed — reduced dot. Neutral, never a warning colour. */
    MISSED,
    /** Today, still pending — primary ring. */
    PENDING_TODAY,
}

/**
 * One day in a year grid. Layout indexes are 0-based:
 * - [monthIndex] is the printed row (January = 0)
 * - [dayColumn] is the day-of-month column
 * - [weekdayColumn] is Monday-first (Monday = 0)
 * - [weekColumn] is the week from the Monday on or before 1 January
 */
data class YearCell(
    val date: LocalDate,
    val monthIndex: Int,
    val dayColumn: Int,
    val weekdayColumn: Int,
    val weekColumn: Int,
    val mark: YearCellMark,
    val status: DayStatus?,
    val theme: String?,
    val frozen: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean,
)

/** One month band in the printed year: every calendar day, in order. */
data class YearMonthBand(
    val month: Month,
    val cells: List<YearCell>,
)

/** Completed / (elapsed days − paused), same rule as the month ring. */
data class YearProgress(
    val completed: Int,
    val eligible: Int,
)

/**
 * A full year of cells. Always [Year.length] cells so a sparse year still
 * reads as a continuous field rather than scattered dots.
 */
data class YearGrid(
    val year: Int,
    val months: List<YearMonthBand>,
    val progress: YearProgress,
) {
    val cells: List<YearCell> get() = months.flatMap { it.cells }
}

/** Slim day record so the grid stays independent of prompt copy. */
data class YearDayRecord(
    val date: LocalDate,
    val status: DayStatus,
    val theme: String,
    val frozen: Boolean,
)

fun DayPrompt.toYearDayRecord(): YearDayRecord =
    YearDayRecord(date = date, status = status, theme = theme, frozen = frozen)

/**
 * Builds the year grid for [year]. Records in other years are ignored.
 * Days with no record still occupy a cell ([YearCellMark.ABSENT]).
 */
fun yearGrid(
    year: Int,
    days: Collection<YearDayRecord>,
    today: LocalDate,
): YearGrid {
    val byDate = days.filter { it.date.year == year }.associateBy { it.date }
    val jan1 = LocalDate.of(year, 1, 1)
    val gridStart = jan1.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val months = Month.entries.map { month ->
        val start = LocalDate.of(year, month, 1)
        val end = start.with(TemporalAdjusters.lastDayOfMonth())
        val cells = buildList {
            var date = start
            while (!date.isAfter(end)) {
                add(cellFor(date, byDate[date], today, gridStart))
                date = date.plusDays(1)
            }
        }
        YearMonthBand(month = month, cells = cells)
    }
    return YearGrid(
        year = year,
        months = months,
        progress = yearProgress(year, days, today),
    )
}

/**
 * Eligible days are elapsed days of [year] up through [today], minus paused.
 * A future year is 0 / 0; a past year uses every day of that year.
 */
fun yearProgress(
    year: Int,
    days: Collection<YearDayRecord>,
    today: LocalDate,
): YearProgress {
    val start = LocalDate.of(year, 1, 1)
    val endOfYear = LocalDate.of(year, 12, 31)
    val lastElapsed = minOf(today, endOfYear)
    if (lastElapsed.isBefore(start)) return YearProgress(completed = 0, eligible = 0)
    val byDate = days.filter { it.date.year == year }.associateBy { it.date }
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
    return YearProgress(
        completed = completed,
        eligible = (elapsed - paused).coerceAtLeast(0),
    )
}

fun yearLength(year: Int): Int = Year.of(year).length()

private fun cellFor(
    date: LocalDate,
    record: YearDayRecord?,
    today: LocalDate,
    gridStart: LocalDate,
): YearCell {
    val isToday = date == today
    val isFuture = date.isAfter(today)
    val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val mark = markFor(record, isToday, isFuture)
    val theme = if (mark == YearCellMark.COMPLETED) {
        record?.theme?.takeIf { it.isNotBlank() }
    } else {
        null
    }
    return YearCell(
        date = date,
        monthIndex = date.monthValue - 1,
        dayColumn = date.dayOfMonth - 1,
        weekdayColumn = mondayFirstIndex(date.dayOfWeek),
        weekColumn = ChronoUnit.WEEKS.between(gridStart, weekStart).toInt(),
        mark = mark,
        status = record?.status,
        theme = theme,
        frozen = record?.frozen == true,
        isToday = isToday,
        isFuture = isFuture,
    )
}

private fun markFor(
    record: YearDayRecord?,
    isToday: Boolean,
    isFuture: Boolean,
): YearCellMark {
    if (isFuture) return YearCellMark.ABSENT
    return when (record?.status) {
        DayStatus.COMPLETED, DayStatus.COMPLETED_NO_PHOTO -> YearCellMark.COMPLETED
        DayStatus.SKIPPED -> YearCellMark.SKIPPED
        DayStatus.MISSED -> YearCellMark.MISSED
        DayStatus.PAUSED -> YearCellMark.ABSENT
        DayStatus.PENDING -> if (isToday) YearCellMark.PENDING_TODAY else YearCellMark.ABSENT
        null -> if (isToday) YearCellMark.PENDING_TODAY else YearCellMark.ABSENT
    }
}

/** Monday = 0 … Sunday = 6. */
internal fun mondayFirstIndex(day: DayOfWeek): Int = (day.value + 6) % 7
