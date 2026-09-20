package app.shutterup.ui.issue

import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class IssueKickerTest {
    @Test
    fun pageKickerMatchesWireframe() {
        assertEquals("SEPTEMBER · 24 DAYS", issueKicker(YearMonth.of(2026, 9), 24))
        assertEquals("SEPTEMBER · 1 DAY", issueKicker(YearMonth.of(2026, 9), 1))
    }

    @Test
    fun listKickerIncludesYear() {
        assertEquals("SEPTEMBER 2026 · 24 DAYS", issueListKicker(YearMonth.of(2026, 9), 24))
    }

    @Test
    fun themesKickerUppercasesAndJoins() {
        assertEquals("REFLECTIONS · LOOKING UP", issueThemesKicker(listOf("Reflections", "Looking up")))
    }
}
