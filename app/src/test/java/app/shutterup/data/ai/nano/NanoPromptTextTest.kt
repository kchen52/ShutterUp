package app.shutterup.data.ai.nano

import app.shutterup.domain.ai.GenerationRequest
import app.shutterup.domain.ai.PromptSource
import app.shutterup.domain.ai.Season
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NanoPromptTextTest {
    private val request = GenerationRequest(
        date = LocalDate.of(2026, 9, 19),
        themeFocus = "my dog",
        recentTitles = listOf("Puddle Sky", "Corner Light"),
        recentThemes = listOf("Reflections"),
        dayOfWeek = DayOfWeek.SATURDAY,
        season = Season.AUTUMN,
    )

    @Test
    fun systemPrompt_encodesFocusRecentsAndRules() {
        val text = NanoPromptText.systemPrompt(request)
        assertTrue(text.contains("my dog"))
        assertTrue(text.contains("Puddle Sky") && text.contains("Corner Light"))
        assertTrue(text.contains("Reflections"))
        assertTrue(text.contains("30 minutes"))
        assertTrue(text.contains("No emojis"))
        assertTrue(text.contains("AUTUMN"))
    }

    @Test
    fun systemPrompt_autoThemeWhenFocusBlank() {
        val text = NanoPromptText.systemPrompt(request.copy(themeFocus = null, recentTitles = emptyList(), recentThemes = emptyList()))
        assertTrue(text.contains("Auto-generate"))
        assertTrue(text.contains("no recent titles"))
    }

    @Test
    fun map_copiesFieldsAndMarksAiSource() {
        val output = NanoPromptOutput(
            title = "Find the sky in a puddle",
            oneLiner = "Turn the world upside down with a reflection.",
            details = "Look down after rain. Frame sky only.",
            tips = listOf("Tap to focus on the reflection."),
            constraint = "No rotation afterwards.",
            theme = "Reflections",
        )
        val mapped = NanoPromptText.map(output, "nano-v2")
        assertEquals("Find the sky in a puddle", mapped.title)
        assertEquals(listOf("Tap to focus on the reflection."), mapped.tips)
        assertEquals(PromptSource.ON_DEVICE_AI, mapped.source)
        assertEquals("nano-v2", mapped.modelName)
    }
}
