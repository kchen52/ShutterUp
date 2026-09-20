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
    override suspend fun availability(): Availability = active().availability()

    override suspend fun generate(request: GenerationRequest): Result<GeneratedPrompt> =
        active().generate(request)

    override suspend fun generateSeries(request: GenerationRequest): Result<GeneratedSeries> =
        active().generateSeries(request)

    override suspend fun generateMonthlyIssue(
        request: MonthlyIssueRequest,
    ): Result<GeneratedMonthlyIssue> = active().generateMonthlyIssue(request)

    private suspend fun active(): PromptGenerator {
        val useFake = fake != null && preferences.observeDebugUseFakeAi().first()
        return if (useFake) checkNotNull(fake) else onDevice
    }
}
