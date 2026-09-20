package app.shutterup.domain.capture

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What to do with one `cache/pending/` file after process death (SPEC §14). */
enum class PendingFileDisposition {
    /** Non-empty, captured today — finish the MediaStore save. */
    RECOVER,

    /** Empty but still today's capture session — camera may still be writing. */
    KEEP_IN_FLIGHT,

    /** Captured on another local date, or otherwise stale. */
    DISCARD,
}

object PendingCapturePolicy {
    fun disposition(
        fileNonEmptyAndDecodable: Boolean,
        capturedAt: Instant,
        today: LocalDate,
        zone: ZoneId,
    ): PendingFileDisposition {
        val todayCapture = CaptureDateValidator.isCapturedToday(capturedAt, today, zone)
        return when {
            !todayCapture -> PendingFileDisposition.DISCARD
            fileNonEmptyAndDecodable -> PendingFileDisposition.RECOVER
            else -> PendingFileDisposition.KEEP_IN_FLIGHT
        }
    }
}
