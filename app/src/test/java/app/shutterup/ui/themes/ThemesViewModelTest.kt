package app.shutterup.ui.themes

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.domain.model.SupersededPrompt
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.PreferencesRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemesViewModelTest {

    @Before
    fun setMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun reset() {
        Dispatchers.resetMain()
    }

    @Test
    fun groupsCountsAndFilters() = runTest(UnconfinedTestDispatcher()) {
        val days = MutableStateFlow(
            listOf(
                prompt(LocalDate.of(2026, 9, 19), "Reflections"),
                prompt(LocalDate.of(2026, 9, 5), "Reflections"),
                prompt(LocalDate.of(2026, 9, 12), "Quiet hours"),
                prompt(LocalDate.of(2026, 9, 4), "Reflections", DayStatus.SKIPPED),
            ),
        )
        val vm = ThemesViewModel(FakePrompts(days), FakeEntries(), FakePrefs("my dog"))
        val collected = mutableListOf<ThemesUiState>()
        val job = launch { vm.state.collect { collected.add(it) } }
        vm.selectTheme("Reflections")
        job.cancel()
        val last = collected.last()
        assertEquals("my dog", last.themeFocus)
        assertEquals(
            listOf(ThemeCountUi("Reflections", 2), ThemeCountUi("Quiet hours", 1)),
            last.themes,
        )
        assertEquals(listOf("2026-09-19", "2026-09-05"), last.items.map { it.dateIso })
    }

    private fun prompt(
        date: LocalDate,
        theme: String,
        status: DayStatus = DayStatus.COMPLETED,
    ) = DayPrompt(
        date = date,
        title = "Title",
        oneLiner = "one",
        details = "details",
        constraint = null,
        theme = theme,
        tips = emptyList(),
        source = PromptSourceRef.LIBRARY,
        libraryId = "lib-1",
        modelName = null,
        generatedAt = Instant.EPOCH,
        status = status,
        frozen = false,
        rerollUsed = false,
    )

    private class FakePrompts(
        private val days: MutableStateFlow<List<DayPrompt>>,
    ) : DayPromptRepository {
        override fun observeDay(date: LocalDate) = flowOf(days.value.find { it.date == date })
        override fun observeDays(start: LocalDate, endInclusive: LocalDate) = days
        override suspend fun getDay(date: LocalDate) = days.value.find { it.date == date }
        override suspend fun upsert(prompt: DayPrompt) = Unit
        override suspend fun recordSuperseded(prompt: SupersededPrompt) = Unit
        override suspend fun recentTitles(limit: Int) = emptyList<String>()
        override suspend fun recentThemes(limit: Int) = emptyList<String>()
        override suspend fun recentDays(limit: Int) = days.value
        override suspend fun allDays() = days.value
    }

    private class FakeEntries : EntryRepository {
        override fun observeEntry(date: LocalDate) = flowOf(null)
        override fun observeEntries(date: LocalDate) = flowOf(emptyList<Entry>())
        override fun observeRecentEntries(limit: Int) = flowOf(emptyList<Entry>())
        override fun observeEntriesByTheme(theme: String) = flowOf(emptyList<Entry>())
        override suspend fun upsert(entry: Entry) = Unit
        override suspend fun delete(date: LocalDate) = Unit
        override suspend fun count() = 0
        override suspend fun countForDate(date: LocalDate) = 0
        override suspend fun listAll() = emptyList<Entry>()
    }

    private class FakePrefs(focus: String?) : PreferencesRepository {
        private val themeFocus = MutableStateFlow(focus)
        override fun observeNotifyTime() = flowOf(LocalTime.of(9, 0))
        override suspend fun setNotifyTime(time: LocalTime) = Unit
        override fun observePreciseTiming() = flowOf(false)
        override suspend fun setPreciseTiming(enabled: Boolean) = Unit
        override fun observeThemeFocus() = themeFocus
        override suspend fun setThemeFocus(focus: String?) = Unit
        override fun observePaused() = flowOf(false)
        override suspend fun setPaused(paused: Boolean) = Unit
        override fun observeOnboardingComplete() = flowOf(true)
        override suspend fun setOnboardingComplete(complete: Boolean) = Unit
        override fun observeDebugUseFakeAi() = flowOf(false)
        override suspend fun setDebugUseFakeAi(useFake: Boolean) = Unit
        override fun observeLastNotifiedDate() = flowOf(null)
        override suspend fun setLastNotifiedDate(date: LocalDate?) = Unit
    }
}
