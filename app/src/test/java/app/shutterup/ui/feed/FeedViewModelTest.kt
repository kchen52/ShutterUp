package app.shutterup.ui.feed

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.MonthlyIssue
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.MonthlyIssueRepository
import app.shutterup.domain.model.SupersededPrompt
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setMain() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun reset() {
        Dispatchers.resetMain()
    }

    @Test
    fun mappingDropsNonCompletedAndSortsNewestFirst() {
        val days = listOf(
            prompt(LocalDate.of(2026, 9, 10), DayStatus.MISSED, "Missed"),
            prompt(LocalDate.of(2026, 9, 12), DayStatus.COMPLETED, "Older"),
            prompt(LocalDate.of(2026, 9, 19), DayStatus.COMPLETED, "Newest"),
            prompt(LocalDate.of(2026, 9, 11), DayStatus.PENDING, "Today"),
            prompt(LocalDate.of(2026, 9, 8), DayStatus.COMPLETED_NO_PHOTO, "No photo"),
        )
        val completed = completedDaysNewestFirst(days)
        assertEquals(
            listOf(
                LocalDate.of(2026, 9, 19),
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 8),
            ),
            completed.map { it.date },
        )
    }

    @Test
    fun viewModelFiltersByTheme() = runTest(UnconfinedTestDispatcher()) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val days = MutableStateFlow(
            listOf(
                prompt(LocalDate.of(2026, 9, 19), DayStatus.COMPLETED, "Puddle", "Reflections"),
                prompt(LocalDate.of(2026, 9, 12), DayStatus.COMPLETED, "Kitchen", "Quiet hours"),
            ),
        )
        val vm = FeedViewModel(FakePrompts(days), FakeEntries(), FakeIssues())
        val collected = mutableListOf<FeedUiState>()
        val job = launch { vm.state.collect { collected.add(it) } }
        vm.selectTheme("Reflections")
        job.cancel()
        val last = collected.last()
        assertEquals(listOf("Reflections", "Quiet hours"), last.themes)
        assertEquals(listOf("2026-09-19"), last.items.map { it.dateIso })
        assertEquals("19 SEP · REFLECTIONS", last.items.single().kicker)
    }

    private fun prompt(
        date: LocalDate,
        status: DayStatus,
        title: String,
        theme: String = "Reflections",
    ) = DayPrompt(
        date = date,
        title = title,
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
        override suspend fun recentDays(limit: Int) = days.value.take(limit)
        override suspend fun allDays() = days.value
        override suspend fun deleteAfter(date: LocalDate) = Unit
        override fun observeDaysInSeries(seriesId: Long) = flowOf(emptyList<DayPrompt>())
        override suspend fun daysInSeries(seriesId: Long) = emptyList<DayPrompt>()
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

    private class FakeIssues : MonthlyIssueRepository {
        override fun observeAll() = flowOf(emptyList<MonthlyIssue>())
        override fun observe(yearMonth: String) = flowOf(null)
        override suspend fun get(yearMonth: String) = null
        override suspend fun all() = emptyList<MonthlyIssue>()
        override suspend fun insert(issue: MonthlyIssue) = 1L
        override suspend fun dismissFromFeed(yearMonth: String) = Unit
    }
}
