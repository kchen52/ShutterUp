package app.shutterup.domain.ai

import app.shutterup.domain.model.Achievement
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.LibraryUsage
import app.shutterup.domain.model.Series
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.model.SupersededPrompt
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.domain.repository.SeriesRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratePromptUseCaseTest {

    private val date = LocalDate.of(2024, 6, 15)
    private val clock = Clock.fixed(Instant.parse("2024-06-15T12:00:00Z"), ZoneOffset.UTC)
    private val zone = ZoneOffset.UTC
    private val validator = PromptValidator(emptySet(), DEFAULT_GEAR_TERMS)

    @Test
    fun primaryInvalidTwiceThenValid_succeedsOnThirdCall() = runBlocking {
        val primary = ScriptedGenerator(
            listOf(invalidPrompt(), invalidPrompt(), validPrompt(title = "Third Try Light")),
        )
        val useCase = useCase(primary = primary)
        val result = useCase.promptFor(date, themeFocus = null)
        assertEquals(3, primary.calls)
        assertEquals("Third Try Light", result.title)
        assertEquals(PromptSource.ON_DEVICE_AI, result.source)
    }

    @Test
    fun primaryAlwaysInvalid_usesLibraryAndRecordsUsage() = runBlocking {
        val primary = ScriptedGenerator(listOf(invalidPrompt()))
        val libraryPrompt = libraryEntry("lib-42", "Kettle Weather")
        val gamification = RecordingGamification()
        val prompts = FakeDayPrompts()
        val useCase = useCase(
            primary = primary,
            librarySource = UseCaseLibrarySource(listOf(libraryPrompt)),
            prompts = prompts,
            gamification = gamification,
        )
        val result = useCase.promptFor(date, themeFocus = null)
        assertEquals(3, primary.calls)
        assertEquals("Kettle Weather", result.title)
        assertEquals(PromptSource.LIBRARY, result.source)
        assertEquals(listOf(LibraryUsage("lib-42", date)), gamification.recorded)
        assertEquals("lib-42", prompts.getDay(date)?.libraryId)
    }

    @Test
    fun primaryLibraryResult_recordsUsageByTitle() = runBlocking {
        val libraryPrompt = libraryEntry("lib-7", "Kettle Weather")
        val primary = ScriptedGenerator(listOf(libraryPrompt.toGeneratedPrompt()))
        val gamification = RecordingGamification()
        val prompts = FakeDayPrompts()
        val useCase = useCase(
            primary = primary,
            librarySource = UseCaseLibrarySource(listOf(libraryPrompt)),
            prompts = prompts,
            gamification = gamification,
        )
        val result = useCase.promptFor(date, themeFocus = null)
        assertEquals(PromptSource.LIBRARY, result.source)
        assertEquals(listOf(LibraryUsage("lib-7", date)), gamification.recorded)
        assertEquals("lib-7", prompts.getDay(date)?.libraryId)
    }

    @Test
    fun themeFocusReachesTheRequest() = runBlocking {        val primary = ScriptedGenerator(listOf(validPrompt()))
        val useCase = useCase(primary = primary)
        useCase.promptFor(date, themeFocus = "architecture")
        assertEquals("architecture", primary.lastRequest?.themeFocus)
    }

    @Test
    fun secondPromptForSameDate_doesNotCallPrimaryAgain() = runBlocking {
        val primary = ScriptedGenerator(listOf(validPrompt(title = "Once Only")))
        val useCase = useCase(primary = primary)
        val first = useCase.promptFor(date, null)
        val second = useCase.promptFor(date, "should not matter")
        assertEquals(1, primary.calls)
        assertEquals(first, second)
        assertEquals("Once Only", second.title)
    }

    @Test
    fun topUpBufferFillsTodayPlusTwo() = runBlocking {
        val primary = ScriptedGenerator(
            listOf(
                validPrompt(
                    title = "Day Zero Light",
                    oneLiner = "Photograph morning light on a table.",
                    theme = "Morning",
                ),
                validPrompt(
                    title = "Day One Light",
                    oneLiner = "Find a shadow that splits a wall.",
                    theme = "Shadow",
                ),
                validPrompt(
                    title = "Day Two Light",
                    oneLiner = "Frame a doorway as a bright rectangle.",
                    theme = "Doorway",
                ),
            ),
        )
        val prompts = FakeDayPrompts()
        val useCase = useCase(primary = primary, prompts = prompts)
        val filled = useCase.topUpBuffer(date, themeFocus = "windows")
        assertEquals(listOf(date, date.plusDays(1), date.plusDays(2)), filled)
        assertEquals(3, primary.calls)
        assertEquals("windows", primary.requests.map { it.themeFocus }.distinct().single())
        assertTrue(prompts.getDay(date) != null)
        assertTrue(prompts.getDay(date.plusDays(1)) != null)
        assertTrue(prompts.getDay(date.plusDays(2)) != null)
    }

    @Test
    fun rerollRecordsSupersededAndSetsRerollUsed() = runBlocking {
        val primary = ScriptedGenerator(
            listOf(
                validPrompt(
                    title = "Original Frame",
                    oneLiner = "Capture crumbs as a leading line.",
                    theme = "Table",
                ),
                validPrompt(
                    title = "Rerolled Frame",
                    oneLiner = "Turn a mug into a silhouette study.",
                    theme = "Silhouette",
                ),
            ),
        )
        val prompts = FakeDayPrompts()
        val useCase = useCase(primary = primary, prompts = prompts)
        useCase.promptFor(date, null)
        val rerolled = useCase.reroll(date, "still life")
        assertEquals("Rerolled Frame", rerolled.title)
        assertEquals(1, prompts.superseded.size)
        assertEquals("Original Frame", prompts.superseded.single().title)
        assertEquals(date, prompts.superseded.single().date)
        assertEquals("still life", primary.lastRequest?.themeFocus)
        assertEquals(true, prompts.getDay(date)?.rerollUsed)
        assertEquals("Rerolled Frame", prompts.getDay(date)?.title)
    }

    @Test
    fun rerollClearsRepeatLinkAndGeneratesANormalPrompt() = runBlocking {
        val primary = ScriptedGenerator(
            listOf(
                validPrompt(
                    title = "Original Frame",
                    oneLiner = "Capture crumbs as a leading line.",
                    theme = "Table",
                ),
                validPrompt(
                    title = "Rerolled Frame",
                    oneLiner = "Turn a mug into a silhouette study.",
                    theme = "Silhouette",
                ),
            ),
        )
        val prompts = FakeDayPrompts()
        val original = LocalDate.of(2024, 5, 1)
        val useCase = useCase(primary = primary, prompts = prompts)
        useCase.promptFor(date, null)
        prompts.upsert(prompts.getDay(date)!!.copy(repeatsDate = original))
        val rerolled = useCase.reroll(date, null)
        assertEquals("Rerolled Frame", rerolled.title)
        assertEquals(null, prompts.getDay(date)?.repeatsDate)
        assertEquals(true, prompts.getDay(date)?.rerollUsed)
    }

    @Test
    fun seriesEnabled_generatesSevenRelatedDays() = runBlocking {
        val primary = ScriptedGenerator(
            prompts = listOf(validPrompt()),
            series = listOf(validSeries()),
        )
        val prompts = FakeDayPrompts()
        val series = MemorySeries()
        val prefs = TogglePreferences(enabled = true)
        val useCase = useCase(primary = primary, prompts = prompts, preferences = prefs, series = series)
        val result = useCase.promptFor(date, null)
        assertEquals("Hands day 1 light", result.title)
        assertEquals(1, primary.seriesCalls)
        assertEquals(7, prompts.allDays().size)
        assertEquals(1, series.all().size)
        assertEquals("A Week of Hands", series.all().single().title)
        assertEquals(date, series.all().single().startDate)
        assertEquals(date.plusDays(6), series.all().single().endDate)
        assertEquals(1, prompts.getDay(date)?.seriesIndex)
        assertEquals(7, prompts.getDay(date.plusDays(6))?.seriesIndex)
    }

    @Test
    fun seriesEnabled_doesNotReplaceExistingTodayPrompt() = runBlocking {
        val primary = ScriptedGenerator(
            prompts = listOf(validPrompt(title = "Already Here")),
            series = listOf(validSeries()),
        )
        val prompts = FakeDayPrompts()
        val prefs = TogglePreferences(enabled = false)
        val useCase = useCase(primary = primary, prompts = prompts, preferences = prefs)
        useCase.promptFor(date, null)
        prefs.enabled = true
        val again = useCase.promptFor(date, "should not matter")
        assertEquals("Already Here", again.title)
        assertEquals(0, primary.seriesCalls)
        assertEquals(1, prompts.allDays().size)
    }

    @Test
    fun seriesFallbackToLibraryTheme_whenPrimarySeriesInvalid() = runBlocking {
        val items = (1..8).map { i ->
            libraryEntry("hands-$i", "Hand study $i").copy(
                theme = "Hands",
                tags = listOf("indoor", "hands"),
                oneLiner = "Photograph gesture $i on a nearby surface today.",
            )
        }
        val primary = ScriptedGenerator(prompts = listOf(invalidPrompt()), series = listOf(validSeries().copy(title = "")))
        val prompts = FakeDayPrompts()
        val series = MemorySeries()
        val useCase = useCase(
            primary = primary,
            librarySource = UseCaseLibrarySource(items),
            prompts = prompts,
            preferences = TogglePreferences(enabled = true),
            series = series,
        )
        val result = useCase.promptFor(date, null)
        assertEquals(PromptSource.LIBRARY, result.source)
        assertEquals(7, prompts.allDays().size)
        assertEquals("A Week of Hands", series.all().single().title)
        assertTrue(prompts.getDay(date)?.libraryId != null)
    }

    @Test
    fun rerollInsideSeries_passesSeriesTitleOnRequest() = runBlocking {
        val seriesPrompts = validSeries()
        val reroll = validPrompt(
            title = "Rerolled hand light",
            oneLiner = "Catch the shadow a wrist throws on a table.",
            theme = "Hands",
        )
        val primary = ScriptedGenerator(
            prompts = listOf(reroll),
            series = listOf(seriesPrompts),
        )
        val prompts = FakeDayPrompts()
        val series = MemorySeries()
        val useCase = useCase(
            primary = primary,
            prompts = prompts,
            preferences = TogglePreferences(enabled = true),
            series = series,
        )
        useCase.promptFor(date, null)
        val rerolled = useCase.reroll(date, "architecture")
        assertEquals("Rerolled hand light", rerolled.title)
        assertEquals("A Week of Hands", primary.lastRequest?.seriesTitle)
        assertEquals(true, prompts.getDay(date)?.rerollUsed)
        assertEquals(prompts.getDay(date.plusDays(1))?.seriesId, prompts.getDay(date)?.seriesId)
        assertEquals(1, prompts.superseded.size)
    }

    @Test
    fun discardUnshownFuture_keepsTodayAndDropsBuffer() = runBlocking {
        val primary = ScriptedGenerator(
            listOf(
                validPrompt(title = "Day Zero Light", oneLiner = "Photograph morning light on a table.", theme = "Morning"),
                validPrompt(title = "Day One Light", oneLiner = "Find a shadow that splits a wall.", theme = "Shadow"),
                validPrompt(title = "Day Two Light", oneLiner = "Frame a doorway as a bright rectangle.", theme = "Doorway"),
            ),
        )
        val prompts = FakeDayPrompts()
        val useCase = useCase(primary = primary, prompts = prompts)
        useCase.topUpBuffer(date, "windows")
        assertEquals(3, prompts.allDays().size)
        useCase.discardUnshownFuture(date)
        assertEquals("Day Zero Light", prompts.getDay(date)?.title)
        assertEquals(null, prompts.getDay(date.plusDays(1)))
        assertEquals(null, prompts.getDay(date.plusDays(2)))
        val regenerated = useCase.topUpBuffer(date, "my dog")
        assertEquals(listOf(date.plusDays(1), date.plusDays(2)), regenerated)
        assertEquals("my dog", primary.lastRequest?.themeFocus)
    }

    @Test
    fun seriesEnabled_startsOnNextEmptyDate_afterExistingToday() = runBlocking {
        val primary = ScriptedGenerator(
            prompts = listOf(validPrompt(title = "Already Here")),
            series = listOf(validSeries()),
        )
        val prompts = FakeDayPrompts()
        val series = MemorySeries()
        val prefs = TogglePreferences(enabled = false)
        val useCase = useCase(primary = primary, prompts = prompts, preferences = prefs, series = series)
        useCase.promptFor(date, null)
        prefs.enabled = true
        useCase.topUpBuffer(date, null)
        assertEquals("Already Here", prompts.getDay(date)?.title)
        assertEquals(null, prompts.getDay(date)?.seriesId)
        assertEquals(date.plusDays(1), series.all().single().startDate)
        assertEquals(1, prompts.getDay(date.plusDays(1))?.seriesIndex)
        assertEquals(7, prompts.allDays().count { it.seriesId != null })
    }

    @Test
    fun discardUnshownFuture_truncatesActiveSeriesAndKeepsToday() = runBlocking {
        val primary = ScriptedGenerator(prompts = listOf(validPrompt()), series = listOf(validSeries()))
        val prompts = FakeDayPrompts()
        val series = MemorySeries()
        val useCase = useCase(
            primary = primary,
            prompts = prompts,
            preferences = TogglePreferences(enabled = true),
            series = series,
        )
        useCase.promptFor(date, null)
        assertEquals(7, prompts.allDays().size)
        useCase.discardUnshownFuture(date)
        assertEquals("Hands day 1 light", prompts.getDay(date)?.title)
        assertEquals(1, prompts.allDays().size)
        assertEquals(date, series.all().single().endDate)
        assertEquals(null, prompts.getDay(date.plusDays(1)))
    }

    @Test
    fun seriesDisabledAfterStart_leavesGeneratedDays() = runBlocking {
        val primary = ScriptedGenerator(prompts = listOf(validPrompt()), series = listOf(validSeries()))
        val prompts = FakeDayPrompts()
        val prefs = TogglePreferences(enabled = true)
        val series = MemorySeries()
        val useCase = useCase(primary = primary, prompts = prompts, preferences = prefs, series = series)
        useCase.promptFor(date, null)
        prefs.enabled = false
        val later = date.plusDays(7)
        val after = useCase.promptFor(later, null)
        assertEquals("Steam Maps", after.title)
        assertEquals(null, prompts.getDay(later)?.seriesId)
        assertEquals(7, prompts.allDays().count { it.seriesId != null })
    }

    private fun useCase(
        primary: PromptGenerator,
        librarySource: LibraryPromptSource = UseCaseLibrarySource(listOf(libraryEntry("fallback", "Library Frame"))),
        prompts: FakeDayPrompts = FakeDayPrompts(),
        gamification: RecordingGamification = RecordingGamification(),
        preferences: TogglePreferences = TogglePreferences(),
        series: MemorySeries = MemorySeries(),
    ): GeneratePromptUseCase {
        val library = LibraryPromptGenerator(
            source = librarySource,
            gamification = gamification,
            clock = clock,
            random = Random(0),
        )
        return GeneratePromptUseCase(
            primary = primary,
            library = library,
            prompts = prompts,
            gamification = gamification,
            validator = validator,
            preferences = preferences,
            seriesRepo = series,
            clock = clock,
            zone = zone,
        )
    }
}

private fun validPrompt(
    title: String = "Steam Maps",
    oneLiner: String = "Turn kitchen steam into contour lines of light.",
    theme: String = "Steam",
): GeneratedPrompt = GeneratedPrompt(
    title = title,
    oneLiner = oneLiner,
    details = "Wait for a kettle or hot tap. Side-light the plume and expose for the brightest edge.",
    tips = listOf("Use a dark backdrop."),
    constraint = "No zoom",
    theme = theme,
    source = PromptSource.ON_DEVICE_AI,
)

private fun invalidPrompt(): GeneratedPrompt =
    validPrompt(title = "This title is definitely longer than forty characters")

private fun libraryEntry(id: String, title: String): LibraryPrompt = LibraryPrompt(
    id = id,
    title = title,
    oneLiner = "Look at ordinary light on a nearby surface today.",
    details = "Stand still and notice one edge of shadow. Frame it so the rest of the room falls away.",
    tips = listOf("Expose for the bright edge."),
    constraint = "No zoom",
    theme = "Indoor",
    tags = listOf("indoor"),
)

private class UseCaseLibrarySource(
    private val items: List<LibraryPrompt>,
) : LibraryPromptSource {
    override suspend fun loadAll(): List<LibraryPrompt> = items
}

private class ScriptedGenerator(
    private val prompts: List<GeneratedPrompt>,
    private val series: List<GeneratedSeries> = emptyList(),
) : PromptGenerator {
    var calls: Int = 0
        private set
    var seriesCalls: Int = 0
        private set
    val requests = mutableListOf<GenerationRequest>()
    val lastRequest: GenerationRequest?
        get() = requests.lastOrNull()

    override suspend fun availability(): Availability = Availability.AVAILABLE

    override suspend fun generate(request: GenerationRequest): Result<GeneratedPrompt> {
        requests += request
        val prompt = prompts[calls.coerceAtMost(prompts.lastIndex)]
        calls += 1
        return Result.success(prompt)
    }

    override suspend fun generateSeries(request: GenerationRequest): Result<GeneratedSeries> {
        if (series.isEmpty()) {
            return Result.failure(UnsupportedOperationException("no series"))
        }
        seriesCalls += 1
        return Result.success(series[(seriesCalls - 1).coerceAtMost(series.lastIndex)])
    }
}

private class FakeDayPrompts : DayPromptRepository {
    private val days = linkedMapOf<LocalDate, DayPrompt>()
    val superseded = mutableListOf<SupersededPrompt>()

    override fun observeDay(date: LocalDate): Flow<DayPrompt?> = flowOf(days[date])

    override fun observeDays(start: LocalDate, endInclusive: LocalDate): Flow<List<DayPrompt>> =
        flowOf(days.values.filter { it.date >= start && it.date <= endInclusive })

    override suspend fun getDay(date: LocalDate): DayPrompt? = days[date]

    override suspend fun upsert(prompt: DayPrompt) {
        days[prompt.date] = prompt
    }

    override suspend fun recordSuperseded(prompt: SupersededPrompt) {
        superseded += prompt
    }

    override suspend fun recentTitles(limit: Int): List<String> = recentDays(limit).map { it.title }

    override suspend fun recentThemes(limit: Int): List<String> = recentDays(limit).map { it.theme }

    override suspend fun recentDays(limit: Int): List<DayPrompt> =
        days.values.sortedByDescending { it.date }.take(limit)

    override suspend fun allDays(): List<DayPrompt> =
        days.values.sortedBy { it.date }

    override suspend fun deleteAfter(date: LocalDate) {
        days.keys.filter { it > date }.forEach { days.remove(it) }
    }

    override fun observeDaysInSeries(seriesId: Long): Flow<List<DayPrompt>> =
        flowOf(days.values.filter { it.seriesId == seriesId }.sortedBy { it.date })

    override suspend fun daysInSeries(seriesId: Long): List<DayPrompt> =
        days.values.filter { it.seriesId == seriesId }.sortedBy { it.date }
}

private class RecordingGamification : GamificationRepository {
    val recorded = mutableListOf<LibraryUsage>()

    override fun observeAchievements(): Flow<List<Achievement>> = flowOf(emptyList())

    override suspend fun unlock(achievement: Achievement) = Unit

    override fun observeStreak(): Flow<StreakState> =
        flowOf(StreakState(0, 0, 0, null))

    override suspend fun updateStreak(state: StreakState) = Unit

    override suspend fun recordLibraryUsage(usage: LibraryUsage) {
        recorded += usage
    }

    override suspend fun libraryUsedSince(libraryId: String, since: LocalDate): Boolean =
        recorded.any { it.libraryId == libraryId && !it.usedOnDate.isBefore(since) }
}

private class TogglePreferences(
    enabled: Boolean = false,
) : PreferencesRepository {
    var enabled: Boolean = enabled
    override fun observeNotifyTime() = flowOf(java.time.LocalTime.of(9, 0))
    override suspend fun setNotifyTime(time: java.time.LocalTime) = Unit
    override fun observePreciseTiming() = flowOf(false)
    override suspend fun setPreciseTiming(enabled: Boolean) = Unit
    override fun observeThemeFocus() = flowOf<String?>(null)
    override suspend fun setThemeFocus(focus: String?) = Unit
    override fun observeSeriesEnabled() = flowOf(enabled)
    override suspend fun setSeriesEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
    override fun observePaused() = flowOf(false)
    override suspend fun setPaused(paused: Boolean) = Unit
    override fun observeOnboardingComplete() = flowOf(true)
    override suspend fun setOnboardingComplete(complete: Boolean) = Unit
    override fun observeDebugUseFakeAi() = flowOf(false)
    override suspend fun setDebugUseFakeAi(useFake: Boolean) = Unit
    override fun observeLastNotifiedDate() = flowOf<LocalDate?>(null)
    override suspend fun setLastNotifiedDate(date: LocalDate?) = Unit
}

private class MemorySeries : SeriesRepository {
    private val rows = linkedMapOf<Long, Series>()
    private var nextId = 1L

    override fun observe(id: Long) = flowOf(rows[id])
    override fun observeCovering(date: LocalDate) = flowOf(
        rows.values.firstOrNull { !date.isBefore(it.startDate) && !date.isAfter(it.endDate) },
    )
    override suspend fun get(id: Long) = rows[id]
    override suspend fun covering(date: LocalDate): Series? =
        rows.values.firstOrNull { !date.isBefore(it.startDate) && !date.isAfter(it.endDate) }
    override suspend fun latest(): Series? = rows.values.maxByOrNull { it.endDate }
    override suspend fun all(): List<Series> = rows.values.sortedBy { it.startDate }
    override suspend fun insert(series: Series): Long {
        val id = if (series.id == 0L) nextId++ else series.id
        rows[id] = series.copy(id = id)
        return id
    }
    override suspend fun update(series: Series) {
        rows[series.id] = series
    }
    override suspend fun delete(id: Long) {
        rows.remove(id)
    }
}

private fun validSeries(): GeneratedSeries {
    val prompts = listOf(
        validPrompt(title = "Hands day 1 light", oneLiner = "Photograph the hands that made breakfast.", theme = "Hands"),
        validPrompt(title = "Hands day 2 light", oneLiner = "Frame knuckles wrapped around a mug.", theme = "Hands"),
        validPrompt(title = "Hands day 3 light", oneLiner = "Catch a shadow a wrist throws at noon.", theme = "Hands"),
        validPrompt(title = "Hands day 4 light", oneLiner = "Study the grip that holds a book open.", theme = "Hands"),
        validPrompt(title = "Hands day 5 light", oneLiner = "Look at soap bubbles on the sink edge.", theme = "Hands"),
        validPrompt(title = "Hands day 6 light", oneLiner = "Watch fingers rest on a windowsill.", theme = "Hands"),
        validPrompt(title = "Hands day 7 light", oneLiner = "Find the last light on your palms.", theme = "Hands"),
    )
    return GeneratedSeries(title = "A Week of Hands", theme = "Hands", prompts = prompts)
}
