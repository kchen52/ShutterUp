package app.shutterup.domain.ai

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.LibraryUsage
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.domain.model.Series
import app.shutterup.domain.model.SupersededPrompt
import app.shutterup.domain.geo.CityCatalog
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.domain.repository.SeriesRepository
import app.shutterup.domain.series.SeriesCalendar
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class GeneratePromptUseCase @Inject constructor(
    @Named("primaryGenerator") private val primary: PromptGenerator,
    private val library: LibraryPromptGenerator,
    private val prompts: DayPromptRepository,
    private val gamification: GamificationRepository,
    private val validator: PromptValidator,
    private val preferences: PreferencesRepository,
    private val seriesRepo: SeriesRepository,
    private val clock: Clock,
    @Suppress("unused") private val zone: ZoneId,
) {
    private val mutex = Mutex()
    private val generationDepth = AtomicInteger(0)
    private val _generation = MutableStateFlow<GenerationProgress?>(null)
    val generation: StateFlow<GenerationProgress?> = _generation.asStateFlow()

    suspend fun promptFor(date: LocalDate, themeFocus: String?): GeneratedPrompt = mutex.withLock {
        promptForLocked(date, themeFocus)
    }

    /**
     * Library-only path for the notification worker (SPEC §7.6): never calls Nano.
     * Returns the persisted prompt for [date], or picks from the bundled bank.
     */
    suspend fun ensureLibraryPrompt(date: LocalDate, themeFocus: String?): GeneratedPrompt =
        mutex.withLock {
            prompts.getDay(date)?.let { return@withLock it.toGeneratedPrompt() }
            val covering = seriesRepo.covering(date)
            val seriesEnabled = preferences.observeSeriesEnabled().first()
            if (covering != null) {
                return@withLock withGeneration(GenerationProgress(date)) {
                    fillSeriesHole(date, themeFocus, covering, libraryOnly = true)
                }
            }
            if (SeriesCalendar.shouldStartSeries(seriesEnabled, alreadyInSeries = false)) {
                return@withLock withGeneration(GenerationProgress(date, series = true)) {
                    val request = requestFor(date, themeFocus)
                    val picked = library.pickSeries(request)
                    if (picked != null &&
                        validateSeries(picked, validator, prompts.recentTitles(90), recentLedes()) is ValidationResult.Valid
                    ) {
                        persistSeries(date, picked)
                        picked.prompts.first()
                    } else {
                        persistLibraryPick(date, themeFocus, rerollUsed = false)
                    }
                }
            }
            withGeneration(GenerationProgress(date)) {
                persistLibraryPick(date, themeFocus, rerollUsed = false)
            }
        }

    suspend fun topUpBuffer(
        today: LocalDate,
        themeFocus: String?,
        daysAhead: Int = 2,
    ): List<LocalDate> = mutex.withLock {
        val filled = mutableListOf<LocalDate>()
        var date = today
        val end = today.plusDays(daysAhead.toLong())
        while (!date.isAfter(end)) {
            if (prompts.getDay(date) == null) {
                promptForLocked(date, themeFocus)
                filled += date
            }
            date = date.plusDays(1)
        }
        filled
    }

    /**
     * SPEC §7.5 / §14: changing theme focus discards un-shown future buffer
     * prompts. Today's prompt is kept.
     */
    suspend fun discardUnshownFuture(today: LocalDate) = mutex.withLock {
        discardUnshownFutureLocked(today)
    }

    /**
     * Enabling Series keeps today and starts a seven-day run tomorrow, replacing
     * independent (non-series) buffer prompts. An in-progress series is left alone.
     */
    suspend fun startSeriesTomorrow(today: LocalDate, themeFocus: String?) = mutex.withLock {
        if (prompts.getDay(today) == null) {
            withGeneration(GenerationProgress(today)) {
                generateFresh(today, themeFocus, rerollUsed = false)
            }
        }
        clearUnshownFutureForNewSeries(today)
        val start = today.plusDays(1)
        if (seriesRepo.covering(start) != null) return@withLock
        if (prompts.getDay(start) != null) return@withLock
        withGeneration(GenerationProgress(start, series = true)) {
            generateSeriesOrSingle(start, themeFocus)
        }
    }

    suspend fun reroll(date: LocalDate, themeFocus: String?): GeneratedPrompt = mutex.withLock {
        val existing = prompts.getDay(date) ?: error("No prompt for $date")
        prompts.recordSuperseded(
            SupersededPrompt(
                date = date,
                title = existing.title,
                theme = existing.theme,
                generatedAt = clock.instant(),
            ),
        )
        val covering = existing.seriesId?.let { seriesRepo.get(it) } ?: seriesRepo.covering(date)
        withGeneration(GenerationProgress(date, series = covering != null && existing.repeatsDate == null)) {
            if (existing.repeatsDate != null) {
                generateFresh(date, themeFocus, rerollUsed = true, overwrite = true)
            } else if (covering != null) {
                rerollInsideSeries(date, themeFocus, existing, covering)
            } else {
                generateFresh(date, themeFocus, rerollUsed = true, overwrite = true)
            }
        }
    }

    private suspend fun promptForLocked(date: LocalDate, themeFocus: String?): GeneratedPrompt {
        prompts.getDay(date)?.let { return it.toGeneratedPrompt() }
        val covering = seriesRepo.covering(date)
        val seriesEnabled = preferences.observeSeriesEnabled().first()
        return when {
            covering != null -> withGeneration(GenerationProgress(date)) {
                fillSeriesHole(date, themeFocus, covering)
            }
            SeriesCalendar.shouldStartSeries(seriesEnabled, alreadyInSeries = false) ->
                withGeneration(GenerationProgress(date, series = true)) {
                    generateSeriesOrSingle(date, themeFocus)
                }
            else -> withGeneration(GenerationProgress(date)) {
                generateFresh(date, themeFocus, rerollUsed = false)
            }
        }
    }

    private suspend fun discardUnshownFutureLocked(today: LocalDate) {
        prompts.deleteAfter(today)
        for (row in seriesRepo.all()) {
            when {
                row.startDate.isAfter(today) -> seriesRepo.delete(row.id)
                row.endDate.isAfter(today) -> seriesRepo.update(row.copy(endDate = today))
            }
        }
    }

    private suspend fun clearUnshownFutureForNewSeries(today: LocalDate) {
        prompts.deleteIndependentAfter(today)
        for (row in seriesRepo.all()) {
            if (row.startDate.isAfter(today)) {
                prompts.deleteDaysInSeries(row.id)
                seriesRepo.delete(row.id)
            }
        }
    }

    private suspend fun generateSeriesOrSingle(
        date: LocalDate,
        themeFocus: String?,
    ): GeneratedPrompt {
        val request = requestFor(date, themeFocus)
        val recentTitles = prompts.recentTitles(90)
        val recentThemeLedes = recentLedes()
        var chosen: GeneratedSeries? = null
        repeat(3) {
            if (chosen != null) return@repeat
            val candidate = primary.generateSeries(request).getOrNull() ?: return@repeat
            if (validateSeries(candidate, validator, recentTitles, recentThemeLedes) is ValidationResult.Valid) {
                chosen = candidate
            }
        }
        if (chosen == null) {
            val picked = library.pickSeries(request)
            if (picked != null &&
                validateSeries(picked, validator, recentTitles, recentThemeLedes) is ValidationResult.Valid
            ) {
                chosen = picked
            }
        }
        val series = chosen
        if (series != null) {
            persistSeries(date, series)
            return series.prompts.first()
        }
        return generateFresh(date, themeFocus, rerollUsed = false)
    }

    private suspend fun fillSeriesHole(
        date: LocalDate,
        themeFocus: String?,
        covering: Series,
        libraryOnly: Boolean = false,
    ): GeneratedPrompt {
        val index = SeriesCalendar.indexOf(covering.startDate, date).coerceIn(1, SeriesCalendar.LENGTH)
        val siblings = prompts.daysInSeries(covering.id)
        val exclude = siblings.map { it.title }.toSet()
        val request = requestFor(date, themeFocus).copy(seriesTitle = covering.title)
        val generated = if (libraryOnly) {
            pickLibraryInTheme(request, covering, exclude)
        } else {
            generateFreshPrompt(request, overwrite = false)
                ?: pickLibraryInTheme(request, covering, exclude)
        }
        persistAndRecord(
            date = date,
            result = generated,
            libraryId = libraryIdFor(generated),
            rerollUsed = false,
            seriesId = covering.id,
            seriesIndex = index,
        )
        return generated
    }

    private suspend fun rerollInsideSeries(
        date: LocalDate,
        themeFocus: String?,
        existing: DayPrompt,
        covering: Series,
    ): GeneratedPrompt {
        val request = requestFor(date, themeFocus).copy(seriesTitle = covering.title)
        val siblings = prompts.daysInSeries(covering.id)
        val exclude = (siblings.map { it.title } + existing.title).toSet()
        val fromPrimary = generateFreshPrompt(request, overwrite = true)
        val generated = if (fromPrimary != null && fromPrimary.title !in exclude) {
            fromPrimary
        } else {
            pickLibraryInTheme(request, covering, exclude)
        }
        persistAndRecord(
            date = date,
            result = generated,
            libraryId = libraryIdFor(generated),
            rerollUsed = true,
            seriesId = covering.id,
            seriesIndex = existing.seriesIndex
                ?: SeriesCalendar.indexOf(covering.startDate, date).coerceIn(1, SeriesCalendar.LENGTH),
        )
        return generated
    }

    private suspend fun generateFresh(
        date: LocalDate,
        themeFocus: String?,
        rerollUsed: Boolean,
        overwrite: Boolean = false,
        seriesTitle: String? = null,
        seriesId: Long? = null,
        seriesIndex: Int? = null,
    ): GeneratedPrompt {
        val request = requestFor(date, themeFocus).copy(seriesTitle = seriesTitle)
        val result = generateFreshPrompt(request, overwrite)
            ?: persistLibraryPick(date, themeFocus, rerollUsed, overwrite, seriesId, seriesIndex).let {
                return it
            }
        if (!overwrite) {
            prompts.getDay(date)?.let { return it.toGeneratedPrompt() }
        }
        persistAndRecord(date, result, libraryIdFor(result), rerollUsed, seriesId, seriesIndex)
        return result
    }

    private suspend fun generateFreshPrompt(
        request: GenerationRequest,
        @Suppress("UNUSED_PARAMETER") overwrite: Boolean,
    ): GeneratedPrompt? {
        val recentTitles = prompts.recentTitles(90)
        val recentThemeLedes = recentLedes()
        var chosen: GeneratedPrompt? = null
        repeat(3) {
            if (chosen != null) return@repeat
            val candidate = primary.generate(request).getOrNull() ?: return@repeat
            if (validator.validate(candidate, recentTitles, recentThemeLedes) is ValidationResult.Valid) {
                chosen = candidate
            }
        }
        return chosen
    }

    private suspend fun persistLibraryPick(
        date: LocalDate,
        themeFocus: String?,
        rerollUsed: Boolean,
        overwrite: Boolean = false,
        seriesId: Long? = null,
        seriesIndex: Int? = null,
    ): GeneratedPrompt {
        val request = requestFor(date, themeFocus)
        val recentTitles = prompts.recentTitles(90)
        val recentThemeLedes = recentLedes()
        var picked = library.pick(request)
        var generated = picked.toGeneratedPrompt()
        if (validator.validate(generated, recentTitles, recentThemeLedes) is ValidationResult.Invalid) {
            picked = library.pick(request)
            generated = picked.toGeneratedPrompt()
        }
        if (!overwrite) {
            prompts.getDay(date)?.let { return it.toGeneratedPrompt() }
        }
        persistAndRecord(date, generated, picked.id, rerollUsed, seriesId, seriesIndex)
        return generated
    }

    private suspend fun pickLibraryInTheme(
        request: GenerationRequest,
        covering: Series,
        exclude: Set<String>,
    ): GeneratedPrompt {
        val picked = library.pickFromTheme(request, covering.theme, exclude)
            ?: library.pick(request)
        val generated = picked.toGeneratedPrompt()
        return generated
    }

    private suspend fun persistSeries(start: LocalDate, generated: GeneratedSeries) {
        prompts.getDay(start)?.let { return }
        val source = generated.prompts.first().source.toRef()
        val id = seriesRepo.insert(
            Series(
                title = generated.title,
                startDate = start,
                endDate = start.plusDays(SeriesCalendar.LENGTH - 1L),
                theme = generated.theme,
                source = source,
            ),
        )
        generated.prompts.forEachIndexed { index, prompt ->
            val date = start.plusDays(index.toLong())
            if (prompts.getDay(date) != null) return@forEachIndexed
            val libraryId = generated.libraryIds.getOrNull(index)
                ?: libraryIdFor(prompt)
            persistAndRecord(
                date = date,
                result = prompt,
                libraryId = libraryId,
                rerollUsed = false,
                seriesId = id,
                seriesIndex = index + 1,
            )
        }
    }

    private suspend fun persistAndRecord(
        date: LocalDate,
        result: GeneratedPrompt,
        libraryId: String?,
        rerollUsed: Boolean,
        seriesId: Long? = null,
        seriesIndex: Int? = null,
    ) {
        persist(date, result, libraryId, rerollUsed, seriesId, seriesIndex)
        if (result.source == PromptSource.LIBRARY && libraryId != null) {
            gamification.recordLibraryUsage(LibraryUsage(libraryId, date))
        }
    }

    private suspend fun persist(
        date: LocalDate,
        result: GeneratedPrompt,
        libraryId: String?,
        rerollUsed: Boolean,
        seriesId: Long?,
        seriesIndex: Int?,
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
                seriesId = seriesId,
                seriesIndex = seriesIndex,
            ),
        )
    }

    private suspend fun requestFor(date: LocalDate, themeFocus: String?): GenerationRequest =
        GenerationRequest(
            date = date,
            themeFocus = themeFocus,
            recentTitles = prompts.recentTitles(30),
            recentThemes = prompts.recentThemes(14),
            dayOfWeek = date.dayOfWeek,
            season = seasonForDate(date, CityCatalog.find(preferences.observeCoarseCityId().first())?.latitude),
            excludeConstraintKinds = emptySet(),
        )

    private suspend fun recentLedes(): List<ThemeLede> =
        prompts.recentDays(90).map { ThemeLede(it.theme, it.oneLiner) }

    private suspend fun libraryIdFor(result: GeneratedPrompt): String? {
        if (result.source != PromptSource.LIBRARY) return null
        return library.findIdByTitle(result.title)
    }

    private suspend fun <T> withGeneration(
        progress: GenerationProgress,
        block: suspend () -> T,
    ): T {
        _generation.value = progress
        generationDepth.incrementAndGet()
        try {
            return block()
        } finally {
            if (generationDepth.decrementAndGet() == 0) {
                _generation.value = null
            }
        }
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
