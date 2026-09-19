package app.shutterup.ui.calendar

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.PromptSourceRef
import java.time.Instant
import java.time.LocalDate

/** Seeded September 2026 mix of COMPLETED / MISSED / PENDING for Roborazzi. */
fun sampleMonthPrompts(today: LocalDate = LocalDate.of(2026, 9, 19)): List<DayPrompt> {
    val statuses = mapOf(
        1 to DayStatus.COMPLETED,
        2 to DayStatus.COMPLETED,
        3 to DayStatus.COMPLETED,
        4 to DayStatus.COMPLETED,
        5 to DayStatus.COMPLETED,
        6 to DayStatus.COMPLETED,
        7 to DayStatus.COMPLETED,
        8 to DayStatus.COMPLETED,
        9 to DayStatus.PENDING,
        10 to DayStatus.COMPLETED,
        11 to DayStatus.COMPLETED,
        12 to DayStatus.COMPLETED,
        13 to DayStatus.SKIPPED,
        14 to DayStatus.COMPLETED,
        15 to DayStatus.COMPLETED,
        16 to DayStatus.COMPLETED,
        17 to DayStatus.MISSED,
        18 to DayStatus.COMPLETED,
        19 to DayStatus.PENDING,
    )
    return statuses.map { (day, status) ->
        val date = LocalDate.of(today.year, today.month, day)
        DayPrompt(
            date = date,
            title = "Find the sky in a puddle",
            oneLiner = "Turn the world upside down using any reflective surface you pass today.",
            details = "Look down, not up.",
            constraint = "Don't rotate the photo afterwards.",
            theme = "Reflections",
            tips = emptyList(),
            source = PromptSourceRef.LIBRARY,
            libraryId = "lib-1",
            modelName = null,
            generatedAt = Instant.parse("2026-09-01T08:00:00Z"),
            status = status,
            frozen = day == 13,
            rerollUsed = false,
        )
    }
}
