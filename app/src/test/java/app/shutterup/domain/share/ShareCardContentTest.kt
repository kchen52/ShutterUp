package app.shutterup.domain.share

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ShareCardContentTest {

    @Test
    fun kicker_nineteenthSeptemberReflections() {
        val content = ShareCardContent.from(
            date = LocalDate.of(2026, 9, 19),
            theme = "Reflections",
            title = "Find the sky in a puddle",
        )
        assertEquals("19 SEPTEMBER", content.dateLabel)
        assertEquals("REFLECTIONS", content.themeLabel)
        assertEquals("19 SEPTEMBER · REFLECTIONS", content.kicker)
        assertEquals("Find the sky in a puddle", content.title)
        assertEquals("Reflections", content.theme)
    }

    @Test
    fun dateLabel_doesNotZeroPadTheDay() {
        val content = ShareCardContent.from(
            date = LocalDate.of(2026, 1, 5),
            theme = "Quiet hours",
            title = "Steam on a window",
        )
        assertEquals("5 JANUARY", content.dateLabel)
        assertEquals("5 JANUARY · QUIET HOURS", content.kicker)
    }

    @Test
    fun kicker_omitsBlankTheme() {
        val content = ShareCardContent.from(
            date = LocalDate.of(2026, 9, 19),
            theme = "  ",
            title = "Find the sky in a puddle",
        )
        assertEquals("19 SEPTEMBER", content.kicker)
    }

    @Test
    fun title_isPassedThroughUnchanged() {
        val title = "Find the last remaining scrap of sky in a puddle"
        val content = ShareCardContent.from(
            date = LocalDate.of(2026, 9, 19),
            theme = "Reflections",
            title = title,
        )
        assertEquals(title, content.title)
    }
}
