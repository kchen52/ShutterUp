package app.shutterup.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shutterup.domain.ai.Availability
import app.shutterup.domain.ai.GeneratePromptUseCase
import app.shutterup.domain.ai.PromptGenerator
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.domain.repository.SeriesRepository
import app.shutterup.domain.rollover.DayRolloverUseCase
import app.shutterup.domain.series.SeriesProgress
import app.shutterup.domain.series.SeriesProgressCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Home surface: today's prompt, streak/freezes, and this-month ring. */
data class HomeUiState(
    val today: LocalDate,
    val prompt: DayPrompt? = null,
    val streak: StreakState = StreakState(0, 0, 0, null),
    val monthCompleted: Int = 0,
    val monthEligible: Int = 0,
    val recent: List<Entry> = emptyList(),
    val paused: Boolean = false,
    val notificationsDenied: Boolean = false,
    val aiDownloadPercent: Int? = null,
    val preparingPrompts: Boolean = false,
    val seriesProgress: SeriesProgress? = null,
)

/**
 * Loads today from repositories, rolls over gaps, and tops up the prompt buffer.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prompts: DayPromptRepository,
    private val entries: EntryRepository,
    private val gamification: GamificationRepository,
    private val preferences: PreferencesRepository,
    private val generatePrompt: GeneratePromptUseCase,
    private val rollover: DayRolloverUseCase,
    private val seriesRepo: SeriesRepository,
    @Named("primaryGenerator") private val primary: PromptGenerator,
    private val clock: Clock,
    private val zone: ZoneId,
) : ViewModel() {

    val today: LocalDate = LocalDate.now(clock.withZone(zone))

    private val _state = MutableStateFlow(HomeUiState(today = today))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        val month = YearMonth.from(today)
        val rangeStart = minOf(month.atDay(1), today.minusDays(6))
        val rangeEnd = maxOf(month.atEndOfMonth(), today.plusDays(6))
        viewModelScope.launch {
            combine(
                combine(
                    prompts.observeDay(today),
                    prompts.observeDays(rangeStart, rangeEnd),
                    seriesRepo.observeCovering(today),
                ) { prompt, monthDays, series -> Triple(prompt, monthDays, series) },
                entries.observeRecentEntries(RECENT_LIMIT),
                gamification.observeStreak(),
                preferences.observePaused(),
            ) { slice, recent, streak, paused ->
                val (prompt, monthDays, series) = slice
                val progress = monthProgress(monthDays, today, month)
                HomeUiState(
                    today = today,
                    prompt = prompt,
                    streak = streak,
                    monthCompleted = progress.completed,
                    monthEligible = progress.eligible,
                    recent = recent,
                    paused = paused,
                    notificationsDenied = _state.value.notificationsDenied,
                    aiDownloadPercent = _state.value.aiDownloadPercent,
                    preparingPrompts = _state.value.preparingPrompts,
                    seriesProgress = series?.let {
                        SeriesProgressCalculator.progress(it, monthDays, today)
                    },
                )
            }.collect { next -> _state.value = next }
        }
        viewModelScope.launch {
            refreshNotifications()
            refreshAvailability()
            val paused = preferences.observePaused().first()
            rollover.rollover(today, paused)
            if (!paused) {
                _state.update { it.copy(preparingPrompts = true) }
                val focus = preferences.observeThemeFocus().first()
                generatePrompt.promptFor(today, focus)
                generatePrompt.topUpBuffer(today, focus)
                _state.update { it.copy(preparingPrompts = false) }
            }
        }
    }

    /** Clears the pause flag and generates today's prompt immediately (SPEC §4.3). */
    fun resume() {
        viewModelScope.launch {
            preferences.setPaused(false)
            val focus = preferences.observeThemeFocus().first()
            generatePrompt.promptFor(today, focus)
        }
    }

    fun refreshNotifications() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        _state.update { it.copy(notificationsDenied = !granted) }
    }

    private suspend fun refreshAvailability() {
        val availability = runCatching { primary.availability() }.getOrNull()
        _state.update {
            it.copy(
                aiDownloadPercent = if (availability == Availability.DOWNLOADING) 0 else null,
            )
        }
    }

    companion object {
        private const val RECENT_LIMIT = 8
    }
}
