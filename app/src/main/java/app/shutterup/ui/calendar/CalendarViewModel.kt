package app.shutterup.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.ui.home.MonthProgress
import app.shutterup.ui.home.monthProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Calendar month grid plus month-ring totals and longest streak. */
data class CalendarUiState(
    val month: YearMonth,
    val today: LocalDate,
    val cells: List<CalendarCell> = emptyList(),
    val monthCompleted: Int = 0,
    val monthEligible: Int = 0,
    val longestStreak: Int = 0,
    val hasHistory: Boolean = false,
)

/**
 * Observes the visible month through repositories (no DAOs).
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val prompts: DayPromptRepository,
    private val entries: EntryRepository,
    private val gamification: GamificationRepository,
    clock: Clock,
    zone: ZoneId,
) : ViewModel() {

    val today: LocalDate = LocalDate.now(clock.withZone(zone))

    private val visibleMonth = MutableStateFlow(YearMonth.from(today))
    private val thumbs = MutableStateFlow<Map<LocalDate, String>>(emptyMap())

    private val _state = MutableStateFlow(
        CalendarUiState(month = visibleMonth.value, today = today),
    )
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val byDate = entries.listAll().associate { it.date to it.thumbPath }
            thumbs.value = byDate
        }
        viewModelScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            visibleMonth.flatMapLatest { month ->
                combine(
                    prompts.observeDays(month.atDay(1), month.atEndOfMonth()),
                    thumbs,
                    gamification.observeStreak(),
                ) { days, thumbMap, streak ->
                    Triple(days, thumbMap, streak)
                }
            }.collect { (days, thumbMap, streak) ->
                val month = visibleMonth.value
                val byDate = days.associateBy { it.date }
                val progress = monthProgress(days, today, month)
                _state.value = CalendarUiState(
                    month = month,
                    today = today,
                    cells = monthCells(month, byDate, thumbMap, today),
                    monthCompleted = progress.completed,
                    monthEligible = progress.eligible,
                    longestStreak = streak.longest,
                    hasHistory = days.any { it.status.isHistory() } || thumbMap.isNotEmpty(),
                )
            }
        }
        viewModelScope.launch {
            gamification.observeStreak().collect { streak: StreakState ->
                _state.update { it.copy(longestStreak = streak.longest) }
            }
        }
    }

    /** Moves the pager / chevron selection to [month]. */
    fun showMonth(month: YearMonth) {
        visibleMonth.value = month
    }

    fun previousMonth() {
        showMonth(visibleMonth.value.minusMonths(1))
    }

    fun nextMonth() {
        showMonth(visibleMonth.value.plusMonths(1))
    }
}

private fun DayStatus.isHistory(): Boolean = when (this) {
    DayStatus.COMPLETED,
    DayStatus.COMPLETED_NO_PHOTO,
    DayStatus.SKIPPED,
    DayStatus.MISSED,
    DayStatus.PAUSED,
    -> true
    DayStatus.PENDING -> false
}

/** Preview / Roborazzi helper so screens do not depend on Hilt. */
fun sampleCalendarState(
    month: YearMonth = YearMonth.of(2026, 9),
    today: LocalDate = LocalDate.of(2026, 9, 19),
    progress: MonthProgress = MonthProgress(18, 19),
    longest: Int = 22,
): CalendarUiState {
    val days = sampleMonthPrompts(today)
    return CalendarUiState(
        month = month,
        today = today,
        cells = monthCells(month, days.associateBy { it.date }, emptyMap(), today),
        monthCompleted = progress.completed,
        monthEligible = progress.eligible,
        longestStreak = longest,
        hasHistory = true,
    )
}
