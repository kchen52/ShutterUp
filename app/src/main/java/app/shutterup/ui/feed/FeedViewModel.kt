package app.shutterup.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewModelScope
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.MonthlyIssueRepository
import app.shutterup.ui.calendar.spokenDate
import app.shutterup.ui.issue.IssuePageUi
import app.shutterup.ui.issue.thumbFor
import app.shutterup.ui.issue.toPage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One completed-day card in the chronological feed (DESIGN §4.6). */
data class FeedCardUi(
    val dateIso: String,
    val title: String,
    val theme: String,
    val kicker: String,
    val spokenDescription: String,
    val thumbPath: String?,
    val aspectRatio: Float,
)

data class FeedUiState(
    val items: List<FeedCardUi> = emptyList(),
    val themes: List<String> = emptyList(),
    /** `null` means the All chip. */
    val selectedTheme: String? = null,
    val featuredIssue: IssuePageUi? = null,
    val hasPastIssues: Boolean = false,
)

/**
 * Chronological completed-days feed. Repositories only (SPEC §10).
 */
@HiltViewModel
class FeedViewModel @Inject constructor(
    prompts: DayPromptRepository,
    entries: EntryRepository,
    private val issues: MonthlyIssueRepository,
) : ViewModel() {
    private val selectedTheme = MutableStateFlow<String?>(null)

    val state: StateFlow<FeedUiState> = combine(
        prompts.observeDays(HISTORY_START, HISTORY_END),
        entries.observeRecentEntries(HISTORY_LIMIT),
        issues.observeAll(),
        selectedTheme,
    ) { days, recent, monthly, theme ->
        val completed = completedDaysNewestFirst(days)
        val byDate = latestEntryByDate(recent)
        val themes = completed.map { it.theme }.distinct()
        val filtered = if (theme == null) completed else completed.filter { it.theme == theme }
        val latest = monthly.maxByOrNull { it.yearMonth }
        val featured = latest?.takeIf { !it.dismissedFromFeed }?.let { issue ->
            val start = issue.startDate
            val end = issue.endDate
            val thumbs = byDate.keys.filter { it in start..end }.sorted().map { date ->
                thumbFor(date, byDate[date]?.thumbPath)
            }
            issue.toPage(thumbs)
        }
        FeedUiState(
            items = filtered.map { it.toFeedCard(byDate[it.date]) },
            themes = themes,
            selectedTheme = theme,
            featuredIssue = featured,
            hasPastIssues = monthly.isNotEmpty(),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        FeedUiState(),
    )

    /** `null` selects All. */
    fun selectTheme(theme: String?) {
        selectedTheme.value = theme
    }

    fun dismissFeaturedIssue() {
        val yearMonth = state.value.featuredIssue?.yearMonth ?: return
        viewModelScope.launch { issues.dismissFromFeed(yearMonth) }
    }
}

internal val HISTORY_START: LocalDate = LocalDate.of(1970, 1, 1)
internal val HISTORY_END: LocalDate = LocalDate.of(2100, 12, 31)
private const val HISTORY_LIMIT = 2_000

private val FeedKickerFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

/** Feed kicker, e.g. `12 SEP · REFLECTIONS` (DESIGN §4.6). */
fun feedKicker(date: LocalDate, theme: String): String {
    val day = date.format(FeedKickerFormatter).uppercase(Locale.ENGLISH)
    return "$day · ${theme.uppercase(Locale.ENGLISH)}"
}

internal fun isCompletedStatus(status: DayStatus): Boolean =
    status == DayStatus.COMPLETED || status == DayStatus.COMPLETED_NO_PHOTO

internal fun completedDaysNewestFirst(days: List<DayPrompt>): List<DayPrompt> =
    days.filter { isCompletedStatus(it.status) }.sortedByDescending { it.date }

internal fun latestEntryByDate(entries: List<Entry>): Map<LocalDate, Entry> =
    entries.groupBy { it.date }.mapValues { (_, rows) -> rows.maxBy { it.capturedAt } }

internal fun DayPrompt.toFeedCard(entry: Entry?): FeedCardUi {
    val ratio = if (entry != null && entry.height > 0 && entry.width > 0) {
        entry.width.toFloat() / entry.height.toFloat()
    } else {
        3f / 4f
    }
    return FeedCardUi(
        dateIso = date.toString(),
        title = title,
        theme = theme,
        kicker = feedKicker(date, theme),
        spokenDescription = "${spokenDate(date)}, completed",
        thumbPath = entry?.thumbPath?.takeIf { it.isNotBlank() },
        aspectRatio = ratio.coerceIn(0.4f, 2.2f),
    )
}
