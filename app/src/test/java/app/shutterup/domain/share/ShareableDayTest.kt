package app.shutterup.domain.share

import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.MediaKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ShareableDayTest {

    private val date = LocalDate.of(2026, 9, 19)

    @Test
    fun completedDayWithPhoto_isShareable() {
        assertTrue(isDayShareable(DayStatus.COMPLETED, listOf(photo())))
    }

    @Test
    fun completedNoPhoto_isNotShareableEvenIfAnEntryRemains() {
        assertFalse(isDayShareable(DayStatus.COMPLETED_NO_PHOTO, listOf(photo())))
    }

    @Test
    fun noEntries_isNotShareable() {
        assertFalse(isDayShareable(DayStatus.COMPLETED, emptyList()))
        assertFalse(isDayShareable(DayStatus.PENDING, emptyList()))
        assertFalse(isDayShareable(null, emptyList()))
    }

    @Test
    fun videoOnly_isNotShareable() {
        assertFalse(isDayShareable(DayStatus.COMPLETED, listOf(photo(kind = MediaKind.VIDEO))))
    }

    @Test
    fun skippedOrMissedWithoutPhoto_isNotShareable() {
        assertFalse(isDayShareable(DayStatus.SKIPPED, emptyList()))
        assertFalse(isDayShareable(DayStatus.MISSED, emptyList()))
    }

    private fun photo(kind: MediaKind = MediaKind.PHOTO): Entry = Entry(
        id = 1,
        date = date,
        mediaUri = "content://app.shutterup/photo.jpg",
        thumbPath = "/tmp/thumb.jpg",
        capturedAt = Instant.parse("2026-09-19T10:00:00Z"),
        width = 1200,
        height = 1600,
        note = "private — must never appear on the card",
        importedFromGallery = false,
        createdAt = Instant.parse("2026-09-19T10:00:00Z"),
        mediaKind = kind,
    )
}
