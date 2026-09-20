package app.shutterup.domain.monthly

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyIssueCalendarTest {

    @Test
    fun window_usesInclusiveCalendarMonth() {
        val (start, end) = MonthlyIssueCalendar.window(java.time.YearMonth.of(2026, 9))
        assertEquals(LocalDate.of(2026, 9, 1), start)
        assertEquals(LocalDate.of(2026, 9, 30), end)
    }

    @Test
    fun window_februaryLeapYear() {
        val (start, end) = MonthlyIssueCalendar.window(java.time.YearMonth.of(2024, 2))
        assertEquals(LocalDate.of(2024, 2, 1), start)
        assertEquals(LocalDate.of(2024, 2, 29), end)
    }

    @Test
    fun dueMonths_emptyWhenNoHistory() {
        assertEquals(emptyList<java.time.YearMonth>(), MonthlyIssueCalendar.dueMonths(LocalDate.of(2026, 9, 15), null))
    }

    @Test
    fun dueMonths_midMonthInstall_doesNotIssueCurrentMonth() {
        val today = LocalDate.of(2026, 9, 15)
        val earliest = LocalDate.of(2026, 9, 3)
        assertEquals(emptyList<java.time.YearMonth>(), MonthlyIssueCalendar.dueMonths(today, earliest))
    }

    @Test
    fun dueMonths_onFirst_issuesPreviousMonth() {
        val today = LocalDate.of(2026, 10, 1)
        val earliest = LocalDate.of(2026, 9, 4)
        assertEquals(
            listOf(java.time.YearMonth.of(2026, 9)),
            MonthlyIssueCalendar.dueMonths(today, earliest),
        )
    }

    @Test
    fun dueMonths_deviceOffAcrossBoundary_catchesUpFinishedMonths() {
        val today = LocalDate.of(2026, 11, 5)
        val earliest = LocalDate.of(2026, 8, 20)
        assertEquals(
            listOf(
                java.time.YearMonth.of(2026, 8),
                java.time.YearMonth.of(2026, 9),
                java.time.YearMonth.of(2026, 10),
            ),
            MonthlyIssueCalendar.dueMonths(today, earliest),
        )
    }

    @Test
    fun dueMonths_yearBoundary_decemberBecomesDueOnJanuaryFirst() {
        val today = LocalDate.of(2027, 1, 1)
        val earliest = LocalDate.of(2026, 12, 2)
        assertEquals(
            listOf(java.time.YearMonth.of(2026, 12)),
            MonthlyIssueCalendar.dueMonths(today, earliest),
        )
    }

    @Test
    fun dueMonths_yearBoundary_decemberNotDueOnDecemberThirtyFirst() {
        val today = LocalDate.of(2026, 12, 31)
        val earliest = LocalDate.of(2026, 12, 1)
        assertEquals(emptyList<java.time.YearMonth>(), MonthlyIssueCalendar.dueMonths(today, earliest))
        assertEquals(
            listOf(java.time.YearMonth.of(2026, 11)),
            MonthlyIssueCalendar.dueMonths(today, LocalDate.of(2026, 11, 4)),
        )
    }

    @Test
    fun timezone_tokyoFirstIsStillSeptemberInNewYork() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val ny = ZoneId.of("America/New_York")
        val instant = LocalDate.of(2026, 10, 1).atTime(0, 30).atZone(tokyo).toInstant()
        val todayTokyo = instant.atZone(tokyo).toLocalDate()
        val todayNy = instant.atZone(ny).toLocalDate()
        val earliest = LocalDate.of(2026, 9, 10)
        assertEquals(LocalDate.of(2026, 10, 1), todayTokyo)
        assertEquals(LocalDate.of(2026, 9, 30), todayNy)
        assertEquals(
            listOf(java.time.YearMonth.of(2026, 9)),
            MonthlyIssueCalendar.dueMonths(todayTokyo, earliest),
        )
        assertEquals(emptyList<java.time.YearMonth>(), MonthlyIssueCalendar.dueMonths(todayNy, earliest))
    }

    @Test
    fun isFinished_previousMonthOnly() {
        val today = LocalDate.of(2026, 10, 1)
        assertTrue(MonthlyIssueCalendar.isFinished(java.time.YearMonth.of(2026, 9), today))
        assertFalse(MonthlyIssueCalendar.isFinished(java.time.YearMonth.of(2026, 10), today))
        assertTrue(MonthlyIssueCalendar.isFinished(java.time.YearMonth.of(2026, 8), today))
    }

    @Test
    fun longestRun_empty_isZero() {
        assertEquals(0, MonthlyIssueCalendar.longestRun(emptyList()))
    }

    @Test
    fun longestRun_isolatedDays_isOne() {
        val dates = listOf(
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 8),
            LocalDate.of(2026, 9, 20),
        )
        assertEquals(1, MonthlyIssueCalendar.longestRun(dates))
    }

    @Test
    fun longestRun_picksTheLongestConsecutiveStretch() {
        val dates = listOf(
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 2),
            LocalDate.of(2026, 9, 4),
            LocalDate.of(2026, 9, 5),
            LocalDate.of(2026, 9, 6),
            LocalDate.of(2026, 9, 7),
            LocalDate.of(2026, 9, 10),
        )
        assertEquals(4, MonthlyIssueCalendar.longestRun(dates))
    }

    @Test
    fun longestRun_fullMonth() {
        val dates = (1..30).map { LocalDate.of(2026, 9, it) }
        assertEquals(30, MonthlyIssueCalendar.longestRun(dates))
    }

    @Test
    fun contains_matchesYearMonth() {
        assertTrue(MonthlyIssueCalendar.contains(java.time.YearMonth.of(2026, 9), LocalDate.of(2026, 9, 30)))
        assertFalse(MonthlyIssueCalendar.contains(java.time.YearMonth.of(2026, 9), LocalDate.of(2026, 10, 1)))
    }
}
