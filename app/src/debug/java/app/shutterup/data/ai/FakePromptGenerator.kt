package app.shutterup.data.ai

import app.shutterup.domain.ai.Availability
import app.shutterup.domain.ai.GeneratedPrompt
import app.shutterup.domain.ai.GenerationRequest
import app.shutterup.domain.ai.PromptGenerator
import app.shutterup.domain.ai.PromptSource
import javax.inject.Inject

class FakePromptGenerator @Inject constructor() : PromptGenerator {
    var availability: Availability = Availability.AVAILABLE
    var seed: Long = 0

    override suspend fun availability(): Availability = availability

    override suspend fun generate(request: GenerationRequest): Result<GeneratedPrompt> {
        val date = request.date
        return Result.success(
            GeneratedPrompt(
                title = "Test prompt $date",
                oneLiner = "Make a careful study of ordinary light on $date.",
                details = "Pick one nearby surface and watch how the light changes across it. Frame tightly so the rest of the room falls away.",
                tips = listOf("Hold the phone still.", "Expose for the brightest edge."),
                constraint = "Stay indoors",
                theme = "Test Theme",
                source = PromptSource.ON_DEVICE_AI,
            ),
        )
    }
}
