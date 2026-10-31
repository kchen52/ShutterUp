package app.shutterup.domain.ai

import app.shutterup.domain.monthly.MonthlyIssueHeadlines
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptLibraryBankTest {

    private val blockedTerms = setOf(
        "weapon", "gun", "rifle", "knife", "nudity", "nude", "naked", "trespass",
        "trespassing", "stranger", "child", "children", "kid", "baby", "roof",
        "rooftop", "cliff", "driving", "drugged", "alcohol",
    )
    private val validator = PromptValidator(blockedTerms, DEFAULT_GEAR_TERMS)
    private val prompts: List<LibraryPrompt> by lazy {
        PromptParser.parseLibrary(libraryFile().readText()).getOrThrow()
    }

    @Test
    fun bundledLibrary_meetsLibraryOnlyContract() {
        assertEquals("expected about 2000 bundled prompts", 2000, prompts.size)
        assertEquals(prompts.size, prompts.map { it.id }.distinct().size)
        assertEquals(prompts.size, prompts.map { it.title.lowercase() }.distinct().size)

        val byTheme = prompts.groupBy { it.theme }
        assertTrue("need 40+ themes for Series, had ${byTheme.size}", byTheme.size >= 40)
        byTheme.forEach { (theme, items) ->
            assertTrue("$theme is longer than 24 characters", theme.length <= 24)
            assertTrue("$theme has ${items.size} prompts, need at least 7", items.size >= 7)
        }

        val indoor = prompts.count { prompt ->
            prompt.tags.any { it.equals("indoor", ignoreCase = true) }
        }
        assertTrue("indoor share was $indoor/${prompts.size}", indoor * 2 >= prompts.size)

        prompts.forEach { prompt ->
            val generated = prompt.toGeneratedPrompt()
            val result = validator.validate(
                generated,
                recentTitles = emptyList(),
                recentThemeLedes = emptyList(),
            )
            assertEquals(
                "library ${prompt.id} (${prompt.title}) failed validation: $result",
                ValidationResult.Valid,
                result,
            )
        }
    }

    @Test
    fun everyPrompt_isEitherIndoorOrOutdoor() {
        prompts.forEach { prompt ->
            val placement = prompt.tags.filter { it == "indoor" || it == "outdoor" }
            assertEquals("${prompt.id} placement tags $placement", 1, placement.size)
        }
    }

    @Test
    fun prompts_areNotStampedFromATemplate() {
        val sentences = prompts
            .flatMap { prompt -> prompt.details.split(Regex("(?<=[.!?])\\s+")) }
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
        sentences.forEach { (sentence, count) ->
            assertTrue("details sentence used $count times: $sentence", count <= 3)
        }
        val tips = prompts.flatMap { it.tips }.map { it.trim().lowercase() }.groupingBy { it }.eachCount()
        tips.forEach { (tip, count) -> assertTrue("tip used $count times: $tip", count <= 5) }
    }

    @Test
    fun sameThemeLedes_doNotRepeat() {
        prompts.groupBy { it.theme }.forEach { (theme, items) ->
            val ledes = items.map { prompt ->
                prompt.oneLiner.lowercase().split(Regex("\\s+")).take(4).joinToString(" ")
            }
            assertEquals("$theme repeats a one-liner opening", ledes.size, ledes.distinct().size)
        }
    }

    @Test
    fun everyTheme_hasMonthlyHeadlines() {
        val themes = prompts.map { it.theme.lowercase() }.toSet()
        assertEquals(themes, MonthlyIssueHeadlines.BANK.keys)
    }

    private fun libraryFile(): File {
        val candidates = listOf(
            File("src/main/assets/prompt_library.json"),
            File("app/src/main/assets/prompt_library.json"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("prompt_library.json not found from ${File(".").canonicalPath}")
    }
}
