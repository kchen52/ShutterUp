package app.shutterup.ui.calendar

import app.shutterup.domain.model.DayStatus
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarGridTest {

    @Test
    fun september2026StartsMondayAndIncludesTodayRingCandidate() {
        val today = LocalDate.of(2026, 9, 19)
        val days = sampleMonthPrompts(today).associateBy { it.date }
        val cells = monthCells(YearMonth.of(2026, 9), days, emptyMap(), today)
        assertEquals(35, cells.size)
        assertEquals(LocalDate.of(2026, 8, 31), cells.first().date)
        val nineteen = cells.first { it.date.dayOfMonth == 19 && it.inMonth }
        assertTrue(nineteen.isToday)
        assertEquals(DayStatus.PENDING, nineteen.status)
        assertEquals("19 September, pending", calendarCellDescription(nineteen))
    }

    @Test
    fun missedFrozenDescription() {
        val cell = CalendarCell(
            date = LocalDate.of(2026, 9, 17),
            inMonth = true,
            status = DayStatus.MISSED,
            frozen = true,
            thumbPath = null,
            isToday = false,
            isFuture = false,
        )
        assertEquals("17 September, missed, streak frozen", calendarCellDescription(cell))
    }

    @Test
    fun yearCellDescription_matchesMonthVocabulary() {
        val today = LocalDate.of(2026, 9, 19)
        val grid = sampleYearGrid(year = 2026, today = today, full = false)
        val completed = grid.cells.single { it.date == LocalDate.of(2026, 1, 3) }
        assertEquals("3 January, completed", yearCellDescription(completed))
        val paused = grid.cells.single { it.date == LocalDate.of(2026, 8, 15) }
        assertEquals("15 August, paused", yearCellDescription(paused))
        val missedFrozen = grid.cells.single { it.date == LocalDate.of(2026, 9, 17) }
        assertEquals("17 September, missed", yearCellDescription(missedFrozen))
        val skippedFrozen = grid.cells.single { it.date == LocalDate.of(2026, 5, 1) }
        assertEquals("1 May, skipped, streak frozen", yearCellDescription(skippedFrozen))
        val pending = grid.cells.single { it.date == today }
        assertEquals("19 September, pending", yearCellDescription(pending))
        val future = grid.cells.single { it.date == LocalDate.of(2026, 12, 1) }
        assertEquals("1 December", yearCellDescription(future))
        val empty = grid.cells.single { it.date == LocalDate.of(2026, 2, 1) }
        assertEquals("1 February", yearCellDescription(empty))
    }
}
