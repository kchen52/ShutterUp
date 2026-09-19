package app.shutterup.capture

import app.shutterup.domain.capture.MediaNaming
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.MediaKind
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.testutil.FakeDayPrompts
import app.shutterup.testutil.FakeEntries
import app.shutterup.testutil.TestJpegs
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeleteDayPhotoUseCaseTest {
    private val today = LocalDate.of(2026, 9, 19)
    private val zone = ZoneOffset.UTC

    @Test
    fun alsoFromGallery_deletesMediaStoreRowAndMarksCompletedNoPhoto() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val files = CaptureFileStore(context)
        val archiver = MediaStorePhotoArchiver(context)
        val source = files.createPending()
        TestJpegs.write(source)
        val uri = archiver.insertOriginal(
            source,
            MediaNaming.displayName(today, "Reflections"),
            today.atTime(12, 0).toInstant(zone),
        )
        val thumb = files.thumbFile(today)
        TestJpegs.write(thumb, width = 16, height = 16)
        val prompts = FakeDayPrompts(mutableListOf(samplePrompt(today, DayStatus.COMPLETED)))
        val entries = FakeEntries(
            mutableListOf(
                sampleEntry(today, uri.toString(), thumb.absolutePath),
            ),
        )
        val useCase = DeleteDayPhotoUseCase(entries, prompts, archiver, files)
        useCase(today, alsoFromGallery = true)
        assertEquals(0, entries.countForDate(today))
        assertEquals(DayStatus.COMPLETED_NO_PHOTO, prompts.getDay(today)?.status)
        assertFalse(archiver.exists(uri.toString()))
        assertFalse(thumb.exists())
    }

    @Test
    fun shutterUpOnly_leavesGalleryRow() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val files = CaptureFileStore(context)
        val archiver = MediaStorePhotoArchiver(context)
        val source = files.createPending()
        TestJpegs.write(source)
        val uri = archiver.insertOriginal(
            source,
            "2026-09-19_keep.jpg",
            today.atTime(12, 0).toInstant(zone),
        )
        val prompts = FakeDayPrompts(mutableListOf(samplePrompt(today, DayStatus.COMPLETED)))
        val entries = FakeEntries(mutableListOf(sampleEntry(today, uri.toString(), "/missing.jpg")))
        DeleteDayPhotoUseCase(entries, prompts, archiver, files)(today, alsoFromGallery = false)
        assertEquals(DayStatus.COMPLETED_NO_PHOTO, prompts.getDay(today)?.status)
        assertTrue(archiver.exists(uri.toString()))
    }

    @Test
    fun galleryDeleteFailure_stillClearsEntry() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val files = CaptureFileStore(context)
        val archiver = MediaStorePhotoArchiver(context)
        val prompts = FakeDayPrompts(mutableListOf(samplePrompt(today, DayStatus.COMPLETED)))
        val entries = FakeEntries(
            mutableListOf(sampleEntry(today, "content://media/external/images/media/999999", "/no.jpg")),
        )
        DeleteDayPhotoUseCase(entries, prompts, archiver, files)(today, alsoFromGallery = true)
        assertEquals(0, entries.count())
        assertEquals(DayStatus.COMPLETED_NO_PHOTO, prompts.getDay(today)?.status)
    }

    private fun samplePrompt(date: LocalDate, status: DayStatus) = DayPrompt(
        date = date,
        title = "Find the sky in a puddle",
        oneLiner = "one",
        details = "details",
        constraint = null,
        theme = "Reflections",
        tips = emptyList(),
        source = PromptSourceRef.LIBRARY,
        libraryId = "lib-1",
        modelName = null,
        generatedAt = Instant.EPOCH,
        status = status,
        frozen = false,
        rerollUsed = false,
    )

    private fun sampleEntry(date: LocalDate, mediaUri: String, thumbPath: String) = Entry(
        id = 1L,
        date = date,
        mediaUri = mediaUri,
        thumbPath = thumbPath,
        capturedAt = date.atTime(12, 0).toInstant(ZoneOffset.UTC),
        width = 64,
        height = 48,
        note = null,
        importedFromGallery = false,
        createdAt = Instant.EPOCH,
        mediaKind = MediaKind.PHOTO,
    )
}
