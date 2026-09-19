package app.shutterup.ui.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shutterup.data.ai.NanoDownloadState
import app.shutterup.data.ai.NanoPromptGenerator
import app.shutterup.domain.ai.Availability
import app.shutterup.domain.ai.GeneratePromptUseCase
import app.shutterup.domain.ai.PromptGenerator
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.ui.settings.sanitizeThemeFocus
import app.shutterup.widget.TodayWidgetUpdater
import app.shutterup.work.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val page: Int = 0,
    val notifyHour: Int = 9,
    val notifyMinute: Int = 0,
    val themeFocus: String = "",
    val finished: Boolean = false,
    val saving: Boolean = false,
    val availability: Availability? = null,
    val aiDownloadPercent: Int? = null,
) {
    val isLastPage: Boolean get() = page == LAST_PAGE

    companion object {
        const val PAGE_COUNT = 4
        const val LAST_PAGE = PAGE_COUNT - 1
    }
}

/**
 * Four first-run pages: welcome, notifications, time + theme, AI status (DESIGN §4.9).
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val generatePrompt: GeneratePromptUseCase,
    private val scheduler: NotificationScheduler,
    private val widgetUpdater: TodayWidgetUpdater,
    @Named("primaryGenerator") private val primary: PromptGenerator,
    private val nano: NanoPromptGenerator,
    private val clock: Clock,
    private val zone: ZoneId,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { refreshAi() }
    }

    fun setPage(page: Int) {
        _state.update { it.copy(page = page.coerceIn(0, OnboardingUiState.LAST_PAGE)) }
        if (page == OnboardingUiState.LAST_PAGE) {
            viewModelScope.launch { refreshAi() }
        }
    }

    fun nextPage() {
        setPage(_state.value.page + 1)
    }

    fun previousPage() {
        setPage(_state.value.page - 1)
    }

    fun setNotifyTime(hour: Int, minute: Int) {
        _state.update { it.copy(notifyHour = hour, notifyMinute = minute) }
    }

    fun setThemeFocus(raw: String) {
        _state.update { it.copy(themeFocus = raw.take(THEME_FOCUS_MAX)) }
    }

    /** Persist prefs, generate today, then finish immediately; the buffer top-up
     * follows after navigation so a slow first Nano inference can never trap
     * the user on this screen (SPEC §4.1). Failures still complete onboarding:
     * Home shows the preparing state and the worker retries. */
    fun finish() {
        if (_state.value.saving || _state.value.finished) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            try {
                val snapshot = _state.value
                preferences.setNotifyTime(LocalTime.of(snapshot.notifyHour, snapshot.notifyMinute))
                preferences.setThemeFocus(sanitizeThemeFocus(snapshot.themeFocus))
                val today = LocalDate.now(clock.withZone(zone))
                val focus = preferences.observeThemeFocus().first()
                val paused = preferences.observePaused().first()
                if (!paused) {
                    runCatching { generatePrompt.promptFor(today, focus) }
                }
                scheduler.onSettingsChanged()
                runCatching { widgetUpdater.refresh() }
                preferences.setOnboardingComplete(true)
                _state.update { it.copy(finished = true) }
            } catch (e: Exception) {
                Log.w(TAG, "finish failed; completing onboarding anyway", e)
                runCatching { preferences.setOnboardingComplete(true) }
                _state.update { it.copy(finished = true) }
            } finally {
                _state.update { it.copy(saving = false) }
            }
            runCatching {
                val today = LocalDate.now(clock.withZone(zone))
                val focus = preferences.observeThemeFocus().first()
                if (!preferences.observePaused().first()) {
                    generatePrompt.topUpBuffer(today, focus)
                }
                widgetUpdater.refresh()
            }
        }
    }

    private suspend fun refreshAi() {
        val availability = runCatching { primary.availability() }.getOrNull()
        _state.update { it.copy(availability = availability, aiDownloadPercent = null) }
        if (availability == Availability.DOWNLOADABLE || availability == Availability.DOWNLOADING) {
            runCatching {
                nano.downloadState().collect { status ->
                    when (status) {
                        is NanoDownloadState.Downloading -> {
                            // Bytes-only progress: hide percent until a known fraction exists.
                            _state.update { it.copy(aiDownloadPercent = null) }
                        }
                        NanoDownloadState.Ready -> {
                            _state.update {
                                it.copy(availability = Availability.AVAILABLE, aiDownloadPercent = null)
                            }
                        }
                        NanoDownloadState.Unavailable, is NanoDownloadState.Failed -> {
                            _state.update {
                                it.copy(availability = Availability.UNAVAILABLE, aiDownloadPercent = null)
                            }
                        }
                        NanoDownloadState.Starting -> Unit
                    }
                }
            }
        }
    }

    companion object {
        const val THEME_FOCUS_MAX = 60
        private const val TAG = "Onboarding"
    }
}
