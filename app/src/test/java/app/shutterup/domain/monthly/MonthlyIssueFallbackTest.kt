package app.shutterup.domain.monthly

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyIssueFallbackTest {
    private val validator = app.shutterup.domain.ai.MonthlyIssueValidator()

    @Test
    fun fullMonth_twoThemesAndNotes() {
        val copy = MonthlyIssueFallback.compose(
            snapshot(
                completed = 24,
                themes = listOf("Reflections" to 14, "Looking up" to 6, "Low light" to 4),
                notes = 9,
                longestRun = 11,
            ),
        )
        assertEquals("Reflections and looking up.", copy.headline)
        assertEquals(
            "Twenty-four days, mostly reflections and looking up. You wrote on nine of them. The longest stretch was eleven days.",
            copy.body,
        )
        assertValid(copy)
    }

    @Test
    fun sparseMonth_fewDaysNoNotes() {
        val copy = MonthlyIssueFallback.compose(
            snapshot(
                completed = 3,
                themes = listOf("Reflections" to 2, "Quiet hours" to 1),
                notes = 0,
                longestRun = 1,
            ),
        )
        assertEquals("Reflections and quiet hours.", copy.headline)
        assertEquals("Three days, mostly reflections and quiet hours. A small set, held still.", copy.body)
        assertValid(copy)
    }

    @Test
    fun singleTheme_returnsToTheSameLooking() {
        val copy = MonthlyIssueFallback.compose(
            snapshot(
                completed = 18,
                themes = listOf("Reflections" to 18),
                notes = 2,
                longestRun = 7,
            ),
        )
        assertEquals("Reflections.", copy.headline)
        assertEquals(
            "Eighteen days of reflections. You wrote on two of them. You kept returning to the same kind of looking.",
            copy.body,
        )
        assertValid(copy)
    }

    @Test
    fun noteHeavy_mentionsWritingWithoutQuoting() {
        val copy = MonthlyIssueFallback.compose(
            snapshot(
                completed = 12,
                themes = listOf("Light" to 7, "Glass" to 5),
                notes = 11,
                longestRun = 4,
                noteText = "the glass in the stairwell again",
            ),
        )
        assertEquals("Light and glass.", copy.headline)
        assertEquals(
            "Twelve days, mostly light and glass. You wrote through most of the month.",
            copy.body,
        )
        assertFalse(copy.body.contains("stairwell"))
        assertValid(copy, notes = listOf("the glass in the stairwell again"))
    }

    @Test
    fun singleDay_withNote() {
        val copy = MonthlyIssueFallback.compose(
            snapshot(
                completed = 1,
                themes = listOf("Reflections" to 1),
                notes = 1,
                longestRun = 1,
            ),
        )
        assertEquals("A quiet month.", copy.headline)
        assertEquals("One day of reflections. You wrote a note that day.", copy.body)
        assertValid(copy)
    }

    @Test
    fun singleDay_withoutNote() {
        val copy = MonthlyIssueFallback.compose(
            snapshot(
                completed = 1,
                themes = listOf("Hands" to 1),
                notes = 0,
                longestRun = 1,
            ),
        )
        assertEquals("A quiet month.", copy.headline)
        assertEquals("One day of hands. The rest of the month is paper.", copy.body)
        assertValid(copy)
    }

    @Test
    fun fullMonthEveryDayShot_everyDayWritten() {
        val copy = MonthlyIssueFallback.compose(
            snapshot(
                completed = 30,
                themes = listOf("Looking up" to 16, "Reflections" to 14),
                notes = 30,
                longestRun = 30,
            ),
        )
        assertEquals("Looking up and reflections.", copy.headline)
        assertTrue(copy.body.startsWith("Thirty days, mostly looking up and reflections."))
        assertTrue(copy.body.contains("You wrote on every day you shot."))
        assertTrue(copy.body.contains("The longest stretch was thirty days."))
        assertValid(copy)
    }

    @Test
    fun twoDays_apart() {
        val copy = MonthlyIssueFallback.compose(
            snapshot(
                completed = 2,
                themes = listOf("Steam" to 2),
                notes = 0,
                longestRun = 1,
            ),
        )
        assertEquals("A quiet month.", copy.headline)
        assertEquals("Two days of steam. A small set, held still.", copy.body)
        assertValid(copy)
    }

    @Test
    fun longHeadlineFallsBackToPrimaryTheme() {
        val headline = MonthlyIssueFallback.headlineFor(
            completedCount = 20,
            themes = listOf("Negative space", "Kitchen still life"),
        )
        assertTrue(headline.length <= 30)
        assertEquals("Negative space.", headline)
    }

    @Test
    fun allFixturesPassTheValidator() {
        val fixtures = listOf(
            snapshot(24, listOf("Reflections" to 14, "Looking up" to 6), 9, 11),
            snapshot(3, listOf("Reflections" to 2, "Quiet hours" to 1), 0, 1),
            snapshot(18, listOf("Reflections" to 18), 2, 7),
            snapshot(12, listOf("Light" to 7, "Glass" to 5), 11, 4),
            snapshot(1, listOf("Reflections" to 1), 1, 1),
            snapshot(8, listOf("Hands" to 5, "Windows" to 3), 0, 3),
            snapshot(15, listOf("Low light" to 15), 0, 8),
        )
        for (snap in fixtures) {
            val copy = MonthlyIssueFallback.compose(snap)
            assertValid(copy, notes = snap.notes)
            assertFalse(copy.headline.contains("!"))
            assertFalse(copy.body.contains("!"))
            assertFalse(copy.body.contains("missed", ignoreCase = true))
        }
    }

    private fun snapshot(
        completed: Int,
        themes: List<Pair<String, Int>>,
        notes: Int,
        longestRun: Int,
        noteText: String? = "a short observation",
    ): MonthlyIssueSnapshot {
        val days = ArrayList<CompletedDayForIssue>()
        var remainingNotes = notes
        var date = LocalDate.of(2026, 9, 1)
        val expanded = themes.flatMap { (theme, count) -> List(count) { theme } }
        check(expanded.size == completed)
        for (index in 0 until completed) {
            val note = if (remainingNotes > 0) {
                remainingNotes -= 1
                noteText
            } else {
                null
            }
            days += CompletedDayForIssue(
                date = date,
                title = "Title $index",
                theme = expanded[index],
                note = note,
            )
            date = date.plusDays(1)
        }
        return MonthlyIssueSnapshot(
            yearMonth = YearMonth.of(2026, 9),
            days = days,
            rankedThemes = ThemeRanking.rank(days.map { it.theme }),
            longestRun = longestRun,
        )
    }

    private fun assertValid(copy: MonthlyIssueCopy, notes: List<String> = emptyList()) {
        val result = validator.validate(copy, notes)
        assertEquals("expected Valid for '${copy.headline}' / '${copy.body}', was $result", app.shutterup.domain.ai.ValidationResult.Valid, result)
    }
}
