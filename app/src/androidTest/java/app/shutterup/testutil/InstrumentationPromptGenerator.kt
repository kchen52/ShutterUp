package app.shutterup.testutil

import app.shutterup.domain.ai.Availability
import app.shutterup.domain.ai.GeneratedPrompt
import app.shutterup.domain.ai.GenerationRequest
import app.shutterup.domain.ai.PromptGenerator
import app.shutterup.domain.ai.PromptSource
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic on-device stand-in. Each [generate] call returns a unique
 * title/theme so reroll is observable and validator dedup passes.
 */
@Singleton
class InstrumentationPromptGenerator @Inject constructor() : PromptGenerator {
    private val next = AtomicInteger(1)

    override suspend fun availability(): Availability = Availability.AVAILABLE

    override suspend fun generate(request: GenerationRequest): Result<GeneratedPrompt> {
        val n = next.getAndIncrement()
        return Result.success(
            GeneratedPrompt(
                title = "Test prompt $n",
                oneLiner = "Notice take $n of ordinary indoor light today.",
                details = "Pick one nearby surface and watch how the light changes across it. " +
                    "Frame tightly so the rest of the room falls away.",
                tips = listOf("Hold the phone still.", "Expose for the brightest edge."),
                constraint = "Stay indoors",
                theme = "Theme $n",
                source = PromptSource.ON_DEVICE_AI,
            ),
        )
    }
}
