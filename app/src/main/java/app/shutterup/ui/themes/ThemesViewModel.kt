package app.shutterup.ui.themes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.ui.feed.FeedCardUi
import app.shutterup.ui.feed.HISTORY_END
import app.shutterup.ui.feed.HISTORY_START
import app.shutterup.ui.feed.completedDaysNewestFirst
import app.shutterup.ui.feed.latestEntryByDate
import app.shutterup.ui.feed.toFeedCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ThemeCountUi(
    val theme: String,
    val count: Int,
)

data class ThemesUiState(
    val themeFocus: String? = null,
    val themes: List<ThemeCountUi> = emptyList(),
    /** `null` shows the theme list (compact) / unfiltered grid (expanded). */
    val selectedTheme: String? = null,
    val items: List<FeedCardUi> = emptyList(),
)

/**
 * Theme-focus browser: chips with counts and a filtered completed-days grid.
 */
@HiltViewModel
class ThemesViewModel @Inject constructor(
    prompts: DayPromptRepository,
    entries: EntryRepository,
    preferences: PreferencesRepository,
) : ViewModel() {
    private val selectedTheme = MutableStateFlow<String?>(null)

    val state: StateFlow<ThemesUiState> = combine(
        prompts.observeDays(HISTORY_START, HISTORY_END),
        entries.observeRecentEntries(2_000),
        preferences.observeThemeFocus(),
        selectedTheme,
    ) { days, recent, focus, theme ->
        val completed = completedDaysNewestFirst(days)
        val byDate = latestEntryByDate(recent)
        val counts = completed
            .groupingBy { it.theme }
            .eachCount()
            .map { (name, count) -> ThemeCountUi(name, count) }
        val filtered = if (theme == null) completed else completed.filter { it.theme == theme }
        ThemesUiState(
            themeFocus = focus?.takeIf { it.isNotBlank() },
            themes = counts,
            selectedTheme = theme,
            items = filtered.map { it.toFeedCard(byDate[it.date]) },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ThemesUiState(),
    )

    /** `null` clears the filter and returns to the theme list. */
    fun selectTheme(theme: String?) {
        selectedTheme.value = theme
    }
}
