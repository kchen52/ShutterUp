package app.shutterup.domain.ai

import java.time.LocalDate

/**
 * In-flight prompt generation, observed by the slim in-app status bar.
 * Idle is [null] on [GeneratePromptUseCase.generation].
 */
data class GenerationProgress(
    val date: LocalDate,
    val series: Boolean = false,
)
