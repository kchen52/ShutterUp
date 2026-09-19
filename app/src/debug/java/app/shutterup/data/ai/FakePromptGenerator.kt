package app.shutterup.data.ai

import app.shutterup.domain.ai.Availability
import app.shutterup.domain.ai.GeneratedMonthlyIssue
import app.shutterup.domain.ai.GeneratedPrompt
import app.shutterup.domain.ai.GeneratedSeries
import app.shutterup.domain.ai.GenerationRequest
import app.shutterup.domain.ai.MonthlyIssueRequest
import app.shutterup.domain.ai.PromptGenerator
import app.shutterup.domain.ai.PromptSource
import javax.inject.Inject

class FakePromptGenerator @Inject constructor() : PromptGenerator {
    var availability: Availability = Availability.AVAILABLE
    var seed: Long = 0

    override suspend fun availability(): Availability = availability

    override suspend fun generate(request: GenerationRequest): Result<GeneratedPrompt> {
        val date = request.date
        val theme = request.seriesTitle?.takeIf { it.isNotBlank() } ?: "Test Theme"
        return Result.success(
            GeneratedPrompt(
                title = "Test prompt $date",
                oneLiner = "Make a careful study of ordinary light on $date.",
                details = "Pick one nearby surface and watch how the light changes across it. Frame tightly so the rest of the room falls away.",
                tips = listOf("Hold the phone still.", "Expose for the brightest edge."),
                constraint = "Stay indoors",
                theme = theme.take(24),
                source = PromptSource.ON_DEVICE_AI,
            ),
        )
    }

    override suspend fun generateSeries(request: GenerationRequest): Result<GeneratedSeries> {
        val prompts = (0 until 7).map { offset ->
            val date = request.date.plusDays(offset.toLong())
            GeneratedPrompt(
                title = "Test prompt $date",
                oneLiner = "Make a careful study of ordinary light on $date.",
                details = "Pick one nearby surface and watch how the light changes across it. Frame tightly so the rest of the room falls away.",
                tips = listOf("Hold the phone still.", "Expose for the brightest edge."),
                constraint = "Stay indoors",
                theme = "Test Theme",
                source = PromptSource.ON_DEVICE_AI,
            )
        }
        return Result.success(
            GeneratedSeries(
                title = "A Week of Test Theme",
                theme = "Test Theme",
                prompts = prompts,
            ),
        )
    }

    override suspend fun generateMonthlyIssue(request: MonthlyIssueRequest): Result<GeneratedMonthlyIssue> {
        val theme = request.themeCounts.firstOrNull()?.first ?: "Looking"
        val second = request.themeCounts.getOrNull(1)?.first
        val headline = if (second == null) {
            "${theme.take(28)}."
        } else {
            val joined = "$theme and ${second.replaceFirstChar { it.lowercase() }}"
            if (joined.length <= 29) "$joined." else "${theme.take(28)}."
        }
        val body = buildString {
            append("You spent ${request.completedCount} days with ${theme.lowercase()}")
            if (second != null) append(" and ${second.lowercase()}")
            append(". ")
            if (request.notes.isNotEmpty()) {
                append("You wrote on ${request.notes.size} of them.")
            } else {
                append("The longest stretch was ${request.longestRun} days.")
            }
        }
        return Result.success(GeneratedMonthlyIssue(headline = headline, body = body))
    }
}
