package app.shutterup.ui.issue

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.MonthlyIssueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class IssueViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    issues: MonthlyIssueRepository,
    entries: EntryRepository,
) : ViewModel() {
    private val yearMonth: String = savedStateHandle.get<String>("yearMonth").orEmpty()

    val page: StateFlow<IssuePageUi?> = combine(
        issues.observe(yearMonth),
        entries.observeRecentEntries(2_000),
    ) { issue, recent ->
        val stored = issue ?: return@combine null
        val ym = YearMonth.parse(stored.yearMonth)
        val start = ym.atDay(1)
        val end = ym.atEndOfMonth()
        val byDate = recent
            .filter { it.date in start..end && it.thumbPath.isNotBlank() }
            .groupBy { it.date }
            .mapValues { (_, rows) -> rows.maxBy { it.capturedAt } }
        val thumbs = byDate.keys.sorted().map { date ->
            thumbFor(date, byDate[date]?.thumbPath)
        }
        stored.toPage(thumbs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@HiltViewModel
class IssueListViewModel @Inject constructor(
    issues: MonthlyIssueRepository,
) : ViewModel() {
    val items: StateFlow<List<IssueListItemUi>> = issues.observeAll()
        .map { rows -> rows.map { it.toListItem() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
