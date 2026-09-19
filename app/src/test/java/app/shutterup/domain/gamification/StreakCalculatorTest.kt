package app.shutterup.domain.gamification

import app.shutterup.domain.model.DayStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    private val today = LocalDate.of(2024, 6, 15)

    @Test
    fun emptyHistory_isZero() {
        assertEquals(0, StreakCalculator.currentStreak(emptyList(), today))
        assertEquals(0, StreakCalculator.longestStreak(emptyList()))
    }

    @Test
    fun currentStreak_countsTodayWhenCompleted() {
        val days = listOf(
            rec(today.minusDays(2), DayStatus.COMPLETED),
            rec(today.minusDays(1), DayStatus.COMPLETED),
            rec(today, DayStatus.COMPLETED),
        )
        assertEquals(3, StreakCalculator.currentStreak(days, today))
    }

    @Test
    fun currentStreak_startsFromYesterdayWhenTodayPending() {
        val days = listOf(
            rec(today.minusDays(2), DayStatus.COMPLETED),
            rec(today.minusDays(1), DayStatus.COMPLETED),
            rec(today, DayStatus.PENDING),
        )
        assertEquals(2, StreakCalculator.currentStreak(days, today))
    }

    @Test
    fun currentStreak_startsFromYesterdayWhenTodayHasNoRecord() {
        val days = listOf(
            rec(today.minusDays(2), DayStatus.COMPLETED),
            rec(today.minusDays(1), DayStatus.COMPLETED),
        )
        assertEquals(2, StreakCalculator.currentStreak(days, today))
    }

    @Test
    fun currentStreak_skipsPausedDays() {
        val days = listOf(
            rec(today.minusDays(3), DayStatus.COMPLETED),
            rec(today.minusDays(2), DayStatus.PAUSED),
            rec(today.minusDays(1), DayStatus.COMPLETED),
            rec(today, DayStatus.COMPLETED),
        )
        assertEquals(3, StreakCalculator.currentStreak(days, today))
    }

    @Test
    fun currentStreak_frozenMissedOrSkippedDoNotBreak() {
        val days = listOf(
            rec(today.minusDays(4), DayStatus.COMPLETED),
            rec(today.minusDays(3), DayStatus.MISSED, frozen = true),
            rec(today.minusDays(2), DayStatus.SKIPPED, frozen = true),
            rec(today.minusDays(1), DayStatus.COMPLETED_NO_PHOTO),
            rec(today, DayStatus.COMPLETED),
        )
        assertEquals(3, StreakCalculator.currentStreak(days, today))
    }

    @Test
    fun currentStreak_plainMissedBreaks() {
        val days = listOf(
            rec(today.minusDays(3), DayStatus.COMPLETED),
            rec(today.minusDays(2), DayStatus.MISSED, frozen = false),
            rec(today.minusDays(1), DayStatus.COMPLETED),
            rec(today, DayStatus.COMPLETED),
        )
        assertEquals(2, StreakCalculator.currentStreak(days, today))
    }

    @Test
    fun currentStreak_plainSkippedBreaks() {
        val days = listOf(
            rec(today.minusDays(2), DayStatus.COMPLETED),
            rec(today.minusDays(1), DayStatus.SKIPPED, frozen = false),
            rec(today, DayStatus.COMPLETED),
        )
        assertEquals(1, StreakCalculator.currentStreak(days, today))
    }

    @Test
    fun longestStreak_calendarGapBreaksRun() {
        val days = listOf(
            rec(LocalDate.of(2024, 1, 1), DayStatus.COMPLETED),
            rec(LocalDate.of(2024, 1, 2), DayStatus.COMPLETED),
            rec(LocalDate.of(2024, 1, 5), DayStatus.COMPLETED),
            rec(LocalDate.of(2024, 1, 6), DayStatus.COMPLETED),
            rec(LocalDate.of(2024, 1, 7), DayStatus.COMPLETED),
        )
        assertEquals(3, StreakCalculator.longestStreak(days))
    }

    @Test
    fun longestStreak_usesSameSkipAndBreakRules() {
        val days = listOf(
            rec(LocalDate.of(2024, 2, 1), DayStatus.COMPLETED),
            rec(LocalDate.of(2024, 2, 2), DayStatus.PAUSED),
            rec(LocalDate.of(2024, 2, 3), DayStatus.MISSED, frozen = true),
            rec(LocalDate.of(2024, 2, 4), DayStatus.COMPLETED),
            rec(LocalDate.of(2024, 2, 5), DayStatus.MISSED, frozen = false),
            rec(LocalDate.of(2024, 2, 6), DayStatus.COMPLETED),
            rec(LocalDate.of(2024, 2, 7), DayStatus.COMPLETED),
        )
        assertEquals(2, StreakCalculator.longestStreak(days))
    }

    private fun rec(
        date: LocalDate,
        status: DayStatus,
        frozen: Boolean = false,
    ): DayRecord = DayRecord(date, status, frozen)
}
