package app.shutterup.domain.calendar

import app.shutterup.domain.model.DayStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YearGridTest {

    private val today = LocalDate.of(2026, 9, 19)

    @Test
    fun emptyYear_hasEveryDayAsAbsentHairline() {
        val grid = yearGrid(2026, emptyList(), today)
        assertEquals(365, grid.cells.size)
        assertEquals(12, grid.months.size)
        grid.months.forEachIndexed { index, band ->
            assertEquals(Month.of(index + 1), band.month)
            assertEquals(Month.of(index + 1).length(false), band.cells.size)
        }
        val past = grid.cells.filter { !it.isToday && !it.isFuture }
        assertTrue(past.isNotEmpty())
        past.forEach { cell ->
            assertEquals(YearCellMark.ABSENT, cell.mark)
            assertNull(cell.status)
            assertNull(cell.theme)
        }
        val sep19 = grid.cells.single { it.date == today }
        assertEquals(YearCellMark.PENDING_TODAY, sep19.mark)
        assertTrue(sep19.isToday)
        grid.cells.filter { it.isFuture }.forEach { cell ->
            assertEquals(YearCellMark.ABSENT, cell.mark)
        }
    }

    @Test
    fun sparseYear_keepsEveryCellAndTintsOnlyCompletedDays() {
        val days = listOf(
            rec(LocalDate.of(2026, 1, 3), DayStatus.COMPLETED, "Reflections"),
            rec(LocalDate.of(2026, 3, 14), DayStatus.COMPLETED, "Quiet hours"),
            rec(LocalDate.of(2026, 6, 2), DayStatus.COMPLETED_NO_PHOTO, "Kitchen still life"),
            rec(LocalDate.of(2026, 8, 20), DayStatus.SKIPPED, "Negative space"),
            rec(LocalDate.of(2026, 9, 17), DayStatus.MISSED, "Golden hour"),
        )
        val grid = yearGrid(2026, days, today)
        assertEquals(365, grid.cells.size)
        val completed = grid.cells.filter { it.mark == YearCellMark.COMPLETED }
        assertEquals(3, completed.size)
        assertEquals("Reflections", cell(grid, 2026, 1, 3).theme)
        assertEquals("Quiet hours", cell(grid, 2026, 3, 14).theme)
        assertEquals("Kitchen still life", cell(grid, 2026, 6, 2).theme)
        assertEquals(YearCellMark.SKIPPED, cell(grid, 2026, 8, 20).mark)
        assertNull(cell(grid, 2026, 8, 20).theme)
        assertEquals(YearCellMark.MISSED, cell(grid, 2026, 9, 17).mark)
        assertEquals(YearCellMark.ABSENT, cell(grid, 2026, 2, 1).mark)
        assertEquals(YearProgress(completed = 3, eligible = 262), grid.progress)
    }

    @Test
    fun leapYear_includesFebruary29AndHas366Cells() {
        val leapToday = LocalDate.of(2024, 12, 31)
        val days = listOf(
            rec(LocalDate.of(2024, 2, 29), DayStatus.COMPLETED, "Thresholds"),
        )
        val grid = yearGrid(2024, days, leapToday)
        assertEquals(366, grid.cells.size)
        assertEquals(366, yearLength(2024))
        val feb = grid.months[1]
        assertEquals(29, feb.cells.size)
        val leap = cell(grid, 2024, 2, 29)
        assertEquals(YearCellMark.COMPLETED, leap.mark)
        assertEquals(1, leap.monthIndex)
        assertEquals(28, leap.dayColumn)
        assertEquals("Thresholds", leap.theme)
        assertFalse(grid.cells.any { it.date == LocalDate.of(2023, 2, 29) })
    }

    @Test
    fun nonLeapYear_omitsFebruary29() {
        val grid = yearGrid(2026, emptyList(), today)
        assertEquals(28, grid.months[1].cells.size)
        assertFalse(grid.cells.any { it.date.month == Month.FEBRUARY && it.date.dayOfMonth == 29 })
    }

    @Test
    fun yearBoundaries_doNotLeakAdjacentYears() {
        val days = listOf(
            rec(LocalDate.of(2025, 12, 31), DayStatus.COMPLETED, "Blue hour"),
            rec(LocalDate.of(2026, 1, 1), DayStatus.COMPLETED, "Dawn"),
            rec(LocalDate.of(2026, 12, 31), DayStatus.MISSED, "Night walk"),
            rec(LocalDate.of(2027, 1, 1), DayStatus.COMPLETED, "Windows"),
        )
        val grid = yearGrid(2026, days, LocalDate.of(2027, 1, 15))
        assertEquals(365, grid.cells.size)
        assertTrue(grid.cells.all { it.date.year == 2026 })
        val jan1 = cell(grid, 2026, 1, 1)
        assertEquals(YearCellMark.COMPLETED, jan1.mark)
        assertEquals(0, jan1.monthIndex)
        assertEquals(0, jan1.dayColumn)
        val dec31 = cell(grid, 2026, 12, 31)
        assertEquals(YearCellMark.MISSED, dec31.mark)
        assertEquals(11, dec31.monthIndex)
        assertEquals(30, dec31.dayColumn)
        assertFalse(grid.cells.any { it.date == LocalDate.of(2025, 12, 31) })
        assertFalse(grid.cells.any { it.date == LocalDate.of(2027, 1, 1) })
    }

    @Test
    fun pausedDays_readAsAbsentAndAreExcludedFromEligible() {
        val days = listOf(
            rec(LocalDate.of(2026, 9, 10), DayStatus.COMPLETED, "Texture"),
            rec(LocalDate.of(2026, 9, 11), DayStatus.PAUSED, "Shadow play"),
            rec(LocalDate.of(2026, 9, 12), DayStatus.PAUSED, "Found geometry"),
            rec(LocalDate.of(2026, 9, 13), DayStatus.MISSED, "Hands"),
        )
        val grid = yearGrid(2026, days, today)
        val paused = cell(grid, 2026, 9, 11)
        assertEquals(YearCellMark.ABSENT, paused.mark)
        assertEquals(DayStatus.PAUSED, paused.status)
        assertNull(paused.theme)
        assertEquals(YearCellMark.ABSENT, cell(grid, 2026, 9, 12).mark)
        assertEquals(YearCellMark.MISSED, cell(grid, 2026, 9, 13).mark)
        // 1 Jan … 19 Sep = 262 elapsed; two paused → 260 eligible.
        assertEquals(YearProgress(completed = 1, eligible = 260), grid.progress)
    }

    @Test
    fun frozenSkippedAndMissed_keepShapeAndFrozenFlag() {
        val days = listOf(
            rec(LocalDate.of(2026, 9, 13), DayStatus.SKIPPED, "Weathered", frozen = true),
            rec(LocalDate.of(2026, 9, 17), DayStatus.MISSED, "Green", frozen = true),
        )
        val grid = yearGrid(2026, days, today)
        val skipped = cell(grid, 2026, 9, 13)
        assertEquals(YearCellMark.SKIPPED, skipped.mark)
        assertTrue(skipped.frozen)
        val missed = cell(grid, 2026, 9, 17)
        assertEquals(YearCellMark.MISSED, missed.mark)
        assertTrue(missed.frozen)
    }

    @Test
    fun layoutIndexes_mondayFirstWeekAndMonthColumns() {
        val grid = yearGrid(2026, emptyList(), today)
        val jan1 = cell(grid, 2026, 1, 1)
        assertEquals(DayOfWeek.THURSDAY, jan1.date.dayOfWeek)
        assertEquals(3, jan1.weekdayColumn)
        assertEquals(0, jan1.weekColumn)
        val jan5 = cell(grid, 2026, 1, 5)
        assertEquals(DayOfWeek.MONDAY, jan5.date.dayOfWeek)
        assertEquals(0, jan5.weekdayColumn)
        assertEquals(1, jan5.weekColumn)
        val sep19 = cell(grid, 2026, 9, 19)
        assertEquals(8, sep19.monthIndex)
        assertEquals(18, sep19.dayColumn)
        assertEquals(5, sep19.weekdayColumn) // Saturday
        grid.months.forEach { band ->
            band.cells.forEachIndexed { index, cell ->
                assertEquals(index, cell.dayColumn)
                assertEquals(band.month.value - 1, cell.monthIndex)
            }
        }
    }

    @Test
    fun futureYear_hasZeroProgressAndAllAbsent() {
        val grid = yearGrid(2027, emptyList(), today)
        assertEquals(365, grid.cells.size)
        assertTrue(grid.cells.all { it.mark == YearCellMark.ABSENT })
        assertEquals(YearProgress(0, 0), grid.progress)
    }

    @Test
    fun pastYear_progressUsesEveryDay() {
        val days = listOf(
            rec(LocalDate.of(2025, 6, 1), DayStatus.COMPLETED, "Water"),
            rec(LocalDate.of(2025, 6, 2), DayStatus.PAUSED, "Lines"),
        )
        val grid = yearGrid(2025, days, today)
        assertEquals(365, grid.cells.size)
        assertEquals(YearProgress(completed = 1, eligible = 364), grid.progress)
    }

    @Test
    fun todayCompleted_isTintedNotPendingRing() {
        val days = listOf(
            rec(today, DayStatus.COMPLETED, "Soft focus"),
        )
        val grid = yearGrid(2026, days, today)
        val cell = cell(grid, 2026, 9, 19)
        assertEquals(YearCellMark.COMPLETED, cell.mark)
        assertEquals("Soft focus", cell.theme)
        assertTrue(cell.isToday)
    }

    @Test
    fun mondayFirstIndex_coversTheWeek() {
        assertEquals(0, mondayFirstIndex(DayOfWeek.MONDAY))
        assertEquals(6, mondayFirstIndex(DayOfWeek.SUNDAY))
        assertEquals(3, mondayFirstIndex(DayOfWeek.THURSDAY))
    }

    private fun rec(
        date: LocalDate,
        status: DayStatus,
        theme: String,
        frozen: Boolean = false,
    ) = YearDayRecord(date = date, status = status, theme = theme, frozen = frozen)

    private fun cell(grid: YearGrid, year: Int, month: Int, day: Int): YearCell =
        grid.cells.single { it.date == LocalDate.of(year, month, day) }
}
