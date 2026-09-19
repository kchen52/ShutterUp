package app.shutterup.domain.ai

import app.shutterup.domain.model.Achievement
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.LibraryUsage
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.model.SupersededPrompt
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.GamificationRepository
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

    private fun useCase(
        primary: PromptGenerator,
        librarySource: LibraryPromptSource = UseCaseLibrarySource(listOf(libraryEntry("fallback", "Library Frame"))),
        prompts: FakeDayPrompts = FakeDayPrompts(),
        gamification: RecordingGamification = RecordingGamification(),
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
) : PromptGenerator {
    var calls: Int = 0
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
