package app.shutterup.domain.monthly

import java.time.LocalDate
import java.time.YearMonth

/** One completed day feeding an issue. Nano sees titles, themes, and notes — never pixels. */
data class CompletedDayForIssue(
    val date: LocalDate,
    val title: String,
    val theme: String,
    val note: String?,
)

data class ThemeCount(
    val theme: String,
    val count: Int,
)

/**
 * Everything the writer — Nano or the fallback templates — is allowed to know
 * about a finished month. No image descriptions.
 */
data class MonthlyIssueSnapshot(
    val yearMonth: YearMonth,
    val days: List<CompletedDayForIssue>,
    val rankedThemes: List<ThemeCount>,
    val longestRun: Int,
    val previousCompletedCount: Int? = null,
) {
    val completedCount: Int get() = days.size
    val notesCount: Int get() = days.count { !it.note.isNullOrBlank() }
    val notes: List<String> get() = days.mapNotNull { it.note?.takeIf { note -> note.isNotBlank() } }
    val titles: List<String> get() = days.map { it.title }

    companion object {
        /** Null when the month has no completed days — no issue at all. */
        fun of(
            yearMonth: YearMonth,
            days: List<CompletedDayForIssue>,
            previousCompletedCount: Int? = null,
        ): MonthlyIssueSnapshot? {
            val inMonth = days.filter { YearMonth.from(it.date) == yearMonth }.sortedBy { it.date }
            if (inMonth.isEmpty()) return null
            return MonthlyIssueSnapshot(
                yearMonth = yearMonth,
                days = inMonth,
                rankedThemes = ThemeRanking.rank(inMonth.map { it.theme }),
                longestRun = MonthlyIssueCalendar.longestRun(inMonth.map { it.date }),
                previousCompletedCount = previousCompletedCount?.takeIf { it > 0 },
            )
        }
    }
}

/** Headline + body produced for an issue (SPEC §7.9). */
data class MonthlyIssueCopy(
    val headline: String,
    val body: String,
)
