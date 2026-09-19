package app.shutterup.ui.calendar

import app.shutterup.domain.calendar.YearDayRecord
import app.shutterup.domain.calendar.YearGrid
import app.shutterup.domain.calendar.yearGrid
import app.shutterup.domain.model.DayStatus
import java.time.LocalDate

/** Themes used so a full year paints many hues, not one wash. */
internal val SAMPLE_YEAR_THEMES = listOf(
    "Reflections",
    "Quiet hours",
    "Kitchen still life",
    "Negative space",
    "Golden hour",
    "Thresholds",
    "Blue hour",
    "Texture",
    "Shadow play",
    "Found geometry",
    "Windows",
    "Hands",
    "Weathered",
    "Green",
    "Street light",
    "Water",
    "Lines",
    "Soft focus",
    "Night walk",
    "Dawn",
)

fun sampleSparseYearRecords(
    year: Int = 2026,
    today: LocalDate = LocalDate.of(2026, 9, 19),
): List<YearDayRecord> = listOf(
    rec(year, 1, 3, DayStatus.COMPLETED, "Reflections"),
    rec(year, 2, 14, DayStatus.COMPLETED, "Quiet hours"),
    rec(year, 3, 21, DayStatus.COMPLETED, "Kitchen still life"),
    rec(year, 4, 8, DayStatus.COMPLETED, "Negative space"),
    rec(year, 5, 1, DayStatus.SKIPPED, "Golden hour", frozen = true),
    rec(year, 6, 20, DayStatus.COMPLETED, "Thresholds"),
    rec(year, 7, 4, DayStatus.MISSED, "Blue hour"),
    rec(year, 7, 22, DayStatus.COMPLETED, "Texture"),
    rec(year, 8, 15, DayStatus.PAUSED, "Shadow play"),
    rec(year, 8, 16, DayStatus.PAUSED, "Found geometry"),
    rec(year, 8, 30, DayStatus.COMPLETED, "Windows"),
    rec(year, 9, 2, DayStatus.COMPLETED, "Hands"),
    rec(year, 9, 13, DayStatus.SKIPPED, "Weathered", frozen = true),
    rec(year, 9, 17, DayStatus.MISSED, "Green"),
    rec(year, 9, 18, DayStatus.COMPLETED, "Street light"),
    rec(year, today.monthValue, today.dayOfMonth, DayStatus.PENDING, "Water"),
)

fun sampleFullYearRecords(
    year: Int = 2026,
    today: LocalDate = LocalDate.of(2026, 9, 19),
): List<YearDayRecord> {
    val start = LocalDate.of(year, 1, 1)
    val records = mutableListOf<YearDayRecord>()
    var date = start
    var themeIndex = 0
    while (!date.isAfter(today)) {
        val paused = date.monthValue == 8 && date.dayOfMonth in 10..12
        val status = when {
            paused -> DayStatus.PAUSED
            date.dayOfMonth == 13 && date.dayOfWeek.value >= 6 -> DayStatus.SKIPPED
            date.dayOfYear % 19 == 0 -> DayStatus.MISSED
            date.dayOfYear % 29 == 0 -> DayStatus.SKIPPED
            date == today -> DayStatus.PENDING
            else -> DayStatus.COMPLETED
        }
        val theme = SAMPLE_YEAR_THEMES[themeIndex % SAMPLE_YEAR_THEMES.size]
        if (status == DayStatus.COMPLETED) themeIndex++
        records += YearDayRecord(
            date = date,
            status = status,
            theme = theme,
            frozen = status == DayStatus.SKIPPED && date.dayOfMonth == 13,
        )
        date = date.plusDays(1)
    }
    return records
}

fun sampleYearGrid(
    year: Int = 2026,
    today: LocalDate = LocalDate.of(2026, 9, 19),
    full: Boolean = false,
): YearGrid = yearGrid(
    year = year,
    days = if (full) sampleFullYearRecords(year, today) else sampleSparseYearRecords(year, today),
    today = today,
)

private fun rec(
    year: Int,
    month: Int,
    day: Int,
    status: DayStatus,
    theme: String,
    frozen: Boolean = false,
) = YearDayRecord(
    date = LocalDate.of(year, month, day),
    status = status,
    theme = theme,
    frozen = frozen,
)
