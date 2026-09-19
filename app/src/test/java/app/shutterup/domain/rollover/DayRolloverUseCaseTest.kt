package app.shutterup.domain.rollover

import app.shutterup.domain.model.Achievement
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.LibraryUsage
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.model.SupersededPrompt
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.GamificationRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayRolloverUseCaseTest {

    private val today = LocalDate.of(2026, 9, 19)
    private val clock = Clock.fixed(Instant.parse("2026-09-19T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun threeDayGap_consumesTwoFreezesInOrder() = runBlocking {
        val prompts = FakeDayPrompts()
        val gamification = FakeGamification(
            StreakState(current = 0, longest = 0, freezes = 2, lastProcessedDate = today.minusDays(4)),
        )
        val useCase = DayRolloverUseCase(prompts, gamification, clock)

        val result = useCase.rollover(today, paused = false)

        val d1 = today.minusDays(3)
        val d2 = today.minusDays(2)
        val d3 = today.minusDays(1)
        assertEquals(listOf(d1, d2, d3, today), result.processed)
        assertEquals(listOf(d1, d2, d3), result.missed)
        assertEquals(2, result.freezesConsumed)
        assertTrue(prompts.getDay(d1)!!.frozen)
        assertTrue(prompts.getDay(d2)!!.frozen)
        assertFalse(prompts.getDay(d3)!!.frozen)
        assertEquals(DayStatus.MISSED, prompts.getDay(d1)!!.status)
        assertEquals(DayStatus.MISSED, prompts.getDay(d3)!!.status)
        assertEquals("Missed day", prompts.getDay(d1)!!.title)
        assertEquals("No photo was taken.", prompts.getDay(d1)!!.oneLiner)
        assertEquals("This day passed without a completed prompt.", prompts.getDay(d1)!!.details)
        assertEquals("Missed", prompts.getDay(d1)!!.theme)
        assertEquals(PromptSourceRef.LIBRARY, prompts.getDay(d1)!!.source)
        assertNull(prompts.getDay(today))
        assertEquals(0, gamification.state.freezes)
        assertEquals(today, gamification.state.lastProcessedDate)
    }

    @Test
    fun noFreezes_plainMissed() = runBlocking {
        val prompts = FakeDayPrompts()
        val yesterday = today.minusDays(1)
        val gamification = FakeGamification(
            StreakState(current = 0, longest = 0, freezes = 0, lastProcessedDate = today.minusDays(2)),
        )
        val result = DayRolloverUseCase(prompts, gamification, clock).rollover(today, paused = false)

        assertEquals(listOf(yesterday), result.missed)
        assertEquals(0, result.freezesConsumed)
        val missed = prompts.getDay(yesterday)!!
        assertEquals(DayStatus.MISSED, missed.status)
        assertFalse(missed.frozen)
        assertEquals(0, gamification.state.freezes)
    }

    @Test
    fun pausedDays_createPausedRows_neverConsumeFreezes() = runBlocking {
        val prompts = FakeDayPrompts()
        val pending = prompt(today.minusDays(1), DayStatus.PENDING, title = "Keep Me")
        prompts.upsert(pending)
        val gamification = FakeGamification(
            StreakState(current = 3, longest = 3, freezes = 2, lastProcessedDate = today.minusDays(3)),
        )
        val result = DayRolloverUseCase(prompts, gamification, clock).rollover(today, paused = true)

        assertTrue(result.missed.isEmpty())
        assertEquals(0, result.freezesConsumed)
        assertEquals(2, gamification.state.freezes)

        val created = prompts.getDay(today.minusDays(2))!!
        assertEquals(DayStatus.PAUSED, created.status)
        assertEquals("Paused", created.title)
        assertEquals("No prompt today.", created.oneLiner)
        assertEquals(
            "Prompts and notifications are paused. Paused days do not affect the streak.",
            created.details,
        )
        assertEquals("Paused", created.theme)
        assertEquals(emptyList<String>(), created.tips)
        assertEquals(PromptSourceRef.LIBRARY, created.source)
        assertNull(created.libraryId)
        assertNull(created.modelName)
        assertFalse(created.rerollUsed)

        val flipped = prompts.getDay(today.minusDays(1))!!
        assertEquals(DayStatus.PAUSED, flipped.status)
        assertEquals("Keep Me", flipped.title)
        assertEquals(DayStatus.PAUSED, prompts.getDay(today)!!.status)
    }

    @Test
    fun completedRows_untouched() = runBlocking {
        val completed = prompt(today.minusDays(1), DayStatus.COMPLETED, title = "Done")
        val noPhoto = prompt(today.minusDays(2), DayStatus.COMPLETED_NO_PHOTO, title = "No Photo")
        val skipped = prompt(today.minusDays(3), DayStatus.SKIPPED, title = "Skip")
        val prompts = FakeDayPrompts().apply {
            upsert(completed)
            upsert(noPhoto)
            upsert(skipped)
        }
        val gamification = FakeGamification(
            StreakState(current = 1, longest = 1, freezes = 1, lastProcessedDate = today.minusDays(4)),
        )
        DayRolloverUseCase(prompts, gamification, clock).rollover(today, paused = false)
        DayRolloverUseCase(prompts, gamification, clock).rollover(today, paused = true)

        assertEquals(completed, prompts.getDay(completed.date))
        assertEquals(noPhoto, prompts.getDay(noPhoto.date))
        assertEquals(skipped, prompts.getDay(skipped.date))
    }

    @Test
    fun doubleRun_isIdempotent() = runBlocking {
        val prompts = FakeDayPrompts()
        val gamification = FakeGamification(
            StreakState(current = 0, longest = 0, freezes = 1, lastProcessedDate = today.minusDays(2)),
        )
        val useCase = DayRolloverUseCase(prompts, gamification, clock)
        val first = useCase.rollover(today, paused = false)
        val snapshot = prompts.getDay(today.minusDays(1))!!.copy()
        val streakAfterFirst = gamification.state
        val second = useCase.rollover(today, paused = false)

        assertTrue(first.processed.isNotEmpty())
        assertTrue(second.processed.isEmpty())
        assertTrue(second.missed.isEmpty())
        assertEquals(0, second.freezesConsumed)
        assertEquals(0, second.freezesEarned)
        assertEquals(snapshot, prompts.getDay(today.minusDays(1)))
        assertEquals(streakAfterFirst, gamification.state)
    }

    @Test
    fun firstRun_processesOnlyToday() = runBlocking {
        val prompts = FakeDayPrompts()
        val gamification = FakeGamification(
            StreakState(current = 0, longest = 0, freezes = 0, lastProcessedDate = null),
        )
        val result = DayRolloverUseCase(prompts, gamification, clock).rollover(today, paused = false)

        assertEquals(listOf(today), result.processed)
        assertTrue(result.missed.isEmpty())
        assertNull(prompts.getDay(today))
        assertNull(prompts.getDay(today.minusDays(1)))
        assertEquals(today, gamification.state.lastProcessedDate)
    }

    @Test
    fun freezeEarn_whenStreakCrossesSeven() = runBlocking {
        val prompts = FakeDayPrompts()
        for (i in 6 downTo 1) {
            prompts.upsert(prompt(today.minusDays(i.toLong()), DayStatus.COMPLETED))
        }
        prompts.upsert(prompt(today, DayStatus.COMPLETED))
        val gamification = FakeGamification(
            StreakState(current = 6, longest = 6, freezes = 0, lastProcessedDate = today.minusDays(1)),
        )
        val result = DayRolloverUseCase(prompts, gamification, clock).rollover(today, paused = false)

        assertEquals(1, result.freezesEarned)
        assertEquals(1, gamification.state.freezes)
        assertEquals(7, gamification.state.current)
        assertEquals(7, gamification.state.longest)
    }

    @Test
    fun freezeEarn_capsAtTwo() = runBlocking {
        val prompts = FakeDayPrompts()
        for (i in 6 downTo 1) {
            prompts.upsert(prompt(today.minusDays(i.toLong()), DayStatus.COMPLETED))
        }
        prompts.upsert(prompt(today, DayStatus.COMPLETED))
        val gamification = FakeGamification(
            StreakState(current = 6, longest = 6, freezes = 2, lastProcessedDate = today.minusDays(1)),
        )
        val result = DayRolloverUseCase(prompts, gamification, clock).rollover(today, paused = false)

        assertEquals(0, result.freezesEarned)
        assertEquals(2, gamification.state.freezes)
        assertEquals(7, gamification.state.current)
    }

    private fun prompt(
        date: LocalDate,
        status: DayStatus,
        title: String = "Prompt $date",
        frozen: Boolean = false,
    ): DayPrompt = DayPrompt(
        date = date,
        title = title,
        oneLiner = "One liner",
        details = "Details",
        constraint = null,
        theme = "Theme",
        tips = listOf("Tip"),
        source = PromptSourceRef.ON_DEVICE_AI,
        libraryId = null,
        modelName = "nano",
        generatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        status = status,
        frozen = frozen,
        rerollUsed = false,
    )
}

private class FakeDayPrompts : DayPromptRepository {
    private val days = linkedMapOf<LocalDate, DayPrompt>()

    override fun observeDay(date: LocalDate): Flow<DayPrompt?> = flowOf(days[date])

    override fun observeDays(start: LocalDate, endInclusive: LocalDate): Flow<List<DayPrompt>> =
        flowOf(days.values.filter { it.date >= start && it.date <= endInclusive })

    override suspend fun getDay(date: LocalDate): DayPrompt? = days[date]

    override suspend fun upsert(prompt: DayPrompt) {
        days[prompt.date] = prompt
    }

    override suspend fun recordSuperseded(prompt: SupersededPrompt) = Unit

    override suspend fun recentTitles(limit: Int): List<String> = recentDays(limit).map { it.title }

    override suspend fun recentThemes(limit: Int): List<String> = recentDays(limit).map { it.theme }

    override suspend fun recentDays(limit: Int): List<DayPrompt> =
        days.values.sortedByDescending { it.date }.take(limit)
}

private class FakeGamification(
    initial: StreakState,
) : GamificationRepository {
    private val streak = MutableStateFlow(initial)
    val state: StreakState get() = streak.value

    override fun observeAchievements(): Flow<List<Achievement>> = flowOf(emptyList())

    override suspend fun unlock(achievement: Achievement) = Unit

    override fun observeStreak(): Flow<StreakState> = streak

    override suspend fun updateStreak(state: StreakState) {
        streak.value = state
    }

    override suspend fun recordLibraryUsage(usage: LibraryUsage) = Unit

    override suspend fun libraryUsedSince(libraryId: String, since: LocalDate): Boolean = false
}
