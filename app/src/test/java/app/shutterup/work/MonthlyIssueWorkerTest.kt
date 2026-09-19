package app.shutterup.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import app.shutterup.domain.ai.Availability
import app.shutterup.domain.ai.GenerateMonthlyIssueUseCase
import app.shutterup.domain.ai.GeneratedPrompt
import app.shutterup.domain.ai.GenerationRequest
import app.shutterup.domain.ai.MonthlyIssueValidator
import app.shutterup.domain.ai.PromptGenerator
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.MonthlyIssue
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.domain.model.SupersededPrompt
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.MonthlyIssueRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MonthlyIssueWorkerTest {

    @Test
    fun doWork_writesTheFinishedMonthOnce() = runBlocking {
        val prompts = MemoryDays()
        prompts.upsert(completed(LocalDate.of(2026, 9, 4), "Puddle", "Reflections"))
        prompts.upsert(completed(LocalDate.of(2026, 9, 12), "Lamp", "Looking up"))
        val issues = MemoryIssues()
        val worker = workerFor(prompts, issues)
        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        assertEquals(1, issues.all().size)
        assertEquals("2026-09", issues.all().single().yearMonth)
        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        assertEquals(1, issues.all().size)
    }

    @Test
    fun doWork_retriesWhenGenerateThrows() = runBlocking {
        val worker = TestListenableWorkerBuilder<MonthlyIssueWorker>(
            ApplicationProvider.getApplicationContext(),
        ).setWorkerFactory(
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = MonthlyIssueWorker(appContext, workerParameters, ThrowingGenerate())
            },
        ).build()
        assertEquals(ListenableWorker.Result.retry(), worker.doWork())
    }

    private fun workerFor(prompts: MemoryDays, issues: MemoryIssues): MonthlyIssueWorker {
        val clock = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC)
        val generate = GenerateMonthlyIssueUseCase(
            primary = UnavailableGenerator,
            prompts = prompts,
            entries = EmptyEntries,
            issues = issues,
            validator = MonthlyIssueValidator(),
            clock = clock,
            zone = ZoneOffset.UTC,
        )
        return TestListenableWorkerBuilder<MonthlyIssueWorker>(
            ApplicationProvider.getApplicationContext(),
        ).setWorkerFactory(
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = MonthlyIssueWorker(appContext, workerParameters, generate)
            },
        ).build()
    }
}

private object UnavailableGenerator : PromptGenerator {
    override suspend fun availability() = Availability.UNAVAILABLE
    override suspend fun generate(request: GenerationRequest): Result<GeneratedPrompt> =
        Result.failure(UnsupportedOperationException())
}

private class ThrowingGenerate : GenerateMonthlyIssueUseCase(
    primary = UnavailableGenerator,
    prompts = MemoryDays(),
    entries = EmptyEntries,
    issues = MemoryIssues(),
    validator = MonthlyIssueValidator(),
    clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
    zone = ZoneOffset.UTC,
) {
    override suspend fun generateDue(): List<String> = error("boom")
}

private fun completed(date: LocalDate, title: String, theme: String) = DayPrompt(
    date = date,
    title = title,
    oneLiner = "one",
    details = "details. more.",
    constraint = null,
    theme = theme,
    tips = listOf("tip"),
    source = PromptSourceRef.LIBRARY,
    libraryId = "lib",
    modelName = null,
    generatedAt = Instant.EPOCH,
    status = DayStatus.COMPLETED,
    frozen = false,
    rerollUsed = false,
)

private class MemoryDays : DayPromptRepository {
    private val days = linkedMapOf<LocalDate, DayPrompt>()
    override fun observeDay(date: LocalDate) = flowOf(days[date])
    override fun observeDays(start: LocalDate, endInclusive: LocalDate) =
        flowOf(days.values.filter { it.date in start..endInclusive })
    override suspend fun getDay(date: LocalDate) = days[date]
    override suspend fun upsert(prompt: DayPrompt) {
        days[prompt.date] = prompt
    }
    override suspend fun recordSuperseded(prompt: SupersededPrompt) = Unit
    override suspend fun recentTitles(limit: Int) = emptyList<String>()
    override suspend fun recentThemes(limit: Int) = emptyList<String>()
    override suspend fun recentDays(limit: Int) = days.values.toList()
    override suspend fun allDays() = days.values.sortedBy { it.date }
    override suspend fun deleteAfter(date: LocalDate) = Unit
    override fun observeDaysInSeries(seriesId: Long) = flowOf(emptyList<DayPrompt>())
    override suspend fun daysInSeries(seriesId: Long) = emptyList<DayPrompt>()
}

private object EmptyEntries : EntryRepository {
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

private class MemoryIssues : MonthlyIssueRepository {
    private val rows = linkedMapOf<String, MonthlyIssue>()
    private var nextId = 1L
    override fun observeAll() = flowOf(all())
    override fun observe(yearMonth: String) = flowOf(rows[yearMonth])
    override suspend fun get(yearMonth: String) = rows[yearMonth]
    override suspend fun all() = rows.values.sortedByDescending { it.yearMonth }
    override suspend fun insert(issue: MonthlyIssue): Long {
        if (rows.containsKey(issue.yearMonth)) return -1L
        val id = nextId++
        rows[issue.yearMonth] = issue.copy(id = id)
        return id
    }
    override suspend fun dismissFromFeed(yearMonth: String) {
        rows[yearMonth]?.let { rows[yearMonth] = it.copy(dismissedFromFeed = true) }
    }
}
