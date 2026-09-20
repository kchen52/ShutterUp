package app.shutterup.testutil

import app.shutterup.domain.model.Achievement
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.LibraryUsage
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.model.SupersededPrompt
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeDayPrompts(
    private val days: MutableList<DayPrompt> = mutableListOf(),
) : DayPromptRepository {
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
    override suspend fun deleteAfter(date: LocalDate) {
        days.removeAll { it.date > date }
    }
    override fun observeDaysInSeries(seriesId: Long) = flowOf(seriesDays(seriesId))
    override suspend fun daysInSeries(seriesId: Long) = seriesDays(seriesId)

    private fun seriesDays(seriesId: Long) =
        days.filter { it.seriesId == seriesId }.sortedBy { it.date }
}

class FakeEntries(
    private val rows: MutableList<Entry> = mutableListOf(),
) : EntryRepository {
    override fun observeEntry(date: LocalDate) = flowOf(rows.lastOrNull { it.date == date })
    override fun observeEntries(date: LocalDate) = flowOf(rows.filter { it.date == date })
    override fun observeRecentEntries(limit: Int) = flowOf(rows.take(limit))
    override fun observeEntriesByTheme(theme: String) = flowOf(emptyList<Entry>())
    override suspend fun upsert(entry: Entry) {
        rows.removeAll { it.date == entry.date && (entry.id == 0L || it.id == entry.id) }
        val stored = if (entry.id == 0L) {
            val nextId = (rows.maxOfOrNull { it.id } ?: 0L) + 1L
            entry.copy(id = nextId)
        } else {
            entry
        }
        rows += stored
    }
    override suspend fun delete(date: LocalDate) {
        rows.removeAll { it.date == date }
    }
    override suspend fun count() = rows.size
    override suspend fun countForDate(date: LocalDate) = rows.count { it.date == date }
    override suspend fun listAll() = rows.toList()
}

class FakeGamification : GamificationRepository {
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

class FakePrefs : PreferencesRepository {
    private val fakeAi = MutableStateFlow(false)
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
    override fun observeDebugUseFakeAi(): Flow<Boolean> = fakeAi
    override suspend fun setDebugUseFakeAi(useFake: Boolean) {
        fakeAi.value = useFake
    }
    override fun observeLastNotifiedDate(): Flow<LocalDate?> = flowOf(null)
    override suspend fun setLastNotifiedDate(date: LocalDate?) = Unit
    override fun observeSeriesEnabled() = flowOf(false)
    override suspend fun setSeriesEnabled(enabled: Boolean) = Unit
    override fun observeCoarseCityId(): Flow<String?> = flowOf(null)
    override suspend fun setCoarseCityId(id: String?) = Unit
}
