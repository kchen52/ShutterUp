package app.shutterup.domain.gamification

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.PromptSourceRef
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementEvaluatorTest {

    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2024, 6, 15)
    private val notify = LocalTime.of(9, 0)

    @Test
    fun firstLight_unlocksOnFirstCompletedDay() {
        assertFalse("first_light" in evaluate(days = emptyList()))
        assertTrue("first_light" in evaluate(days = listOf(day(today, DayStatus.COMPLETED))))
        assertTrue(
            "first_light" in evaluate(days = listOf(day(today, DayStatus.COMPLETED_NO_PHOTO))),
        )
    }

    @Test
    fun streakTiers_unlockAtThresholds() {
        assertFalse("streak_7" in evaluate(currentStreak = 6, completedToday = true))
        assertTrue("streak_7" in evaluate(currentStreak = 7, completedToday = true))
        assertFalse("streak_30" in evaluate(currentStreak = 29, completedToday = true))
        assertTrue("streak_30" in evaluate(currentStreak = 30, completedToday = true))
        assertFalse("streak_100" in evaluate(currentStreak = 99, completedToday = true))
        assertTrue("streak_100" in evaluate(currentStreak = 100, completedToday = true))
        assertFalse("streak_365" in evaluate(currentStreak = 364, completedToday = true))
        val at365 = evaluate(currentStreak = 365, completedToday = true)
        assertTrue("streak_7" in at365)
        assertTrue("streak_30" in at365)
        assertTrue("streak_100" in at365)
        assertTrue("streak_365" in at365)
    }

    @Test
    fun totalTiers_unlockAtCompletedDayCounts() {
        assertFalse("total_10" in evaluate(days = completedDays(9)))
        assertTrue("total_10" in evaluate(days = completedDays(10)))
        assertFalse("total_50" in evaluate(days = completedDays(49)))
        assertTrue("total_50" in evaluate(days = completedDays(50)))
        assertFalse("total_100" in evaluate(days = completedDays(99)))
        assertTrue("total_100" in evaluate(days = completedDays(100)))
        assertFalse("total_250" in evaluate(days = completedDays(249)))
        assertTrue("total_250" in evaluate(days = completedDays(250)))
        assertFalse("total_500" in evaluate(days = completedDays(499)))
        val at500 = evaluate(days = completedDays(500))
        assertTrue("total_10" in at500)
        assertTrue("total_50" in at500)
        assertTrue("total_100" in at500)
        assertTrue("total_250" in at500)
        assertTrue("total_500" in at500)
    }

    @Test
    fun explorerTiers_countDistinctThemesOnCompletedDays() {
        assertFalse("explorer_10" in evaluate(days = themedCompleted(9)))
        assertTrue("explorer_10" in evaluate(days = themedCompleted(10)))
        assertFalse("explorer_25" in evaluate(days = themedCompleted(24)))
        val at25 = evaluate(days = themedCompleted(25))
        assertTrue("explorer_10" in at25)
        assertTrue("explorer_25" in at25)
        val withMissedExtraTheme = themedCompleted(9) +
            day(today.minusDays(20), DayStatus.MISSED, theme = "unused")
        assertFalse("explorer_10" in evaluate(days = withMissedExtraTheme))
    }

    @Test
    fun earlyBird_windowIncludesStartAndExcludesExactlySixtyMinutes() {
        val included = listOf(
            timedEntry(today.minusDays(4), LocalTime.of(9, 0)),
            timedEntry(today.minusDays(3), LocalTime.of(9, 30)),
            timedEntry(today.minusDays(2), LocalTime.of(9, 59, 59)),
            timedEntry(today.minusDays(1), LocalTime.of(9, 0, 1)),
            timedEntry(today, LocalTime.of(9, 45)),
        )
        assertTrue("early_bird" in evaluate(entries = included))

        val withExactHour = included.dropLast(1) +
            timedEntry(today, LocalTime.of(10, 0))
        assertFalse("early_bird" in evaluate(entries = withExactHour))

        val beforeWindow = List(5) { i ->
            timedEntry(today.minusDays(i.toLong()), LocalTime.of(8, 59, 59))
        }
        assertFalse("early_bird" in evaluate(entries = beforeWindow))
    }

    @Test
    fun nightOwl_includes21AndExcludesBefore() {
        val atBoundary = List(5) { i ->
            timedEntry(today.minusDays(i.toLong()), LocalTime.of(21, 0))
        }
        assertTrue("night_owl" in evaluate(entries = atBoundary))

        val before = List(5) { i ->
            timedEntry(today.minusDays(i.toLong()), LocalTime.of(20, 59, 59))
        }
        assertFalse("night_owl" in evaluate(entries = before))

        val fourAfter = List(4) { i ->
            timedEntry(today.minusDays(i.toLong()), LocalTime.of(22, 15))
        }
        assertFalse("night_owl" in evaluate(entries = fourAfter))
    }

    @Test
    fun perfectMonth_pastMonthRequiresEveryNonPausedDayCompleted() {
        val march = YearMonth.of(2024, 3)
        val perfectPast = monthDays(march) { date ->
            if (date.dayOfMonth == 10) day(date, DayStatus.PAUSED) else day(date, DayStatus.COMPLETED)
        }
        assertTrue("perfect_month" in evaluate(days = perfectPast, today = LocalDate.of(2024, 6, 15)))

        val withMiss = monthDays(march) { date ->
            if (date.dayOfMonth == 4) day(date, DayStatus.MISSED) else day(date, DayStatus.COMPLETED)
        }
        assertFalse("perfect_month" in evaluate(days = withMiss, today = LocalDate.of(2024, 6, 15)))
    }

    @Test
    fun perfectMonth_currentMonthOnlyOnLastDay() {
        val june = YearMonth.of(2024, 6)
        val midMonthToday = LocalDate.of(2024, 6, 15)
        val daysThroughMid = (1..15).map { d ->
            day(june.atDay(d), DayStatus.COMPLETED)
        }
        assertFalse("perfect_month" in evaluate(days = daysThroughMid, today = midMonthToday))

        val lastDay = june.atEndOfMonth()
        val fullJune = monthDays(june) { date -> day(date, DayStatus.COMPLETED) }
        assertTrue("perfect_month" in evaluate(days = fullJune, today = lastDay))

        val juneWithSkip = monthDays(june) { date ->
            if (date.dayOfMonth == 2) day(date, DayStatus.SKIPPED) else day(date, DayStatus.COMPLETED)
        }
        assertFalse("perfect_month" in evaluate(days = juneWithSkip, today = lastDay))
    }

    @Test
    fun comeback_requiresThreeQuietDaysNotTwo() {
        val afterTwo = listOf(
            day(today.minusDays(2), DayStatus.MISSED),
            day(today.minusDays(1), DayStatus.SKIPPED),
            day(today, DayStatus.COMPLETED),
        )
        assertFalse("comeback" in evaluate(days = afterTwo))

        val afterThree = listOf(
            day(today.minusDays(3), DayStatus.MISSED),
            day(today.minusDays(2), DayStatus.SKIPPED),
            day(today.minusDays(1), DayStatus.MISSED, frozen = true),
            day(today, DayStatus.COMPLETED),
        )
        assertTrue("comeback" in evaluate(days = afterThree))
    }

    @Test
    fun iceberg_unlocksWhenAnyDayIsFrozen() {
        assertFalse("iceberg" in evaluate(days = listOf(day(today, DayStatus.COMPLETED))))
        assertTrue(
            "iceberg" in evaluate(
                days = listOf(day(today.minusDays(1), DayStatus.MISSED, frozen = true)),
            ),
        )
    }

    @Test
    fun curator_requiresTwentyFiveNonBlankNotes() {
        val twentyFour = (1..24).map { i ->
            notedEntry(today.minusDays(i.toLong()), "note $i")
        }
        assertFalse("curator" in evaluate(entries = twentyFour))

        val withBlanks = twentyFour +
            notedEntry(today.minusDays(30), null) +
            notedEntry(today.minusDays(31), "") +
            notedEntry(today.minusDays(32), "   ")
        assertFalse("curator" in evaluate(entries = withBlanks))

        val twentyFive = twentyFour + notedEntry(today, "kept")
        assertTrue("curator" in evaluate(entries = twentyFive))
    }

    @Test
    fun alreadyUnlockedIds_areNeverReturned() {
        val days = completedDays(500) + themedCompleted(25) + monthDays(YearMonth.of(2024, 3)) { date ->
            day(date, DayStatus.COMPLETED)
        }
        val entries = List(5) { i -> timedEntry(today.minusDays(i.toLong()), LocalTime.of(9, 5)) } +
            List(5) { i -> timedEntry(today.minusDays(10L + i), LocalTime.of(21, 30)) } +
            List(25) { i -> notedEntry(today.minusDays(40L + i), "n$i") }
        val all = evaluate(
            days = days + listOf(
                day(today.minusDays(3), DayStatus.MISSED),
                day(today.minusDays(2), DayStatus.MISSED),
                day(today.minusDays(1), DayStatus.SKIPPED),
                day(today, DayStatus.COMPLETED, frozen = true),
            ),
            entries = entries,
            currentStreak = 365,
            alreadyUnlocked = emptySet(),
        )
        assertTrue(all.isNotEmpty())
        val again = evaluate(
            days = days,
            entries = entries,
            currentStreak = 365,
            alreadyUnlocked = all,
        )
        assertTrue(again.none { it in all })
        assertEquals(emptySet<String>(), evaluate(days = days, alreadyUnlocked = all))
    }

    @Test
    fun evaluate_isDeterministic() {
        val days = completedDays(12)
        val first = evaluate(days = days, currentStreak = 7)
        val second = evaluate(days = days, currentStreak = 7)
        assertEquals(first, second)
    }

    private fun evaluate(
        days: List<DayPrompt> = emptyList(),
        entries: List<Entry> = emptyList(),
        currentStreak: Int = 0,
        alreadyUnlocked: Set<String> = emptySet(),
        today: LocalDate = this.today,
        completedToday: Boolean = false,
    ): Set<String> {
        val resolvedDays = if (completedToday && days.none { it.date == today }) {
            days + day(today, DayStatus.COMPLETED)
        } else {
            days
        }
        return AchievementEvaluator.evaluate(
            AchievementInput(
                days = resolvedDays,
                entries = entries,
                currentStreak = currentStreak,
                notifyTime = notify,
                zone = zone,
                today = today,
            ),
            alreadyUnlocked,
        )
    }

    private fun completedDays(count: Int): List<DayPrompt> =
        (0 until count).map { i ->
            day(today.minusDays(i.toLong()), DayStatus.COMPLETED)
        }

    private fun themedCompleted(count: Int): List<DayPrompt> =
        (0 until count).map { i ->
            day(today.minusDays(i.toLong()), DayStatus.COMPLETED, theme = "theme-$i")
        }

    private fun monthDays(
        month: YearMonth,
        factory: (LocalDate) -> DayPrompt,
    ): List<DayPrompt> = (1..month.lengthOfMonth()).map { factory(month.atDay(it)) }

    private fun day(
        date: LocalDate,
        status: DayStatus,
        theme: String = "light",
        frozen: Boolean = false,
    ): DayPrompt = DayPrompt(
        date = date,
        title = "title",
        oneLiner = "one",
        details = "details",
        constraint = null,
        theme = theme,
        tips = emptyList(),
        source = PromptSourceRef.LIBRARY,
        libraryId = null,
        modelName = null,
        generatedAt = Instant.EPOCH,
        status = status,
        frozen = frozen,
        rerollUsed = false,
    )

    private fun timedEntry(date: LocalDate, time: LocalTime): Entry = Entry(
        date = date,
        mediaUri = "content://photo/$date",
        thumbPath = "/thumbs/$date",
        capturedAt = LocalDateTime.of(date, time).toInstant(ZoneOffset.UTC),
        width = 100,
        height = 100,
        note = null,
        importedFromGallery = false,
        createdAt = Instant.EPOCH,
    )

    private fun notedEntry(date: LocalDate, note: String?): Entry = Entry(
        date = date,
        mediaUri = "content://photo/$date",
        thumbPath = "/thumbs/$date",
        capturedAt = Instant.EPOCH,
        width = 100,
        height = 100,
        note = note,
        importedFromGallery = false,
        createdAt = Instant.EPOCH,
    )
}
