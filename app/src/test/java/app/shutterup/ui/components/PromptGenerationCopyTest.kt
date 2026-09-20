package app.shutterup.ui.components

import app.shutterup.domain.ai.GenerationProgress
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PromptGenerationCopyTest {
    @Test
    fun weekdayCopy_usesFullEnglishDayName() {
        val saturday = LocalDate.of(2024, 6, 15)
        assertEquals("Generating prompts for Saturday", PromptGenerationCopy.forDate(saturday))
        assertEquals(
            "Generating prompts for Saturday",
            PromptGenerationCopy.message(GenerationProgress(saturday)),
        )
    }

    @Test
    fun seriesCopy_isQuietAndSpecific() {
        val saturday = LocalDate.of(2024, 6, 15)
        assertEquals(
            "Generating prompts for a seven-day series",
            PromptGenerationCopy.message(GenerationProgress(saturday, series = true)),
        )
    }
}
