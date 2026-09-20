package app.shutterup.domain.ai

import app.shutterup.domain.monthly.MonthlyIssueCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyIssueValidatorTest {
    private val validator = MonthlyIssueValidator()

    @Test
    fun acceptsWarmCopy() {
        val result = validator.validate(
            MonthlyIssueCopy(
                headline = "Light and glass.",
                body = "You looked up more than usual, and you kept going through a grey week. Twenty-four days, and you wrote on nine of them.",
            ),
        )
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun rejectsHeadlineTooLong() {
        val result = validator.validate(
            goodCopy(headline = "Reflections and looking up twice."),
        )
        assertInvalid(result, "headline length")
    }

    @Test
    fun rejectsHeadlineWithoutPeriod() {
        val result = validator.validate(goodCopy(headline = "Light and glass"))
        assertInvalid(result, "headline period")
    }

    @Test
    fun rejectsHeadlineLowercase() {
        val result = validator.validate(goodCopy(headline = "light and glass."))
        assertInvalid(result, "headline case")
    }

    @Test
    fun rejectsOneSentenceBody() {
        val result = validator.validate(goodCopy(body = "Only one sentence lives here."))
        assertInvalid(result, "body sentence count")
    }

    @Test
    fun rejectsFourSentenceBody() {
        val result = validator.validate(goodCopy(body = "One. Two. Three. Four."))
        assertInvalid(result, "body sentence count")
    }

    @Test
    fun rejectsExclamation() {
        val result = validator.validate(goodCopy(body = "You looked up more than usual. What a month!"))
        assertInvalid(result, "exclamation")
    }

    @Test
    fun rejectsEmoji() {
        val smile = "\u263A"
        val result = validator.validate(goodCopy(headline = "Warm $smile light."))
        assertInvalid(result, "emoji")
    }

    @Test
    fun rejectsHashtag() {
        val result = validator.validate(goodCopy(body = "You kept to #reflections this month. The days added up."))
        assertInvalid(result, "hashtag")
    }

    @Test
    fun rejectsUrl() {
        val result = validator.validate(
            goodCopy(body = "See https://example.com for the month. Then put the phone down."),
        )
        assertInvalid(result, "url")
    }

    @Test
    fun rejectsPraise() {
        val result = validator.validate(
            goodCopy(body = "Great work this month on reflections. You kept a quiet pace."),
        )
        assertInvalid(result, "praise")
    }

    @Test
    fun rejectsAmazingMonth() {
        val result = validator.validate(
            goodCopy(body = "An amazing month of looking. You wrote on nine of them."),
        )
        assertInvalid(result, "praise")
    }

    @Test
    fun rejectsGuiltAboutMissedDays() {
        val result = validator.validate(
            goodCopy(body = "You missed several days in the middle. The rest held together."),
        )
        assertInvalid(result, "guilt")
    }

    @Test
    fun rejectsInventedVisualDetail() {
        val result = validator.validate(
            goodCopy(body = "Your photos show a lot of glass and sky. You kept looking up."),
        )
        assertInvalid(result, "visual detail")
    }

    @Test
    fun rejectsVerbatimNote() {
        val note = "the glass in the stairwell again"
        val result = validator.validate(
            goodCopy(body = "You went back to the glass in the stairwell again. The month stayed close to home."),
            notes = listOf(note),
        )
        assertInvalid(result, "verbatim note")
    }

    @Test
    fun acceptsAllusionWithoutQuotingTheNote() {
        val result = validator.validate(
            goodCopy(body = "You kept returning to glass. Nine notes sit beside the days."),
            notes = listOf("the glass in the stairwell again"),
        )
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun rejectsBodyThatRestatesDominantThemes() {
        val result = validator.validate(
            goodCopy(body = "Twenty-four days, mostly reflections. You wrote on nine of them."),
            themes = listOf("Reflections", "Looking up"),
        )
        assertInvalid(result, "theme restatement")
    }

    @Test
    fun acceptsBodyThatDoesNotNameTheThemes() {
        val result = validator.validate(
            goodCopy(),
            themes = listOf("Reflections", "Looking up"),
        )
        assertEquals(ValidationResult.Valid, result)
    }

    private fun goodCopy(
        headline: String = "Light and glass.",
        body: String = "You looked up more than usual, and you kept going through a grey week. Twenty-four days, and you wrote on nine of them.",
    ) = MonthlyIssueCopy(headline, body)

    private fun assertInvalid(result: ValidationResult, token: String) {
        assertTrue("expected Invalid, was $result", result is ValidationResult.Invalid)
        val reason = (result as ValidationResult.Invalid).reason
        assertTrue("reason should mention $token: $reason", reason.contains(token, ignoreCase = true))
    }
}
