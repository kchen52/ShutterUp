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
}
