package app.shutterup.ui.issue

import app.shutterup.domain.model.MonthlyIssue
import app.shutterup.ui.calendar.spokenDate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class IssueThumbUi(
    val dateIso: String,
    val thumbPath: String?,
    val spokenDescription: String,
)

data class IssuePageUi(
    val yearMonth: String,
    val kicker: String,
    val headline: String,
    val body: String,
    val theme: String,
    val themesKicker: String,
    val thumbs: List<IssueThumbUi>,
)

data class IssueListItemUi(
    val yearMonth: String,
    val kicker: String,
    val headline: String,
    val theme: String,
)

/** Page kicker, e.g. `SEPTEMBER · 24 DAYS`. */
fun issueKicker(yearMonth: YearMonth, completedDayCount: Int): String {
    val month = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(Locale.ENGLISH)
    val unit = if (completedDayCount == 1) "DAY" else "DAYS"
    return "$month · $completedDayCount $unit"
}

fun issueListKicker(yearMonth: YearMonth, completedDayCount: Int): String {
    val month = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(Locale.ENGLISH)
    val unit = if (completedDayCount == 1) "DAY" else "DAYS"
    return "$month ${yearMonth.year} · $completedDayCount $unit"
}

fun issueThemesKicker(themes: List<String>): String =
    themes.joinToString(" · ") { it.uppercase(Locale.ENGLISH) }

fun MonthlyIssue.toPage(thumbs: List<IssueThumbUi>): IssuePageUi {
    val ym = YearMonth.parse(yearMonth)
    return IssuePageUi(
        yearMonth = yearMonth,
        kicker = issueKicker(ym, completedDayCount),
        headline = headline,
        body = body,
        theme = dominantTheme.ifBlank { "Looking" },
        themesKicker = issueThemesKicker(loudestThemes),
        thumbs = thumbs,
    )
}

fun MonthlyIssue.toListItem(): IssueListItemUi {
    val ym = YearMonth.parse(yearMonth)
    return IssueListItemUi(
        yearMonth = yearMonth,
        kicker = issueListKicker(ym, completedDayCount),
        headline = headline,
        theme = dominantTheme.ifBlank { "Looking" },
    )
}

fun thumbFor(date: LocalDate, thumbPath: String?): IssueThumbUi = IssueThumbUi(
    dateIso = date.toString(),
    thumbPath = thumbPath?.takeIf { it.isNotBlank() },
    spokenDescription = "${spokenDate(date)}, completed",
)

internal fun sampleIssuePage(sparse: Boolean = false): IssuePageUi {
    val thumbs = if (sparse) {
        listOf(
            LocalDate.of(2026, 9, 4),
            LocalDate.of(2026, 9, 12),
            LocalDate.of(2026, 9, 20),
        )
    } else {
        (1..24).map { LocalDate.of(2026, 9, it) }
    }
    return IssuePageUi(
        yearMonth = "2026-09",
        kicker = if (sparse) "SEPTEMBER · 3 DAYS" else "SEPTEMBER · 24 DAYS",
        headline = if (sparse) "A quiet month." else "Light and glass.",
        body = if (sparse) {
            "Three days, mostly reflections and quiet hours. A small set, held still."
        } else {
            "You looked up more than usual, and you kept going through a grey week. Twenty-four days, mostly reflections and looking up."
        },
        theme = "Reflections",
        themesKicker = if (sparse) "REFLECTIONS · QUIET HOURS" else "REFLECTIONS · LOOKING UP",
        thumbs = thumbs.map { thumbFor(it, null) },
    )
}

internal fun sampleIssueList(): List<IssueListItemUi> = listOf(
    IssueListItemUi("2026-09", "SEPTEMBER 2026 · 24 DAYS", "Light and glass.", "Reflections"),
    IssueListItemUi("2026-08", "AUGUST 2026 · 11 DAYS", "Hands and windows.", "Hands"),
)
