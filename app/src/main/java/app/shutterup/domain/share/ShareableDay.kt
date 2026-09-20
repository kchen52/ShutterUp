package app.shutterup.domain.share

import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.MediaKind

/**
 * A day can be shared only when it actually has a still photo. Notes,
 * video, and completed-without-photo days stay in the app.
 */
fun isDayShareable(status: DayStatus?, entries: List<Entry>): Boolean {
    if (status == DayStatus.COMPLETED_NO_PHOTO) return false
    return entries.any { it.mediaKind == MediaKind.PHOTO }
}
