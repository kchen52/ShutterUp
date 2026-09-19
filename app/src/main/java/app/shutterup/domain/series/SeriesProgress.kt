package app.shutterup.domain.series

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Series
import java.time.LocalDate

/** Quiet seven-dot row on Home / Prompt Detail. */
enum class SeriesDot {
    /** Filled, primary at low emphasis. */
    COMPLETED,
    /** Ring rather than a fill — the day being viewed. */
    CURRENT,
    /** Outline only: upcoming, missed, or skipped. */
    EMPTY,
}

data class SeriesProgress(
    val title: String,
    val index: Int,
    val length: Int = SeriesCalendar.LENGTH,
    val dots: List<SeriesDot>,
) {
    val kicker: String = "$title · $index OF $length"

    /**
     * Prompt Detail kicker: series line takes the theme's slot.
     * If [theme] is already in the title, drop it; otherwise append
     * ` · theme` only when the line still fits one kicker at 200% font scale.
     */
    fun detailKicker(theme: String?): String {
        val trimmed = theme?.trim().orEmpty()
        if (trimmed.isEmpty()) return kicker
        if (title.contains(trimmed, ignoreCase = true)) return kicker
        val withTheme = "$kicker · $trimmed"
        return if (withTheme.length <= MAX_DETAIL_KICKER_CHARS) withTheme else kicker
    }
}

/** Longest one-line series kicker that still fits a compact row at 200% font scale. */
internal const val MAX_DETAIL_KICKER_CHARS = 36

object SeriesProgressCalculator {
    fun progress(
        series: Series,
        days: List<DayPrompt>,
        viewDate: LocalDate,
    ): SeriesProgress {
        val byDate = days.associateBy { it.date }
        val dots = SeriesCalendar.dates(series.startDate).map { date ->
            val status = byDate[date]?.status
            when {
                date == viewDate -> SeriesDot.CURRENT
                status == DayStatus.COMPLETED || status == DayStatus.COMPLETED_NO_PHOTO ->
                    SeriesDot.COMPLETED
                else -> SeriesDot.EMPTY
            }
        }
        val index = SeriesCalendar.indexOf(series.startDate, viewDate)
            .coerceIn(1, SeriesCalendar.LENGTH)
        return SeriesProgress(
            title = series.title,
            index = index,
            length = SeriesCalendar.LENGTH,
            dots = dots,
        )
    }
}
