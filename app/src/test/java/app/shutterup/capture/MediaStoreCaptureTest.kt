package app.shutterup.capture

import android.provider.MediaStore
import app.shutterup.domain.capture.CompleteCaptureUseCase
import app.shutterup.domain.capture.MediaNaming
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.testutil.FakeDayPrompts
import app.shutterup.testutil.FakeEntries
import app.shutterup.testutil.FakeGamification
import app.shutterup.testutil.FakePrefs
import app.shutterup.testutil.TestJpegs
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaStorePhotoArchiverTest {
    private lateinit var archiver: MediaStorePhotoArchiver
    private val today = LocalDate.of(2026, 9, 19)
    private val capturedAt = today.atTime(12, 0).toInstant(ZoneOffset.UTC)

    @Before
    fun setUp() {
        archiver = MediaStorePhotoArchiver(RuntimeEnvironment.getApplication())
    }

    @Test
    fun insertOriginal_streamCopiesIntoPicturesShutterUp() {
        val source = File(RuntimeEnvironment.getApplication().cacheDir, "pending/shot.jpg")
        TestJpegs.write(source, width = 64, height = 48)
        val originalBytes = source.readBytes()
        val uri = archiver.insertOriginal(source, MediaNaming.displayName(today, "Reflections"), capturedAt)
        assertTrue(uri.toString().startsWith("content://media/"))
        assertEquals("2026-09-19_reflections.jpg", archiver.queryDisplayName(uri))
        val relative = archiver.queryRelativePath(uri)
        if (relative != null) {
            assertTrue(relative.contains("Pictures/ShutterUp") || relative.contains("ShutterUp"))
        }
        val copied = RuntimeEnvironment.getApplication().contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        assertEquals(originalBytes.toList(), copied.toList())
        assertEquals(0, pendingFlag(uri))
        val found = archiver.findByDisplayName("2026-09-19_reflections.jpg")
        assertEquals(uri, found)
    }

    @Test
    fun insertOriginal_doesNotReencodeBytes() {
        val source = File(RuntimeEnvironment.getApplication().cacheDir, "pending/raw.jpg")
        TestJpegs.write(source)
        // Read the expected bytes before archiving: CapturePipeline deletes the pending
        // file once it is in MediaStore, so the source is not guaranteed to outlive the
        // insert. The contract under test is that the stored copy is byte-identical.
        val originalBytes = source.readBytes()
        val uri = archiver.insertOriginal(source, "2026-09-19_untitled.jpg", capturedAt)
        val copied = RuntimeEnvironment.getApplication().contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        assertEquals(originalBytes.toList(), copied.toList())
    }

    private fun pendingFlag(uri: android.net.Uri): Int {
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        resolver.query(
            uri,
            arrayOf(MediaStore.Images.Media.IS_PENDING),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getInt(0)
        }
        return 0
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CapturePipelineTest {
    private val today = LocalDate.of(2026, 9, 19)
    private val zone = ZoneOffset.UTC
    private val clock = Clock.fixed(today.atTime(12, 0).toInstant(zone), zone)
    private lateinit var files: CaptureFileStore
    private lateinit var archiver: MediaStorePhotoArchiver
    private lateinit var metadata: CaptureMetadataReader
    private lateinit var pipeline: CapturePipeline
    private lateinit var prompts: FakeDayPrompts
    private lateinit var entries: FakeEntries

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        files = CaptureFileStore(context)
        archiver = MediaStorePhotoArchiver(context)
        metadata = CaptureMetadataReader(context, zone)
        prompts = FakeDayPrompts(mutableListOf(samplePrompt(today)))
        entries = FakeEntries()
        pipeline = CapturePipeline(
            files = files,
            archiver = archiver,
            metadata = metadata,
            thumbs = ThumbnailWriter(),
            completeCapture = CompleteCaptureUseCase(
                prompts = prompts,
                entries = entries,
                gamification = FakeGamification(),
                preferences = FakePrefs(),
                clock = clock,
                zone = zone,
            ),
            entries = entries,
            prompts = prompts,
            clock = clock,
            zone = zone,
        )
    }

    @Test
    fun persist_archivesToMediaStoreAndDeletesPending() = runTest {
        val pending = files.createPending()
        TestJpegs.write(pending, width = 80, height = 60)
        val pendingBytes = pending.readBytes()
        val outcome = pipeline.persist(
            PendingCapture(
                path = pending.absolutePath,
                capturedAtEpoch = today.atTime(12, 0).toInstant(zone).toEpochMilli(),
                width = 80,
                height = 60,
                imported = false,
            ),
            today,
        )
        assertTrue(outcome is PersistOutcome.Saved)
        val saved = outcome as PersistOutcome.Saved
        assertTrue(saved.mediaUri.startsWith("content://media/"))
        assertFalse(pending.exists())
        val entry = entries.listAll().single()
        assertEquals(saved.mediaUri, entry.mediaUri)
        assertFalse(entry.importedFromGallery)
        assertTrue(File(entry.thumbPath).isFile)
        val archived = RuntimeEnvironment.getApplication().contentResolver
            .openInputStream(android.net.Uri.parse(saved.mediaUri))!!
            .use { it.readBytes() }
        assertEquals(pendingBytes.toList(), archived.toList())
        assertEquals(DayStatus.COMPLETED, prompts.getDay(today)?.status)
        assertEquals(files.pendingDir(), pending.parentFile)
        assertTrue(files.pendingDir().path.contains("cache"))
    }

    @Test
    fun persist_pickerCopy_setsImportedFromGallery() = runTest {
        val pending = files.createPending()
        TestJpegs.write(pending)
        val outcome = pipeline.persist(
            PendingCapture(
                path = pending.absolutePath,
                capturedAtEpoch = today.atTime(12, 0).toInstant(zone).toEpochMilli(),
                width = 48,
                height = 32,
                imported = true,
            ),
            today,
        )
        assertTrue(outcome is PersistOutcome.Saved)
        assertTrue(entries.listAll().single().importedFromGallery)
    }

    @Test
    fun persist_emptyFile_isUndecodableAndLeavesDayPending() = runTest {
        val pending = files.createPending()
        pending.writeBytes(ByteArray(0))
        val outcome = pipeline.persist(
            PendingCapture(
                path = pending.absolutePath,
                capturedAtEpoch = today.atTime(12, 0).toInstant(zone).toEpochMilli(),
                width = 0,
                height = 0,
                imported = false,
            ),
            today,
        )
        assertEquals(PersistOutcome.Undecodable, outcome)
        assertEquals(DayStatus.PENDING, prompts.getDay(today)?.status)
        assertTrue(pending.exists())
    }
}

private fun samplePrompt(
    date: LocalDate,
    status: DayStatus = DayStatus.PENDING,
) = DayPrompt(
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
