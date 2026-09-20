package app.shutterup.domain.monthly

import app.shutterup.domain.ai.MonthlyIssueValidator
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyIssueFallbackTest {
    private val validator = MonthlyIssueValidator()

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
        assertEquals("Glass looking back.", copy.headline)
        assertEquals(
            "Twenty-four days. You wrote on nine of them, and the longest stretch was eleven.",
            copy.body,
        )
        assertBodyDoesNotRepeatThemes(copy, listOf("Reflections", "Looking up", "Low light"))
        assertValid(copy, themes = listOf("Reflections", "Looking up"))
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
        assertEquals("Glass looking back.", copy.headline)
        assertEquals("Three days. A small set, held still.", copy.body)
        assertBodyDoesNotRepeatThemes(copy, listOf("Reflections", "Quiet hours"))
        assertValid(copy, themes = listOf("Reflections", "Quiet hours"))
    }

    @Test
    fun singleTheme_shapeWithoutRepeatingIt() {
        val copy = MonthlyIssueFallback.compose(
            snapshot(
                completed = 18,
                themes = listOf("Reflections" to 18),
                notes = 2,
                longestRun = 7,
            ),
        )
        assertEquals("Glass looking back.", copy.headline)
        assertEquals(
            "Eighteen days. You wrote on two of them, and the longest stretch was seven.",
            copy.body,
        )
        assertBodyDoesNotRepeatThemes(copy, listOf("Reflections"))
        assertValid(copy, themes = listOf("Reflections"))
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
        assertEquals("Light.", copy.headline)
        assertEquals("Twelve days. You wrote through most of the month.", copy.body)
        assertFalse(copy.body.contains("stairwell"))
        assertBodyDoesNotRepeatThemes(copy, listOf("Light", "Glass"))
        assertValid(copy, notes = listOf("the glass in the stairwell again"), themes = listOf("Light", "Glass"))
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
        assertEquals("Glass looking back.", copy.headline)
        assertEquals("One day. You wrote a note that day.", copy.body)
        assertBodyDoesNotRepeatThemes(copy, listOf("Reflections"))
        assertValid(copy, themes = listOf("Reflections"))
    }

    @Test
    fun singleDay_withoutNote_unknownTheme() {
        val copy = MonthlyIssueFallback.compose(
            snapshot(
                completed = 1,
                themes = listOf("Hands" to 1),
                notes = 0,
                longestRun = 1,
            ),
        )
        assertEquals("Hands.", copy.headline)
        assertEquals("One day. The rest of the month is paper.", copy.body)
        assertBodyDoesNotRepeatThemes(copy, listOf("Hands"))
        assertValid(copy, themes = listOf("Hands"))
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
        assertEquals("Above your head.", copy.headline)
        assertEquals("Thirty days. You wrote on every day you shot.", copy.body)
        assertBodyDoesNotRepeatThemes(copy, listOf("Looking up", "Reflections"))
        assertValid(copy, themes = listOf("Looking up", "Reflections"))
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
        assertEquals("Steam.", copy.headline)
        assertEquals("Two days. A small set, held still.", copy.body)
        assertBodyDoesNotRepeatThemes(copy, listOf("Steam"))
        assertValid(copy, themes = listOf("Steam"))
    }

    @Test
    fun previousMonth_moreDays_addsAQuietComparison() {
        val copy = MonthlyIssueFallback.compose(
            snapshot(
                completed = 24,
                themes = listOf("Reflections" to 24),
                notes = 9,
                longestRun = 11,
                previous = 18,
            ),
        )
        assertEquals(
            "Twenty-four days. You wrote on nine of them, and the longest stretch was eleven. A little more than the month before.",
            copy.body,
        )
        assertBodyDoesNotRepeatThemes(copy, listOf("Reflections"))
        assertValid(copy, themes = listOf("Reflections"))
    }

    @Test
    fun previousMonth_fewerDays_doesNotMentionTheDrop() {
        val copy = MonthlyIssueFallback.compose(
            snapshot(
                completed = 8,
                themes = listOf("Shadows" to 8),
                notes = 0,
                longestRun = 3,
                previous = 20,
            ),
        )
        assertEquals("Eight days. The days sat a little apart.", copy.body)
        assertFalse(copy.body.contains("month before"))
        assertBodyDoesNotRepeatThemes(copy, listOf("Shadows"))
        assertValid(copy, themes = listOf("Shadows"))
    }

    @Test
    fun unknownLongTheme_fallsBackToGenericHeadline() {
        val headline = MonthlyIssueFallback.headlineFor(
            YearMonth.of(2026, 9),
            listOf("A very long invented theme name"),
        )
        assertTrue(headline.length <= 30)
        assertEquals("A month of looking.", headline)
    }

    @Test
    fun allFixturesPassTheValidatorWithoutRepeatingThemes() {
        val fixtures = listOf(
            snapshot(24, listOf("Reflections" to 14, "Looking up" to 6, "Low light" to 4), 9, 11),
            snapshot(3, listOf("Reflections" to 2, "Quiet hours" to 1), 0, 1),
            snapshot(18, listOf("Reflections" to 18), 2, 7),
            snapshot(12, listOf("Light" to 7, "Glass" to 5), 11, 4),
            snapshot(1, listOf("Reflections" to 1), 1, 1),
            snapshot(8, listOf("Hands" to 5, "Windows" to 3), 0, 3),
            snapshot(15, listOf("Low light" to 15), 0, 8),
        )
        for (snap in fixtures) {
            val copy = MonthlyIssueFallback.compose(snap)
            val themes = snap.rankedThemes.map { it.theme }
            assertValid(copy, notes = snap.notes, themes = themes)
            assertBodyDoesNotRepeatThemes(copy, themes)
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
        previous: Int? = null,
        yearMonth: YearMonth = YearMonth.of(2026, 9),
    ): MonthlyIssueSnapshot {
        val days = ArrayList<CompletedDayForIssue>()
        var remainingNotes = notes
        var date = yearMonth.atDay(1)
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
            yearMonth = yearMonth,
            days = days,
            rankedThemes = ThemeRanking.rank(days.map { it.theme }),
            longestRun = longestRun,
            previousCompletedCount = previous,
        )
    }

    private fun assertBodyDoesNotRepeatThemes(copy: MonthlyIssueCopy, themes: List<String>) {
        val body = copy.body.lowercase()
        for (theme in themes) {
            assertFalse(
                "body restates theme '$theme': '${copy.headline}' / '${copy.body}'",
                body.contains(theme.lowercase()),
            )
        }
    }

    private fun assertValid(
        copy: MonthlyIssueCopy,
        notes: List<String> = emptyList(),
        themes: List<String> = emptyList(),
    ) {
        val result = validator.validate(copy, notes, themes)
        assertEquals(
            "expected Valid for '${copy.headline}' / '${copy.body}', was $result",
            app.shutterup.domain.ai.ValidationResult.Valid,
            result,
        )
    }
}

class MonthlyIssueHeadlinesTest {
    private val validator = MonthlyIssueValidator()

    @Test
    fun libraryThemes_haveTwoValidVariants() {
        assertEquals(45, MonthlyIssueHeadlines.BANK.size)
        for ((theme, variants) in MonthlyIssueHeadlines.BANK) {
            assertEquals("two variants for $theme", 2, variants.size)
            for (headline in variants) {
                assertTrue("$headline too long", headline.length <= MonthlyIssueValidator.HEADLINE_MAX)
                assertTrue("$headline should end with a period", headline.endsWith('.'))
                assertTrue("$headline should be sentence case", headline.first().isUpperCase())
                assertFalse(headline.contains('!'))
                val copy = MonthlyIssueCopy(
                    headline = headline,
                    body = "Twenty-four days. You wrote on nine of them.",
                )
                assertEquals(
                    "expected Valid for $headline, was ${validator.validate(copy)}",
                    app.shutterup.domain.ai.ValidationResult.Valid,
                    validator.validate(copy),
                )
            }
        }
    }

    @Test
    fun providedRegister_isTheFirstVariant() {
        assertEquals("Second skies.", MonthlyIssueHeadlines.phraseFor("Reflections", 0))
        assertEquals("Early and low.", MonthlyIssueHeadlines.phraseFor("Morning Light", 0))
        assertEquals("Mostly air.", MonthlyIssueHeadlines.phraseFor("Negative Space", 0))
        assertEquals("After dark.", MonthlyIssueHeadlines.phraseFor("Night Lights", 0))
    }

    @Test
    fun consecutiveMonths_alternateVariants() {
        val september = MonthlyIssueHeadlines.phraseFor("Reflections", YearMonth.of(2026, 9))
        val october = MonthlyIssueHeadlines.phraseFor("Reflections", YearMonth.of(2026, 10))
        assertEquals("Glass looking back.", september)
        assertEquals("Second skies.", october)
        assertNotEquals(september, october)
    }

    @Test
    fun unknownTheme_usesTheBarePhrase() {
        assertEquals("Steam.", MonthlyIssueHeadlines.phraseFor("Steam", YearMonth.of(2026, 9)))
        assertEquals("Quiet hours.", MonthlyIssueHeadlines.phraseFor("quiet hours", YearMonth.of(2026, 4)))
    }

    @Test
    fun lookingUp_matchesLibraryKeyRegardlessOfCase() {
        val phrase = MonthlyIssueHeadlines.phraseFor("Looking up", YearMonth.of(2026, 9))
        assertEquals("Above your head.", phrase)
    }
}
