package app.shutterup.domain.share

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Text that appears on a share card. The note is deliberately absent —
 * notes never leave the app.
 *
 * [theme] is the prompt's original label so the UI can key the same
 * theme tint used elsewhere; [kicker] is the uppercase magazine line.
 */
data class ShareCardContent(
    val dateLabel: String,
    val theme: String,
    val title: String,
) {
    val themeLabel: String
        get() = theme.uppercase(Locale.ENGLISH).trim()

    /** `19 SEPTEMBER · REFLECTIONS`. Theme is omitted when blank. */
    val kicker: String
        get() = if (themeLabel.isEmpty()) dateLabel else "$dateLabel · $themeLabel"

    companion object {
        private val DATE: DateTimeFormatter =
            DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH)

        fun from(date: LocalDate, theme: String, title: String): ShareCardContent =
            ShareCardContent(
                dateLabel = date.format(DATE).uppercase(Locale.ENGLISH),
                theme = theme,
                title = title,
            )
    }
}
