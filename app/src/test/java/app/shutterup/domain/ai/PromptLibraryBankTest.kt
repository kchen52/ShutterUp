package app.shutterup.domain.ai

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

    @Test
    fun bundledLibrary_meetsLibraryOnlyContract() {
        val json = libraryFile().readText()
        val prompts = PromptParser.parseLibrary(json).getOrThrow()
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

    private fun libraryFile(): File {
        val candidates = listOf(
            File("src/main/assets/prompt_library.json"),
            File("app/src/main/assets/prompt_library.json"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("prompt_library.json not found from ${File(".").canonicalPath}")
    }
}
