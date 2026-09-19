package app.shutterup.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shutterup.domain.ai.GeneratePromptUseCase
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
) {
    val isLastPage: Boolean get() = page == LAST_PAGE

    companion object {
        const val PAGE_COUNT = 3
        const val LAST_PAGE = PAGE_COUNT - 1
    }
}

/**
 * Three first-run pages: welcome, notification opt-in, time + theme focus.
 * Completing writes [PreferencesRepository] keys additively and lands on Home.
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
        _state.update { it.copy(page = (it.page + 1).coerceAtMost(OnboardingUiState.LAST_PAGE)) }
    }

    fun previousPage() {
        _state.update { it.copy(page = (it.page - 1).coerceAtLeast(0)) }
    }

    fun setNotifyTime(hour: Int, minute: Int) {
        _state.update { it.copy(notifyHour = hour, notifyMinute = minute) }
    }

    fun setThemeFocus(raw: String) {
        _state.update { it.copy(themeFocus = raw.take(THEME_FOCUS_MAX)) }
    }

    /** Persist prefs, generate today, schedule, mark onboarding complete (SPEC §4.1). */
    fun finish() {
        if (_state.value.saving || _state.value.finished) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val snapshot = _state.value
            preferences.setNotifyTime(LocalTime.of(snapshot.notifyHour, snapshot.notifyMinute))
            preferences.setThemeFocus(sanitizeThemeFocus(snapshot.themeFocus))
            val today = LocalDate.now(clock.withZone(zone))
            val focus = preferences.observeThemeFocus().first()
            val paused = preferences.observePaused().first()
            if (!paused) {
                generatePrompt.promptFor(today, focus)
                generatePrompt.topUpBuffer(today, focus)
            }
            scheduler.onSettingsChanged()
            widgetUpdater.refresh()
            preferences.setOnboardingComplete(true)
            _state.update { it.copy(saving = false, finished = true) }
        }
    }

    companion object {
        const val THEME_FOCUS_MAX = 60
    }
}
