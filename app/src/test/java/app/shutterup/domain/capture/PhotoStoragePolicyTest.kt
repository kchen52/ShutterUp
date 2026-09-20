package app.shutterup.domain.capture

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoUriClassifierTest {
    @Test
    fun mediaStoreContentUri_isMediaStore() {
        val uri = "content://media/external/images/media/42"
        assertEquals(PhotoUriKind.MEDIA_STORE, PhotoUriClassifier.kind(uri))
        assertFalse(PhotoUriClassifier.needsMigration(uri))
        assertTrue(PhotoUriClassifier.isMediaStore(uri))
    }

    @Test
    fun fileProviderUri_isPrivateAndNeedsMigration() {
        val uri = "content://app.shutterup.fileprovider/ext_pictures/2026-09-19_reflections_1.jpg"
        assertEquals(PhotoUriKind.PRIVATE, PhotoUriClassifier.kind(uri))
        assertTrue(PhotoUriClassifier.needsMigration(uri))
    }

    @Test
    fun fileSchemeAndRawPath_arePrivate() {
        assertEquals(PhotoUriKind.PRIVATE, PhotoUriClassifier.kind("file:///storage/emulated/0/Android/data/app.shutterup/files/Pictures/a.jpg"))
        assertEquals(PhotoUriKind.PRIVATE, PhotoUriClassifier.kind("/data/user/0/app.shutterup/files/Pictures/a.jpg"))
        assertTrue(PhotoUriClassifier.needsMigration("/tmp/photo.jpg"))
    }

    @Test
    fun empty_isUnknownAndNeedsMigration() {
        assertEquals(PhotoUriKind.UNKNOWN, PhotoUriClassifier.kind("  "))
        assertTrue(PhotoUriClassifier.needsMigration(""))
    }
}

class PhotoMigrationPolicyTest {
    @Test
    fun mediaStoreUri_skipsRegardlessOfFile() {
        val uri = "content://media/external/images/media/7"
        assertEquals(MigrationDecision.AlreadyMigrated, PhotoMigrationPolicy.decide(uri, sourceReadable = true))
        assertEquals(MigrationDecision.AlreadyMigrated, PhotoMigrationPolicy.decide(uri, sourceReadable = false))
    }

    @Test
    fun privateReadable_copies() {
        assertEquals(
            MigrationDecision.CopyToMediaStore,
            PhotoMigrationPolicy.decide("file:///tmp/old.jpg", sourceReadable = true),
        )
    }

    @Test
    fun privateMissing_keepsThumbnail() {
        assertEquals(
            MigrationDecision.KeepThumbnailFallback,
            PhotoMigrationPolicy.decide(
                "content://app.shutterup.fileprovider/ext_pictures/gone.jpg",
                sourceReadable = false,
            ),
        )
    }
}

class PendingCapturePolicyTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 9, 19)

    @Test
    fun nonEmptyToday_recovers() {
        val captured = today.atTime(15, 0).toInstant(zone)
        assertEquals(
            PendingFileDisposition.RECOVER,
            PendingCapturePolicy.disposition(true, captured, today, zone),
        )
    }

    @Test
    fun emptyToday_keptInFlight() {
        val captured = today.atTime(15, 0).toInstant(zone)
        assertEquals(
            PendingFileDisposition.KEEP_IN_FLIGHT,
            PendingCapturePolicy.disposition(false, captured, today, zone),
        )
    }

    @Test
    fun yesterday_discardedEvenIfNonEmpty() {
        val captured = today.minusDays(1).atTime(23, 50).toInstant(zone)
        assertEquals(
            PendingFileDisposition.DISCARD,
            PendingCapturePolicy.disposition(true, captured, today, zone),
        )
        assertEquals(
            PendingFileDisposition.DISCARD,
            PendingCapturePolicy.disposition(false, captured, today, zone),
        )
    }
}
