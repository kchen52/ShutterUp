package app.shutterup.ui.home

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.PromptSourceRef
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthProgressTest {

    @Test
    fun completedOverElapsedMinusPaused() {
        val today = LocalDate.of(2026, 9, 19)
        val days = listOf(
            prompt(today.withDayOfMonth(1), DayStatus.COMPLETED),
            prompt(today.withDayOfMonth(2), DayStatus.PAUSED),
            prompt(today.withDayOfMonth(3), DayStatus.MISSED),
        )
        val progress = monthProgress(days, today, YearMonth.of(2026, 9))
        assertEquals(1, progress.completed)
        assertEquals(18, progress.eligible)
    }

    private fun prompt(date: LocalDate, status: DayStatus): DayPrompt = DayPrompt(
        date = date,
        title = "t",
        oneLiner = "o",
        details = "d",
        constraint = null,
        theme = "Reflections",
        tips = emptyList(),
        source = PromptSourceRef.LIBRARY,
        libraryId = "1",
        modelName = null,
        generatedAt = Instant.parse("2026-09-01T08:00:00Z"),
        status = status,
        frozen = false,
        rerollUsed = false,
    )
}
