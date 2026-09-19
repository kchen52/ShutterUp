package app.shutterup.domain.ai

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.LibraryUsage
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.domain.model.SupersededPrompt
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.GamificationRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Named

class GeneratePromptUseCase @Inject constructor(
    @Named("primaryGenerator") private val primary: PromptGenerator,
    private val library: LibraryPromptGenerator,
    private val prompts: DayPromptRepository,
    private val gamification: GamificationRepository,
    private val validator: PromptValidator,
    private val clock: Clock,
    @Suppress("unused") private val zone: ZoneId,
) {
    suspend fun promptFor(date: LocalDate, themeFocus: String?): GeneratedPrompt {
        prompts.getDay(date)?.let { return it.toGeneratedPrompt() }
        return generateFresh(date, themeFocus, rerollUsed = false)
    }

    suspend fun topUpBuffer(
        today: LocalDate,
        themeFocus: String?,
        daysAhead: Int = 2,
    ): List<LocalDate> {
        val filled = mutableListOf<LocalDate>()
        var date = today
        val end = today.plusDays(daysAhead.toLong())
        while (!date.isAfter(end)) {
            if (prompts.getDay(date) == null) {
                promptFor(date, themeFocus)
                filled += date
            }
            date = date.plusDays(1)
        }
        return filled
    }

    suspend fun reroll(date: LocalDate, themeFocus: String?): GeneratedPrompt {
        val existing = prompts.getDay(date) ?: error("No prompt for $date")
        prompts.recordSuperseded(
            SupersededPrompt(
                date = date,
                title = existing.title,
                theme = existing.theme,
                generatedAt = clock.instant(),
            ),
        )
        return generateFresh(date, themeFocus, rerollUsed = true)
    }

    private suspend fun generateFresh(
        date: LocalDate,
        themeFocus: String?,
        rerollUsed: Boolean,
    ): GeneratedPrompt {
        val request = GenerationRequest(
            date = date,
            themeFocus = themeFocus,
            recentTitles = prompts.recentTitles(30),
            recentThemes = prompts.recentThemes(14),
            dayOfWeek = date.dayOfWeek,
            season = seasonForDate(date),
            excludeConstraintKinds = emptySet(),
        )
        val recentTitles = prompts.recentTitles(90)
        val recentThemeLedes = prompts.recentDays(90).map { ThemeLede(it.theme, it.oneLiner) }

        var chosen: GeneratedPrompt? = null
        var libraryId: String? = null

        repeat(3) {
            if (chosen != null) return@repeat
            val candidate = primary.generate(request).getOrNull() ?: return@repeat
            if (validator.validate(candidate, recentTitles, recentThemeLedes) is ValidationResult.Valid) {
                chosen = candidate
            }
        }

        if (chosen == null) {
            var picked = library.pick(request)
            var generated = picked.toGeneratedPrompt()
            if (validator.validate(generated, recentTitles, recentThemeLedes) is ValidationResult.Invalid) {
                picked = library.pick(request)
                generated = picked.toGeneratedPrompt()
            }
            chosen = generated
            libraryId = picked.id
        }

        // Until Milestone 4 rewires the primary binding to Nano, the primary IS the
        // library: resolve the id by title so usage (and the 180-day exclusion) is
        // recorded on this path too.
        if (chosen?.source == PromptSource.LIBRARY && libraryId == null) {
            libraryId = library.findIdByTitle(checkNotNull(chosen).title)
        }

        val result = checkNotNull(chosen)
        persist(date, result, libraryId, rerollUsed)
        if (result.source == PromptSource.LIBRARY && libraryId != null) {
            gamification.recordLibraryUsage(LibraryUsage(libraryId, date))
        }
        return result
    }

    private suspend fun persist(
        date: LocalDate,
        result: GeneratedPrompt,
        libraryId: String?,
        rerollUsed: Boolean,
    ) {
        prompts.upsert(
            DayPrompt(
                date = date,
                title = result.title,
                oneLiner = result.oneLiner,
                details = result.details,
                constraint = result.constraint,
                theme = result.theme,
                tips = result.tips,
                source = result.source.toRef(),
                libraryId = if (result.source == PromptSource.LIBRARY) libraryId else null,
                modelName = result.modelName,
                generatedAt = clock.instant(),
                status = DayStatus.PENDING,
                frozen = false,
                rerollUsed = rerollUsed,
            ),
        )
    }
}

private fun DayPrompt.toGeneratedPrompt(): GeneratedPrompt =
    GeneratedPrompt(
        title = title,
        oneLiner = oneLiner,
        details = details,
        tips = tips,
        constraint = constraint,
        theme = theme,
        source = when (source) {
            PromptSourceRef.ON_DEVICE_AI -> PromptSource.ON_DEVICE_AI
            PromptSourceRef.LIBRARY -> PromptSource.LIBRARY
        },
        modelName = modelName,
    )

private fun PromptSource.toRef(): PromptSourceRef =
    when (this) {
        PromptSource.ON_DEVICE_AI -> PromptSourceRef.ON_DEVICE_AI
        PromptSource.LIBRARY -> PromptSourceRef.LIBRARY
    }
