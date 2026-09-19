package app.shutterup.domain.ai

import app.shutterup.domain.model.Achievement
import app.shutterup.domain.model.LibraryUsage
import app.shutterup.domain.model.StreakState
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryPromptGeneratorTest {

    private val date = LocalDate.of(2024, 9, 19)
    private val clock = Clock.fixed(Instant.parse("2024-09-19T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun excludesIdsUsedInLast180Days() = runBlocking {
        val used = lib("used", "Window Steam", tags = listOf("indoor"))
        val fresh = lib("fresh", "Sink Mirror", tags = listOf("indoor"))
        val gamification = FakeGamification(
            usedSince = setOf("used"),
        )
        val generator = LibraryPromptGenerator(
            source = GeneratorLibrarySource(listOf(used, fresh)),
            gamification = gamification,
            clock = clock,
            random = Random(0),
        )
        val picked = generator.pick(request())
        assertEquals("fresh", picked.id)
    }

    @Test
    fun prefersTagMatchingThemeFocus() = runBlocking {
        val urban = lib("urban", "Street Lines", tags = listOf("architecture", "city"))
        val canine = lib("canine", "Paw Height", tags = listOf("dog", "pet"))
        val generator = LibraryPromptGenerator(
            source = GeneratorLibrarySource(listOf(urban, canine)),
            gamification = FakeGamification(),
            clock = clock,
            random = Random(99),
        )
        val picked = generator.pick(request(themeFocus = "my dog"))
        assertEquals("canine", picked.id)
    }

    @Test
    fun stillReturnsWhenEverythingIsExhausted() = runBlocking {
        val a = lib("a", "Kettle Plume", tags = listOf("steam"))
        val b = lib("b", "Fridge Glow", tags = listOf("night"))
        val generator = LibraryPromptGenerator(
            source = GeneratorLibrarySource(listOf(a, b)),
            gamification = FakeGamification(usedSince = setOf("a", "b")),
            clock = clock,
            random = Random(3),
        )
        val picked = generator.pick(request())
        assertTrue(picked.id == "a" || picked.id == "b")
        val generated = generator.generate(request()).getOrThrow()
        assertEquals(PromptSource.LIBRARY, generated.source)
        assertNotNull(generated.title)
    }

    @Test
    fun sameSeedPicksTheSameEntry() = runBlocking {
        val items = listOf(
            lib("one", "Spice Dust", tags = listOf("light")),
            lib("two", "Tap Motion", tags = listOf("water")),
            lib("three", "Sock Hills", tags = listOf("fabric")),
        )
        val first = LibraryPromptGenerator(
            source = GeneratorLibrarySource(items),
            gamification = FakeGamification(),
            clock = clock,
            random = Random(42),
        ).pick(request())
        val second = LibraryPromptGenerator(
            source = GeneratorLibrarySource(items),
            gamification = FakeGamification(),
            clock = clock,
            random = Random(42),
        ).pick(request())
        assertEquals(first.id, second.id)
    }

    private fun request(themeFocus: String? = null) = GenerationRequest(
        date = date,
        themeFocus = themeFocus,
        recentTitles = emptyList(),
        recentThemes = emptyList(),
        dayOfWeek = date.dayOfWeek,
        season = seasonForDate(date),
    )
}

private fun lib(
    id: String,
    title: String,
    tags: List<String>,
    theme: String = "Indoor",
): LibraryPrompt = LibraryPrompt(
    id = id,
    title = title,
    oneLiner = "Look at ordinary light on a nearby surface today.",
    details = "Stand still and notice one edge of shadow. Frame it so the rest of the room falls away.",
    tips = listOf("Expose for the bright edge."),
    constraint = "No zoom",
    theme = theme,
    tags = tags,
)

private class GeneratorLibrarySource(
    private val items: List<LibraryPrompt>,
) : LibraryPromptSource {
    override suspend fun loadAll(): List<LibraryPrompt> = items
}

private class FakeGamification(
    private val usedSince: Set<String> = emptySet(),
) : GamificationRepository {
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
        libraryId in usedSince
}
