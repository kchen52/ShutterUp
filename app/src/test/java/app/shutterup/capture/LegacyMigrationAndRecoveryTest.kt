package app.shutterup.capture

import app.shutterup.domain.capture.CompleteCaptureUseCase
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.MediaKind
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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LegacyPhotoMigratorTest {
    private val date = LocalDate.of(2026, 9, 18)
    private val zone = ZoneOffset.UTC
    private val capturedAt = date.atTime(10, 0).toInstant(zone)

    @Test
    fun copiesPrivateFileIntoMediaStoreAndRewritesUri() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val files = CaptureFileStore(context)
        val archiver = MediaStorePhotoArchiver(context)
        val original = File(files.legacyPicturesDir().also { it.mkdirs() }, "2026-09-18_reflections_1.jpg")
        TestJpegs.write(original, width = 40, height = 30)
        val originalBytes = original.readBytes()
        val thumb = files.thumbFile(date)
        TestJpegs.write(thumb, width = 10, height = 10)
        val thumbBytes = thumb.readBytes()
        val prompts = FakeDayPrompts(mutableListOf(prompt(date)))
        val entries = FakeEntries(
            mutableListOf(entry(date, "file://${original.absolutePath}", thumb.absolutePath)),
        )
        LegacyPhotoMigrator(entries, prompts, archiver, files).migrate()
        val migrated = entries.listAll().single()
        assertTrue(migrated.mediaUri.startsWith("content://media/"))
        assertFalse(original.exists())
        assertEquals(thumbBytes.toList(), File(migrated.thumbPath).readBytes().toList())
        val copied = context.contentResolver.openInputStream(android.net.Uri.parse(migrated.mediaUri))!!
            .use { it.readBytes() }
        assertEquals(originalBytes.toList(), copied.toList())
    }

    @Test
    fun alreadyMigrated_isIdempotent() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val files = CaptureFileStore(context)
        val archiver = MediaStorePhotoArchiver(context)
        val source = files.createPending()
        TestJpegs.write(source)
        val uri = archiver.insertOriginal(source, "2026-09-18_reflections.jpg", capturedAt)
        val prompts = FakeDayPrompts(mutableListOf(prompt(date)))
        val entries = FakeEntries(mutableListOf(entry(date, uri.toString(), "/thumbs/x.jpg")))
        val migrator = LegacyPhotoMigrator(entries, prompts, archiver, files)
        migrator.migrate()
        migrator.migrate()
        assertEquals(uri.toString(), entries.listAll().single().mediaUri)
        assertEquals(1, entries.count())
    }

    @Test
    fun missingPrivateFile_keepsThumbnailFallback() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val files = CaptureFileStore(context)
        val archiver = MediaStorePhotoArchiver(context)
        val prompts = FakeDayPrompts(mutableListOf(prompt(date)))
        val missing = "file:///tmp/does-not-exist-shutterup.jpg"
        val entries = FakeEntries(mutableListOf(entry(date, missing, "/thumbs/keep.jpg")))
        LegacyPhotoMigrator(entries, prompts, archiver, files).migrate()
        assertEquals(missing, entries.listAll().single().mediaUri)
    }

    @Test
    fun interruptedPass_resumesRemainingRows() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val files = CaptureFileStore(context)
        val archiver = MediaStorePhotoArchiver(context)
        val first = File(files.legacyPicturesDir().also { it.mkdirs() }, "one.jpg")
        val second = File(files.legacyPicturesDir(), "two.jpg")
        TestJpegs.write(first)
        TestJpegs.write(second)
        val other = date.minusDays(1)
        val prompts = FakeDayPrompts(mutableListOf(prompt(date), prompt(other)))
        val entries = FakeEntries(
            mutableListOf(
                entry(other, "file://${first.absolutePath}", "/t1.jpg", id = 1),
                entry(date, "file://${second.absolutePath}", "/t2.jpg", id = 2),
            ),
        )
        val migrator = LegacyPhotoMigrator(entries, prompts, archiver, files)
        migrator.migrateOne(entries.listAll().first { it.date == other })
        assertTrue(entries.listAll().single { it.date == other }.mediaUri.startsWith("content://media/"))
        assertTrue(entries.listAll().single { it.date == date }.mediaUri.startsWith("file://"))
        migrator.migrate()
        assertTrue(entries.listAll().all { it.mediaUri.startsWith("content://media/") })
    }

    private fun prompt(day: LocalDate) = DayPrompt(
        date = day,
        title = "Title",
        oneLiner = "one",
        details = "details",
        constraint = null,
        theme = "Reflections",
        tips = emptyList(),
        source = PromptSourceRef.LIBRARY,
        libraryId = null,
        modelName = null,
        generatedAt = Instant.EPOCH,
        status = DayStatus.COMPLETED,
        frozen = false,
        rerollUsed = false,
    )

    private fun entry(day: LocalDate, mediaUri: String, thumbPath: String, id: Long = 1L) = Entry(
        id = id,
        date = day,
        mediaUri = mediaUri,
        thumbPath = thumbPath,
        capturedAt = capturedAt,
        width = 40,
        height = 30,
        note = "keep me",
        importedFromGallery = false,
        createdAt = Instant.EPOCH,
        mediaKind = MediaKind.PHOTO,
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PendingCaptureRecoveryTest {
    private val today = LocalDate.of(2026, 9, 19)
    private val zone = ZoneOffset.UTC
    private val clock = Clock.fixed(today.atTime(18, 0).toInstant(zone), zone)

    @Test
    fun recoversTodaysPendingFile() = runTest {
        val env = env()
        val pending = env.files.createPending()
        TestJpegs.write(pending, width = 32, height = 24)
        pending.setLastModified(today.atTime(17, 0).toInstant(zone).toEpochMilli())
        env.recovery.recover()
        assertFalse(pending.exists())
        val entry = env.entries.listAll().single()
        assertTrue(entry.mediaUri.startsWith("content://media/"))
        assertEquals(DayStatus.COMPLETED, env.prompts.getDay(today)?.status)
    }

    @Test
    fun discardsYesterdaysPendingFile() = runTest {
        val env = env()
        val stale = env.files.createPending()
        TestJpegs.write(stale)
        stale.setLastModified(today.minusDays(1).atTime(12, 0).toInstant(zone).toEpochMilli())
        env.recovery.recover()
        assertFalse(stale.exists())
        assertTrue(env.entries.listAll().isEmpty())
        assertEquals(DayStatus.PENDING, env.prompts.getDay(today)?.status)
    }

    @Test
    fun emptyTodaysFile_isLeftInFlight() = runTest {
        val env = env()
        val empty = env.files.createPending()
        empty.writeBytes(ByteArray(0))
        empty.setLastModified(today.atTime(17, 0).toInstant(zone).toEpochMilli())
        env.recovery.recover()
        assertTrue(empty.exists())
        assertTrue(env.entries.listAll().isEmpty())
    }

    @Test
    fun skippedDay_doesNotCompleteFromPending() = runTest {
        val env = env(status = DayStatus.SKIPPED)
        val pending = env.files.createPending()
        TestJpegs.write(pending)
        pending.setLastModified(today.atTime(17, 0).toInstant(zone).toEpochMilli())
        env.recovery.recover()
        assertFalse(pending.exists())
        assertTrue(env.entries.listAll().isEmpty())
        assertEquals(DayStatus.SKIPPED, env.prompts.getDay(today)?.status)
    }

    private fun env(status: DayStatus = DayStatus.PENDING): RecoveryEnv {
        val context = RuntimeEnvironment.getApplication()
        val files = CaptureFileStore(context)
        val archiver = MediaStorePhotoArchiver(context)
        val metadata = CaptureMetadataReader(context, zone)
        val prompts = FakeDayPrompts(
            mutableListOf(
                DayPrompt(
                    date = today,
                    title = "Title",
                    oneLiner = "one",
                    details = "details",
                    constraint = null,
                    theme = "Reflections",
                    tips = emptyList(),
                    source = PromptSourceRef.LIBRARY,
                    libraryId = null,
                    modelName = null,
                    generatedAt = Instant.EPOCH,
                    status = status,
                    frozen = false,
                    rerollUsed = false,
                ),
            ),
        )
        val entries = FakeEntries()
        val pipeline = CapturePipeline(
            files = files,
            archiver = archiver,
            metadata = metadata,
            thumbs = ThumbnailWriter(),
            completeCapture = CompleteCaptureUseCase(
                prompts, entries, FakeGamification(), FakePrefs(), clock, zone,
            ),
            entries = entries,
            prompts = prompts,
            clock = clock,
            zone = zone,
        )
        val recovery = PendingCaptureRecovery(
            files = files,
            metadata = metadata,
            pipeline = pipeline,
            prompts = prompts,
            notifications = app.shutterup.data.notifications.NotificationHelper(context),
            widgetUpdater = unusedWidget(),
            scheduler = unusedScheduler(),
            clock = clock,
            zone = zone,
        )
        return RecoveryEnv(files, prompts, entries, recovery)
    }

    private fun unusedWidget(): app.shutterup.widget.TodayWidgetUpdater {
        val context = RuntimeEnvironment.getApplication()
        return app.shutterup.widget.TodayWidgetUpdater(
            context = context,
            days = FakeDayPrompts(),
            entries = FakeEntries(),
            gamification = FakeGamification(),
            preferences = FakePrefs(),
            clock = clock,
            zone = zone,
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
        )
    }

    private fun unusedScheduler(): app.shutterup.work.NotificationScheduler {
        return app.shutterup.work.NotificationScheduler(
            context = RuntimeEnvironment.getApplication(),
            prefs = FakePrefs(),
            days = FakeDayPrompts(),
            clock = clock,
            zone = zone,
        )
    }

    private data class RecoveryEnv(
        val files: CaptureFileStore,
        val prompts: FakeDayPrompts,
        val entries: FakeEntries,
        val recovery: PendingCaptureRecovery,
    )
}
