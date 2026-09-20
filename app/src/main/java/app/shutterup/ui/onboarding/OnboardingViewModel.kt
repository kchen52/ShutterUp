package app.shutterup.ui.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shutterup.domain.ai.GeneratePromptUseCase
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.widget.TodayWidgetUpdater
import app.shutterup.work.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
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
    val finished: Boolean = false,
    val saving: Boolean = false,
) {
    val isLastPage: Boolean get() = page == LAST_PAGE

    companion object {
        const val PAGE_COUNT = 3
        const val LAST_PAGE = PAGE_COUNT - 1
    }
}

/**
 * Three first-run pages: welcome, notifications, time (DESIGN §4.9).
 * Theme focus and on-device AI download are deferred.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val generatePrompt: GeneratePromptUseCase,
    private val scheduler: NotificationScheduler,
    private val widgetUpdater: TodayWidgetUpdater,
    private val clock: Clock,
    private val zone: ZoneId,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun setPage(page: Int) {
        _state.update { it.copy(page = page.coerceIn(0, OnboardingUiState.LAST_PAGE)) }
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

    /** Persist prefs, pick today's library prompt, then finish. Buffer top-up
     * follows after navigation so a slow write can never trap the user on
     * this screen (SPEC §4.1). Failures still complete onboarding: Home
     * shows the choosing state and the worker retries. */
    fun finish() {
        if (_state.value.saving || _state.value.finished) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            try {
                val snapshot = _state.value
                preferences.setNotifyTime(LocalTime.of(snapshot.notifyHour, snapshot.notifyMinute))
                val today = LocalDate.now(clock.withZone(zone))
                val paused = preferences.observePaused().first()
                if (!paused) {
                    runCatching { generatePrompt.promptFor(today, themeFocus = null) }
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
                if (!preferences.observePaused().first()) {
                    generatePrompt.topUpBuffer(today, themeFocus = null)
                }
                widgetUpdater.refresh()
            }
        }
    }

    companion object {
        private const val TAG = "Onboarding"
    }
}
