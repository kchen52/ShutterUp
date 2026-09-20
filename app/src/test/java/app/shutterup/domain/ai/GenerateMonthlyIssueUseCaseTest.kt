package app.shutterup.domain.ai

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.MediaKind
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateMonthlyIssueUseCaseTest {
    private val zone = ZoneOffset.UTC
    private val octoberFirst = Instant.parse("2026-10-01T12:00:00Z")
    private val clock = Clock.fixed(octoberFirst, zone)

    @Test
    fun midMonthInstall_doesNotGenerateCurrentMonth() = runBlocking {
        val septemberClock = Clock.fixed(Instant.parse("2026-09-15T12:00:00Z"), zone)
        val prompts = MemoryDays()
        prompts.upsert(completedDay(LocalDate.of(2026, 9, 3), "Puddle sky", "Reflections"))
        val issues = MemoryIssues()
        val useCase = useCase(clock = septemberClock, prompts = prompts, issues = issues)
        assertEquals(emptyList<String>(), useCase.generateDue())
        assertTrue(issues.all().isEmpty())
    }

    @Test
    fun zeroCompletedDays_producesNoIssue() = runBlocking {
        val prompts = MemoryDays()
        prompts.upsert(pendingDay(LocalDate.of(2026, 9, 10)))
        val issues = MemoryIssues()
        val useCase = useCase(prompts = prompts, issues = issues)
        assertEquals(emptyList<String>(), useCase.generateDue())
        assertTrue(issues.all().isEmpty())
    }

    @Test
    fun nanoCopy_isPersistedWhenValid() = runBlocking {
        val prompts = MemoryDays()
        seedSeptember(prompts)
        val issues = MemoryIssues()
        val primary = MonthlyScriptedGenerator(
            copies = listOf(
                GeneratedMonthlyIssue(
                    headline = "Light and glass.",
                    body = "You looked up more than usual, and you kept going through a grey week. Twenty-four days sit in the contact sheet.",
                    modelName = "nano-v2",
                ),
            ),
        )
        val useCase = useCase(primary = primary, prompts = prompts, issues = issues)
        assertEquals(listOf("2026-09"), useCase.generateDue())
        val stored = issues.get("2026-09")!!
        assertEquals("Light and glass.", stored.headline)
        assertEquals(PromptSourceRef.ON_DEVICE_AI, stored.source)
        assertEquals("nano-v2", stored.modelName)
        assertEquals("Reflections", stored.dominantTheme)
        assertEquals(listOf("Reflections", "Looking up"), stored.loudestThemes)
        assertEquals(3, stored.completedDayCount)
        assertEquals(1, primary.engineHolds)
        assertEquals(1, primary.engineReleases)
    }

    @Test
    fun invalidNano_fallsBackToTemplates() = runBlocking {
        val prompts = MemoryDays()
        seedSeptember(prompts)
        val issues = MemoryIssues()
        val primary = MonthlyScriptedGenerator(
            copies = listOf(
                GeneratedMonthlyIssue(headline = "Amazing month!", body = "Great work. You crushed it."),
            ),
        )
        val useCase = useCase(primary = primary, prompts = prompts, issues = issues)
        useCase.generateDue()
        assertEquals(3, primary.calls)
        assertEquals(1, primary.engineHolds)
        assertEquals(1, primary.engineReleases)
        val stored = issues.get("2026-09")!!
        assertEquals(PromptSourceRef.LIBRARY, stored.source)
        assertEquals("Glass looking back.", stored.headline)
        assertTrue(stored.body.contains("Three days"))
    }

    @Test
    fun unavailableNano_usesFallbackWithoutCallingGenerate() = runBlocking {
        val prompts = MemoryDays()
        seedSeptember(prompts)
        val issues = MemoryIssues()
        val primary = MonthlyScriptedGenerator(copies = emptyList(), availability = Availability.UNAVAILABLE)
        val useCase = useCase(primary = primary, prompts = prompts, issues = issues)
        useCase.generateDue()
        assertEquals(0, primary.calls)
        assertEquals(0, primary.engineHolds)
        assertEquals(0, primary.engineReleases)
        assertEquals(PromptSourceRef.LIBRARY, issues.get("2026-09")!!.source)
    }

    @Test
    fun regenerate_doesNotDuplicate() = runBlocking {
        val prompts = MemoryDays()
        seedSeptember(prompts)
        val issues = MemoryIssues()
        val useCase = useCase(prompts = prompts, issues = issues)
        assertEquals(listOf("2026-09"), useCase.generateDue())
        assertEquals(emptyList<String>(), useCase.generateDue())
        assertEquals(1, issues.all().size)
        assertEquals("Glass looking back.", issues.get("2026-09")!!.headline)
    }

    @Test
    fun catchUp_skipsEmptyMonthsAndWritesFinishedOnes() = runBlocking {
        val novemberClock = Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), zone)
        val prompts = MemoryDays()
        prompts.upsert(completedDay(LocalDate.of(2026, 8, 20), "Window", "Light"))
        prompts.upsert(completedDay(LocalDate.of(2026, 8, 21), "Glass", "Glass"))
        prompts.upsert(completedDay(LocalDate.of(2026, 10, 2), "Hands", "Hands"))
        val issues = MemoryIssues()
        val useCase = useCase(clock = novemberClock, prompts = prompts, issues = issues)
        val created = useCase.generateDue()
        assertEquals(listOf("2026-08", "2026-10"), created)
        assertEquals(null, issues.get("2026-09"))
    }

    @Test
    fun deviceOffAcrossYearBoundary_writesDecember() = runBlocking {
        val januaryClock = Clock.fixed(Instant.parse("2027-01-02T08:00:00Z"), zone)
        val prompts = MemoryDays()
        prompts.upsert(completedDay(LocalDate.of(2026, 12, 14), "Frost", "Winter light"))
        prompts.upsert(completedDay(LocalDate.of(2026, 12, 15), "Glass", "Reflections"))
        val issues = MemoryIssues()
        val useCase = useCase(clock = januaryClock, prompts = prompts, issues = issues)
        assertEquals(listOf("2026-12"), useCase.generateDue())
    }

    @Test
    fun notesReachTheRequestButAreNotQuotedInFallback() = runBlocking {
        val prompts = MemoryDays()
        seedSeptember(prompts)
        val entries = MemoryEntries()
        entries.upsert(
            entry(
                LocalDate.of(2026, 9, 4),
                note = "the glass in the stairwell again",
            ),
        )
        val issues = MemoryIssues()
        val primary = MonthlyScriptedGenerator(availability = Availability.UNAVAILABLE)
        val useCase = useCase(primary = primary, prompts = prompts, entries = entries, issues = issues)
        useCase.generateDue()
        val stored = issues.get("2026-09")!!
        assertTrue(stored.body.contains("You wrote"))
        assertTrue(!stored.body.contains("stairwell"))
    }

    private fun useCase(
        primary: PromptGenerator = MonthlyScriptedGenerator(availability = Availability.UNAVAILABLE),
        prompts: MemoryDays = MemoryDays(),
        entries: MemoryEntries = MemoryEntries(),
        issues: MemoryIssues = MemoryIssues(),
        clock: Clock = this.clock,
    ) = GenerateMonthlyIssueUseCase(
        primary = primary,
        prompts = prompts,
        entries = entries,
        issues = issues,
        validator = MonthlyIssueValidator(),
        clock = clock,
        zone = zone,
    )

    private fun seedSeptember(prompts: MemoryDays) {
        prompts.put(completedDay(LocalDate.of(2026, 9, 4), "Puddle sky", "Reflections"))
        prompts.put(completedDay(LocalDate.of(2026, 9, 12), "Ceiling lamp", "Looking up"))
        prompts.put(completedDay(LocalDate.of(2026, 9, 20), "Shop window", "Reflections"))
    }
}

private fun completedDay(date: LocalDate, title: String, theme: String) = DayPrompt(
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

private fun pendingDay(date: LocalDate) = completedDay(date, "Pending", "Light")
    .copy(status = DayStatus.PENDING)

private fun entry(date: LocalDate, note: String?) = Entry(
    date = date,
    mediaUri = "content://photo",
    thumbPath = "/thumbs/$date.jpg",
    capturedAt = Instant.EPOCH,
    width = 100,
    height = 100,
    note = note,
    importedFromGallery = false,
    createdAt = Instant.EPOCH,
    mediaKind = MediaKind.PHOTO,
)

private class MonthlyScriptedGenerator(
    private val copies: List<GeneratedMonthlyIssue> = emptyList(),
    private val availability: Availability = Availability.AVAILABLE,
) : PromptGenerator {
    var calls: Int = 0
        private set
    var engineHolds: Int = 0
        private set
    var engineReleases: Int = 0
        private set

    override suspend fun availability(): Availability = availability

    override suspend fun holdEngine() {
        engineHolds += 1
    }

    override suspend fun releaseEngine() {
        engineReleases += 1
    }

    override suspend fun generate(request: GenerationRequest): Result<GeneratedPrompt> =
        Result.failure(UnsupportedOperationException())

    override suspend fun generateMonthlyIssue(request: MonthlyIssueRequest): Result<GeneratedMonthlyIssue> {
        if (copies.isEmpty()) return Result.failure(IllegalStateException("no copy"))
        val copy = copies[calls.coerceAtMost(copies.lastIndex)]
        calls += 1
        return Result.success(copy)
    }
}

private class MemoryDays : DayPromptRepository {
    private val days = linkedMapOf<LocalDate, DayPrompt>()
    override fun observeDay(date: LocalDate) = flowOf(days[date])
    override fun observeDays(start: LocalDate, endInclusive: LocalDate) =
        flowOf(days.values.filter { it.date in start..endInclusive })
    override suspend fun getDay(date: LocalDate) = days[date]
    override suspend fun upsert(prompt: DayPrompt) {
        days[prompt.date] = prompt
    }
    fun put(prompt: DayPrompt) {
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

private class MemoryEntries : EntryRepository {
    private val rows = mutableListOf<Entry>()
    override fun observeEntry(date: LocalDate) = flowOf(rows.lastOrNull { it.date == date })
    override fun observeEntries(date: LocalDate) = flowOf(rows.filter { it.date == date })
    override fun observeRecentEntries(limit: Int) = flowOf(rows.take(limit))
    override fun observeEntriesByTheme(theme: String) = flowOf(emptyList<Entry>())
    override suspend fun upsert(entry: Entry) {
        rows += entry
    }
    override suspend fun delete(date: LocalDate) = Unit
    override suspend fun count() = rows.size
    override suspend fun countForDate(date: LocalDate) = rows.count { it.date == date }
    override suspend fun listAll() = rows.toList()
}

private class MemoryIssues : MonthlyIssueRepository {
    private val rows = linkedMapOf<String, MonthlyIssue>()
    private var nextId = 1L
    override fun observeAll(): Flow<List<MonthlyIssue>> = flowOf(rows.values.sortedByDescending { it.yearMonth })
    override fun observe(yearMonth: String): Flow<MonthlyIssue?> = flowOf(rows[yearMonth])
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
