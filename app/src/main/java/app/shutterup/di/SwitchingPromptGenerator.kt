package app.shutterup.di

import app.shutterup.domain.ai.Availability
import app.shutterup.domain.ai.GeneratedMonthlyIssue
import app.shutterup.domain.ai.GeneratedPrompt
import app.shutterup.domain.ai.GeneratedSeries
import app.shutterup.domain.ai.GenerationRequest
import app.shutterup.domain.ai.MonthlyIssueRequest
import app.shutterup.domain.ai.PromptGenerator
import app.shutterup.domain.repository.PreferencesRepository
import javax.inject.Qualifier
import kotlinx.coroutines.flow.first

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FakeAiBinding

/**
 * Runtime swap between Nano and the debug [app.shutterup.data.ai.FakePromptGenerator]
 * based on the `debug_fake_ai` preference (SPEC §7.1).
 */
class SwitchingPromptGenerator(
    private val onDevice: PromptGenerator,
    private val fake: PromptGenerator?,
    private val preferences: PreferencesRepository,
) : PromptGenerator {
    private var held: PromptGenerator? = null
    private var holds: Int = 0

    override suspend fun availability(): Availability = current().availability()

    override suspend fun generate(request: GenerationRequest): Result<GeneratedPrompt> =
        current().generate(request)

    override suspend fun generateSeries(request: GenerationRequest): Result<GeneratedSeries> =
        current().generateSeries(request)

    override suspend fun generateMonthlyIssue(
        request: MonthlyIssueRequest,
    ): Result<GeneratedMonthlyIssue> = current().generateMonthlyIssue(request)

    override suspend fun holdEngine() {
        val generator = held ?: active()
        held = generator
        holds += 1
        generator.holdEngine()
    }

    override suspend fun releaseEngine() {
        val generator = held ?: active()
        if (holds > 0) holds -= 1
        if (holds == 0) held = null
        generator.releaseEngine()
    }

    private suspend fun current(): PromptGenerator = held ?: active()

    private suspend fun active(): PromptGenerator {
        val useFake = fake != null && preferences.observeDebugUseFakeAi().first()
        return if (useFake) checkNotNull(fake) else onDevice
    }
}
