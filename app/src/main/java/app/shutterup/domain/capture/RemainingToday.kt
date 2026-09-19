package app.shutterup.domain.capture

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Copy for the Prompt Detail time-remaining line (DESIGN §4.2). */
object RemainingToday {
    /**
     * "N hours left today" until under an hour, then "N minutes left today".
     * Never empty; zero minutes is "0 minutes left today".
     */
    fun label(now: ZonedDateTime): String {
        val end = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        val minutes = ChronoUnit.MINUTES.between(now, end).coerceAtLeast(0L)
        return if (minutes >= 60L) {
            val hours = minutes / 60L
            if (hours == 1L) "1 hour left today" else "$hours hours left today"
        } else {
            if (minutes == 1L) "1 minute left today" else "$minutes minutes left today"
        }
    }
}
