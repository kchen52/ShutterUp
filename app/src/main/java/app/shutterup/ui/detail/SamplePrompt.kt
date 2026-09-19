package app.shutterup.ui.detail

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.PromptSourceRef
import java.time.Instant
import java.time.LocalDate

/** Shared sample prompt for previews and Roborazzi. */
fun samplePrompt(
    source: PromptSourceRef = PromptSourceRef.LIBRARY,
    rerollUsed: Boolean = false,
): DayPrompt = DayPrompt(
    date = LocalDate.of(2026, 9, 19),
    title = "Find the sky in a puddle",
    oneLiner = "Turn the world upside down using any reflective surface you pass today.",
    details = "Look down, not up. Puddles, car roofs, and shop windows all hold a second sky.",
    constraint = "Don't rotate the photo afterwards.",
    theme = "Reflections",
    tips = listOf(
        "Tap to focus on the reflection, not the water.",
        "Try it after rain.",
    ),
    source = source,
    libraryId = if (source == PromptSourceRef.LIBRARY) "lib-1" else null,
    modelName = if (source == PromptSourceRef.ON_DEVICE_AI) "nano-v2" else null,
    generatedAt = Instant.parse("2026-09-19T08:00:00Z"),
    status = DayStatus.PENDING,
    frozen = false,
    rerollUsed = rerollUsed,
)
