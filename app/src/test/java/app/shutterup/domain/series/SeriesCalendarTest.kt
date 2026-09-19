package app.shutterup.domain.series

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.domain.model.Series
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesCalendarTest {
    private val today = LocalDate.of(2026, 9, 19)

    @Test
    fun shouldStartSeries_onlyWhenEnabledAndNotAlreadyInOne() {
        assertTrue(SeriesCalendar.shouldStartSeries(seriesEnabled = true, alreadyInSeries = false))
        assertFalse(SeriesCalendar.shouldStartSeries(seriesEnabled = true, alreadyInSeries = true))
        assertFalse(SeriesCalendar.shouldStartSeries(seriesEnabled = false, alreadyInSeries = false))
        assertFalse(SeriesCalendar.shouldStartSeries(seriesEnabled = false, alreadyInSeries = true))
    }

    @Test
    fun nextEmptyDate_skipsExistingPrompts_soSeriesStartsAfterBuffer() {
        val filled = setOf(today, today.plusDays(1), today.plusDays(2))
        val start = SeriesCalendar.nextEmptyDate(today, hasPrompt = { it in filled })
        assertEquals(today.plusDays(3), start)
    }

    @Test
    fun nextEmptyDate_isTodayWhenNothingGeneratedYet() {
        assertEquals(today, SeriesCalendar.nextEmptyDate(today, hasPrompt = { false }))
    }

    @Test
    fun nextEmptyDate_nullWhenHorizonIsFull() {
        assertNull(SeriesCalendar.nextEmptyDate(today, hasPrompt = { true }, searchLimitDays = 3))
    }

    @Test
    fun indexOf_isOneBased() {
        assertEquals(1, SeriesCalendar.indexOf(today, today))
        assertEquals(3, SeriesCalendar.indexOf(today, today.plusDays(2)))
        assertEquals(7, SeriesCalendar.indexOf(today, today.plusDays(6)))
    }

    @Test
    fun librarySeriesTitle_prefixesTheme() {
        assertEquals("A Week of Hands", librarySeriesTitle("Hands"))
        assertEquals("A Week of Morning Light", librarySeriesTitle("Morning Light"))
        assertEquals("A Week of Hands", librarySeriesTitle("A Week of Hands"))
    }
}

class SeriesProgressCalculatorTest {
    private val start = LocalDate.of(2026, 9, 17)
    private val series = Series(
        id = 1,
        title = "A Week of Hands",
        startDate = start,
        endDate = start.plusDays(6),
        theme = "Hands",
        source = PromptSourceRef.ON_DEVICE_AI,
    )

    @Test
    fun currentDayIsRing_completedAreFilled_missedStayEmpty() {
        val days = listOf(
            prompt(start, DayStatus.COMPLETED),
            prompt(start.plusDays(1), DayStatus.MISSED),
            prompt(start.plusDays(2), DayStatus.PENDING),
        )
        val view = start.plusDays(2)
        val progress = SeriesProgressCalculator.progress(series, days, view)
        assertEquals("A Week of Hands · 3 OF 7", progress.kicker)
        assertEquals(3, progress.index)
        assertEquals(SeriesDot.COMPLETED, progress.dots[0])
        assertEquals(SeriesDot.EMPTY, progress.dots[1])
        assertEquals(SeriesDot.CURRENT, progress.dots[2])
        assertTrue(progress.dots.drop(3).all { it == SeriesDot.EMPTY })
    }

    @Test
    fun detailKicker_dropsThemeWhenAlreadyInTitle() {
        val progress = SeriesProgressCalculator.progress(series, emptyList(), start)
        assertEquals("A Week of Hands · 1 OF 7", progress.detailKicker("Hands"))
        assertEquals("A Week of Hands · 1 OF 7", progress.detailKicker("hands"))
    }

    @Test
    fun detailKicker_appendsShortDistinctTheme() {
        val progress = SeriesProgressCalculator.progress(series, emptyList(), start)
        assertEquals("A Week of Hands · 1 OF 7 · Steam", progress.detailKicker("Steam"))
    }

    @Test
    fun detailKicker_dropsLongThemeToStayOneLine() {
        val progress = SeriesProgressCalculator.progress(series, emptyList(), start)
        assertEquals("A Week of Hands · 1 OF 7", progress.detailKicker("Reflections"))
    }

    @Test
    fun completedNoPhotoCountsAsFilled() {
        val days = listOf(prompt(start, DayStatus.COMPLETED_NO_PHOTO))
        val progress = SeriesProgressCalculator.progress(series, days, start.plusDays(1))
        assertEquals(SeriesDot.COMPLETED, progress.dots[0])
        assertEquals(SeriesDot.CURRENT, progress.dots[1])
    }

    private fun prompt(date: LocalDate, status: DayStatus) = DayPrompt(
        date = date,
        title = "t",
        oneLiner = "o",
        details = "d. d.",
        constraint = null,
        theme = "Hands",
        tips = listOf("tip"),
        source = PromptSourceRef.ON_DEVICE_AI,
        libraryId = null,
        modelName = null,
        generatedAt = Instant.EPOCH,
        status = status,
        frozen = false,
        rerollUsed = false,
        seriesId = 1,
        seriesIndex = SeriesCalendar.indexOf(start, date),
    )
}
