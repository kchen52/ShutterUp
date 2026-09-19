package app.shutterup.widget

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Glance + Compose preview snapshot for today's widget (DESIGN.md §8).
 */
data class TodayWidgetState(
    val dateIso: String,
    val weekday: String,
    val theme: String,
    val title: String,
    val oneLiner: String,
    val constraint: String?,
    val streakDays: Int,
    val paused: Boolean,
    val completed: Boolean,
    val thumbPath: String?,
) {
    val kicker: String = "$weekday · $theme".uppercase(Locale.US)

    val streakLabel: String = if (streakDays == 1) "1 day" else "$streakDays days"

    companion object {
        fun from(
            date: LocalDate,
            prompt: DayPrompt?,
            streakDays: Int,
            paused: Boolean,
            thumbPath: String?,
        ): TodayWidgetState {
            val completed = prompt?.status == DayStatus.COMPLETED ||
                prompt?.status == DayStatus.COMPLETED_NO_PHOTO
            return TodayWidgetState(
                dateIso = date.toString(),
                weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US),
                theme = prompt?.theme.orEmpty().ifBlank { "Today" },
                title = when {
                    paused -> "Paused"
                    prompt == null -> "Today's prompt"
                    else -> prompt.title
                },
                oneLiner = prompt?.oneLiner.orEmpty(),
                constraint = prompt?.constraint,
                streakDays = streakDays,
                paused = paused,
                completed = completed && !paused,
                thumbPath = thumbPath,
            )
        }
    }
}
