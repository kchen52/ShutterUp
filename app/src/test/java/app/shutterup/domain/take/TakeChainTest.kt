package app.shutterup.domain.take

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.PromptSourceRef
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TakeChainTest {
    private val a = LocalDate.of(2026, 6, 28)
    private val b = LocalDate.of(2026, 9, 19)
    private val c = LocalDate.of(2026, 11, 1)
    private val d = LocalDate.of(2026, 12, 11)
    private val e = LocalDate.of(2027, 2, 2)

    @Test
    fun twoTakes_currentAgainstOriginal() {
        val days = listOf(prompt(a), prompt(b, repeatsDate = a))
        val chain = TakeChain.build(days, viewed = b)
        assertEquals(a, chain.original)
        assertEquals(listOf(a, b), chain.dates)
        assertEquals(a, chain.previous)
        assertTrue(chain.showDiptych)
        assertEquals(listOf<LocalDate>(), chain.rest)
        assertEquals(listOf(b), chain.laterFromOriginal)
    }

    @Test
    fun fiveTakes_currentAgainstTheOneBefore() {
        val days = listOf(
            prompt(a),
            prompt(b, repeatsDate = a),
            prompt(c, repeatsDate = a),
            prompt(d, repeatsDate = a),
            prompt(e, repeatsDate = a),
        )
        val latest = TakeChain.build(days, viewed = e)
        assertEquals(a, latest.original)
        assertEquals(listOf(a, b, c, d, e), latest.dates)
        assertEquals(d, latest.previous)
        assertEquals(listOf(a, b, c), latest.rest)
        assertTrue(latest.showDiptych)
        assertEquals(5, latest.size)

        val middle = TakeChain.build(days, viewed = c)
        assertEquals(b, middle.previous)
        assertEquals(listOf(a, d, e), middle.rest)
    }

    @Test
    fun originalWithLaterTakes_noDiptych() {
        val days = listOf(prompt(a), prompt(b, repeatsDate = a), prompt(c, repeatsDate = a))
        val chain = TakeChain.build(days, viewed = a)
        assertFalse(chain.isRepeat)
        assertFalse(chain.showDiptych)
        assertNull(chain.previous)
        assertEquals(listOf(b, c), chain.laterFromOriginal)
        assertEquals(listOf(b, c), chain.rest)
    }

    @Test
    fun startingFromALaterTake_stillPointsAtOriginal() {
        assertEquals(a, SecondTakeEligibility.originalDate(repeatsDate = a, date = b))
        assertEquals(a, SecondTakeEligibility.originalDate(repeatsDate = null, date = a))
    }

    @Test
    fun mixedOrientation_cropsSquare() {
        assertEquals(DiptychCrop.SQUARE, diptychCrop(1200, 1600, 1600, 1200))
        assertEquals(DiptychCrop.NATIVE, diptychCrop(1200, 1600, 900, 1200))
        assertEquals(DiptychCrop.NATIVE, diptychCrop(1600, 1200, 2000, 1500))
    }

    private fun prompt(date: LocalDate, repeatsDate: LocalDate? = null): DayPrompt = DayPrompt(
        date = date,
        title = "Find the sky in a puddle",
        oneLiner = "Turn the world upside down.",
        details = "Look down.",
        constraint = null,
        theme = "Reflections",
        tips = emptyList(),
        source = PromptSourceRef.LIBRARY,
        libraryId = "lib-1",
        modelName = null,
        generatedAt = Instant.parse("2026-06-28T08:00:00Z"),
        status = DayStatus.COMPLETED,
        frozen = false,
        rerollUsed = false,
        repeatsDate = repeatsDate,
    )
}

class SecondTakeEligibilityTest {
    private val today = LocalDate.of(2026, 9, 20)
    private val past = LocalDate.of(2026, 9, 19)

    @Test
    fun pastCompletedWithPhoto_isRepeatable() {
        assertTrue(
            SecondTakeEligibility.isRepeatable(past, today, DayStatus.COMPLETED, hasPhotograph = true),
        )
    }

    @Test
    fun todayIsNotRepeatable() {
        assertFalse(
            SecondTakeEligibility.isRepeatable(today, today, DayStatus.COMPLETED, hasPhotograph = true),
        )
    }

    @Test
    fun noPhotograph_isNotRepeatable() {
        assertFalse(
            SecondTakeEligibility.isRepeatable(
                past,
                today,
                DayStatus.COMPLETED_NO_PHOTO,
                hasPhotograph = false,
            ),
        )
    }

    @Test
    fun skippedMissedPaused_notRepeatable() {
        assertFalse(
            SecondTakeEligibility.isRepeatable(past, today, DayStatus.SKIPPED, hasPhotograph = true),
        )
        assertFalse(
            SecondTakeEligibility.isRepeatable(past, today, DayStatus.MISSED, hasPhotograph = true),
        )
        assertFalse(
            SecondTakeEligibility.isRepeatable(past, today, DayStatus.PAUSED, hasPhotograph = true),
        )
    }
}
