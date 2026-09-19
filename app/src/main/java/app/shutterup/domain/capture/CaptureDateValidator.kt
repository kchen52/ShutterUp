package app.shutterup.domain.capture

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object CaptureDateValidator {
    /** True when [capturedAt] falls on [today] in [zone]. EXIF parsing lives in the data layer; this takes the resolved Instant. */
    fun isTakenToday(capturedAt: Instant, today: LocalDate, zone: ZoneId): Boolean {
        return capturedAt.atZone(zone).toLocalDate() == today
    }

    /** Alias used by the capture pipeline when gating yesterday-crossed returns. */
    fun isCapturedToday(capturedAt: Instant, today: LocalDate, zone: ZoneId): Boolean =
        isTakenToday(capturedAt, today, zone)
}
