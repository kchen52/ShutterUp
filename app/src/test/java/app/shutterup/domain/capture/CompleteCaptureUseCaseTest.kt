package app.shutterup.domain.capture

import app.shutterup.domain.model.Achievement
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.LibraryUsage
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.model.SupersededPrompt
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompleteCaptureUseCaseTest {
    private val today = LocalDate.of(2026, 9, 19)
    private val zone = ZoneOffset.UTC
    private val clock = Clock.fixed(today.atTime(12, 0).toInstant(zone), zone)

    @Test
    fun secondEntryReplacesTheFirst() = runTest {
        val entries = FakeEntries(
            mutableListOf(sampleEntry(today).copy(id = 1L, mediaUri = "file://old")),
        )
        val useCase = useCase(entries = entries)
        val result = useCase(sampleEntry(today).copy(id = 0, mediaUri = "file://new"))
        assertTrue(result is CompleteCaptureResult.Saved)
        assertEquals(1, entries.countForDate(today))
        assertEquals("file://new", entries.listAll().single().mediaUri)
    }

    @Test
    fun extraRowsBeyondCapAreBlocked() = runTest {
        val entries = FakeEntries(
            MutableList(2) { i ->
                sampleEntry(today).copy(id = i + 1L, mediaUri = "file://$i")
            },
        )
        val useCase = useCase(entries = entries)
        val result = useCase(sampleEntry(today).copy(id = 0, mediaUri = "file://new"))
        assertEquals(CompleteCaptureResult.CapReached, result)
        assertEquals(2, entries.countForDate(today))
    }

    @Test
    fun pendingBecomesCompletedAndUnlocksFirstLight() = runTest {
        val prompts = FakePrompts(mutableListOf(samplePrompt(today, DayStatus.PENDING)))
        val entries = FakeEntries()
        val gami = FakeGami()
        val useCase = useCase(prompts, entries, gami)
        val result = useCase(sampleEntry(today))
        assertTrue(result is CompleteCaptureResult.Saved)
        assertEquals(DayStatus.COMPLETED, prompts.getDay(today)?.status)
        val saved = result as CompleteCaptureResult.Saved
        assertTrue(saved.newlyUnlocked.any { it.id == "first_light" })
        assertEquals(1, saved.streak.current)
    }

    @Test
    fun yesterdayCaptureIsRejected() = runTest {
        val useCase = useCase()
        val result = useCase(
            sampleEntry(today).copy(
                capturedAt = today.minusDays(1).atTime(12, 0).toInstant(zone),
            ),
        )
        assertEquals(CompleteCaptureResult.NotToday, result)
    }

    private fun useCase(
        prompts: FakePrompts = FakePrompts(mutableListOf(samplePrompt(today, DayStatus.PENDING))),
        entries: FakeEntries = FakeEntries(),
        gami: FakeGami = FakeGami(),
    ) = CompleteCaptureUseCase(
        prompts = prompts,
        entries = entries,
        gamification = gami,
        preferences = FakePrefs(),
        clock = clock,
        zone = zone,
    )

    private fun samplePrompt(date: LocalDate, status: DayStatus) = DayPrompt(
        date = date,
        title = "Find the sky in a puddle",
        oneLiner = "one",
        details = "details",
        constraint = null,
        theme = "Reflections",
        tips = emptyList(),
        source = PromptSourceRef.LIBRARY,
        libraryId = "lib-1",
        modelName = null,
        generatedAt = Instant.EPOCH,
        status = status,
        frozen = false,
        rerollUsed = false,
    )

    private fun sampleEntry(date: LocalDate) = Entry(
        date = date,
        mediaUri = "file://photo.jpg",
        thumbPath = "/thumbs/a.jpg",
        capturedAt = date.atTime(12, 0).toInstant(zone),
        width = 100,
        height = 100,
        note = null,
        importedFromGallery = false,
        createdAt = Instant.EPOCH,
    )

    private class FakePrompts(private val days: MutableList<DayPrompt>) : DayPromptRepository {
        override fun observeDay(date: LocalDate) = flowOf(days.find { it.date == date })
        override fun observeDays(start: LocalDate, endInclusive: LocalDate) = flowOf(days.toList())
        override suspend fun getDay(date: LocalDate) = days.find { it.date == date }
        override suspend fun upsert(prompt: DayPrompt) {
            days.removeAll { it.date == prompt.date }
            days += prompt
        }
        override suspend fun recordSuperseded(prompt: SupersededPrompt) = Unit
        override suspend fun recentTitles(limit: Int) = emptyList<String>()
        override suspend fun recentThemes(limit: Int) = emptyList<String>()
        override suspend fun recentDays(limit: Int) = days.take(limit)
        override suspend fun allDays() = days.toList()
    }

    private class FakeEntries(
        private val rows: MutableList<Entry> = mutableListOf(),
    ) : EntryRepository {
        override fun observeEntry(date: LocalDate) = flowOf(rows.lastOrNull { it.date == date })
        override fun observeEntries(date: LocalDate) = flowOf(rows.filter { it.date == date })
        override fun observeRecentEntries(limit: Int) = flowOf(rows.take(limit))
        override fun observeEntriesByTheme(theme: String) = flowOf(emptyList<Entry>())
        override suspend fun upsert(entry: Entry) {
            rows += entry
        }
        override suspend fun delete(date: LocalDate) {
            rows.removeAll { it.date == date }
        }
        override suspend fun count() = rows.size
        override suspend fun countForDate(date: LocalDate) = rows.count { it.date == date }
        override suspend fun listAll() = rows.toList()
    }

    private class FakeGami : GamificationRepository {
        private val streak = MutableStateFlow(StreakState(0, 0, 0, null))
        private val achievements = MutableStateFlow(emptyList<Achievement>())
        override fun observeAchievements() = achievements
        override suspend fun unlock(achievement: Achievement) {
            achievements.value = achievements.value + achievement
        }
        override fun observeStreak() = streak
        override suspend fun updateStreak(state: StreakState) {
            streak.value = state
        }
        override suspend fun recordLibraryUsage(usage: LibraryUsage) = Unit
        override suspend fun libraryUsedSince(libraryId: String, since: LocalDate) = false
    }

    private class FakePrefs : PreferencesRepository {
        override fun observeNotifyTime() = flowOf(LocalTime.of(9, 0))
        override suspend fun setNotifyTime(time: LocalTime) = Unit
        override fun observePreciseTiming() = flowOf(false)
        override suspend fun setPreciseTiming(enabled: Boolean) = Unit
        override fun observeThemeFocus(): Flow<String?> = flowOf(null)
        override suspend fun setThemeFocus(focus: String?) = Unit
        override fun observePaused() = flowOf(false)
        override suspend fun setPaused(paused: Boolean) = Unit
        override fun observeOnboardingComplete() = flowOf(true)
        override suspend fun setOnboardingComplete(complete: Boolean) = Unit
        override fun observeDebugUseFakeAi() = flowOf(false)
        override suspend fun setDebugUseFakeAi(useFake: Boolean) = Unit
        override fun observeLastNotifiedDate(): Flow<LocalDate?> = flowOf(null)
        override suspend fun setLastNotifiedDate(date: LocalDate?) = Unit
    }
}
