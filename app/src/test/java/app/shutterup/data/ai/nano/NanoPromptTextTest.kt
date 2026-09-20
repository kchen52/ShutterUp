package app.shutterup.data.ai.nano

import app.shutterup.domain.ai.GenerationRequest
import app.shutterup.domain.ai.MonthlyIssueRequest
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
    fun seriesSystemPrompt_asksForSevenRelatedPrompts() {
        val text = NanoPromptText.seriesSystemPrompt(request)
        assertTrue(text.contains("seven-day"))
        assertTrue(text.contains("my dog"))
        assertTrue(text.contains("At least four of the seven must work indoors"))
        assertTrue(text.contains("not restate"))
    }

    @Test
    fun systemPrompt_staysInSeriesWhenRerolling() {
        val text = NanoPromptText.systemPrompt(request.copy(seriesTitle = "A Week of Hands"))
        assertTrue(text.contains("A Week of Hands"))
        assertTrue(text.contains("Stay within that series"))
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

    @Test
    fun mapSeries_copiesTitleAndSevenPrompts() {
        val prompt = NanoPromptOutput(
            title = "Hands at breakfast",
            oneLiner = "Photograph the hands that made breakfast.",
            details = "Watch the smallest gestures at the table. Frame only the hands.",
            tips = listOf("Get close."),
            constraint = "No zoom",
            theme = "Hands",
        )
        val mapped = NanoPromptText.mapSeries(
            NanoSeriesOutput(
                title = "A Week of Hands",
                theme = "Hands",
                prompts = List(7) { index -> prompt.copy(title = "Hands day ${index + 1}") },
            ),
            "nano-v2",
        )
        assertEquals("A Week of Hands", mapped.title)
        assertEquals("Hands", mapped.theme)
        assertEquals(7, mapped.prompts.size)
        assertEquals("Hands day 1", mapped.prompts.first().title)
        assertEquals("nano-v2", mapped.prompts.first().modelName)
    }

    @Test
    fun monthlySystemPrompt_isTextOnlyAndForbidsVisualClaims() {
        val request = MonthlyIssueRequest(
            monthLabel = "September 2026",
            completedCount = 24,
            titles = listOf("Puddle sky", "Ceiling lamp"),
            themeCounts = listOf("Reflections" to 14, "Looking up" to 6),
            notes = listOf("the glass in the stairwell again"),
            longestRun = 11,
        )
        val text = NanoPromptText.monthlySystemPrompt(request)
        assertTrue(text.contains("September 2026"))
        assertTrue(text.contains("Puddle sky"))
        assertTrue(text.contains("Reflections (14)"))
        assertTrue(text.contains("the glass in the stairwell again"))
        assertTrue(text.contains("cannot see the photographs"))
        assertTrue(text.contains("never quote"))
        assertTrue(text.contains("Do not re-list"))
    }

    @Test
    fun mapMonthly_copiesHeadlineAndBody() {
        val mapped = NanoPromptText.mapMonthly(
            NanoMonthlyOutput(
                headline = "Light and glass.",
                body = "You looked up more than usual. Twenty-four days, mostly reflections.",
            ),
            "nano-v2",
        )
        assertEquals("Light and glass.", mapped.headline)
        assertEquals("nano-v2", mapped.modelName)
        assertEquals(
            "You looked up more than usual. Twenty-four days, mostly reflections.",
            mapped.body,
        )
    }
}
