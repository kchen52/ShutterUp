package app.shutterup.data.backup

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import app.shutterup.capture.CaptureFileStore
import app.shutterup.capture.MediaStorePhotoArchiver
import app.shutterup.capture.ThumbnailWriter
import app.shutterup.data.local.AchievementEntity
import app.shutterup.data.local.DayPromptEntity
import app.shutterup.data.local.EntryEntity
import app.shutterup.data.local.SeriesEntity
import app.shutterup.data.local.ShutterUpDatabase
import app.shutterup.data.local.StreakStateEntity
import app.shutterup.data.prefs.PreferencesDataStore
import app.shutterup.domain.capture.MediaNaming
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.MediaKind
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.testutil.TestJpegs
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressBackupStoreTest {
    private val date = LocalDate.of(2026, 9, 18)
    private val capturedAt = Instant.parse("2026-09-18T12:00:00Z")
    private val clock = Clock.fixed(Instant.parse("2026-09-20T16:00:00Z"), ZoneOffset.UTC)
    private lateinit var db: ShutterUpDatabase
    private lateinit var prefs: PreferencesDataStore
    private lateinit var files: CaptureFileStore
    private lateinit var photos: MediaStorePhotoArchiver
    private lateinit var store: ProgressBackupStore

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, ShutterUpDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefsFile = File(context.filesDir, "backup-test.preferences_pb")
        prefsFile.delete()
        prefs = PreferencesDataStore(
            PreferenceDataStoreFactory.create(
                produceFile = { prefsFile },
            ),
        )
        files = CaptureFileStore(context)
        photos = MediaStorePhotoArchiver(context)
        store = ProgressBackupStore(
            context = context,
            database = db,
            prompts = db.dayPromptDao(),
            entries = db.entryDao(),
            gamification = db.gamificationDao(),
            series = db.seriesDao(),
            monthlyIssues = db.monthlyIssueDao(),
            preferences = prefs,
            files = files,
            photos = photos,
            thumbs = ThumbnailWriter(),
            clock = clock,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun exportImport_roundTripsProgressAndPhotos() = runTest {
        seedHistory()
        val originalUri = db.entryDao().listAll().single().mediaUri
        val originalBytes = RuntimeEnvironment.getApplication().contentResolver
            .openInputStream(android.net.Uri.parse(originalUri))!!
            .use { it.readBytes() }

        val zip = ByteArrayOutputStream()
        store.exportTo(zip)

        db.clearAllTables()
        prefs.setNotifyTime(LocalTime.of(9, 0))
        prefs.setOnboardingComplete(false)
        prefs.setCoarseCityId(null)

        store.importFrom(ByteArrayInputStream(zip.toByteArray()))

        assertEquals("Find the sky in a puddle", db.dayPromptDao().getDay(date)?.title)
        assertEquals("wet pavement", db.entryDao().listAll().single().note)
        assertEquals(4, db.gamificationDao().getStreak()?.current)
        assertEquals("first_light", db.gamificationDao().allAchievements().single().id)
        assertEquals("A Week of Hands", db.seriesDao().all().single().title)
        assertEquals(LocalTime.of(7, 15), prefs.observeNotifyTime().first())
        assertEquals("sydney", prefs.observeCoarseCityId().first())
        assertTrue(prefs.observeOnboardingComplete().first())
        val restored = db.entryDao().listAll().single()
        assertTrue(File(restored.thumbPath).isFile)
        val restoredBytes = RuntimeEnvironment.getApplication().contentResolver
            .openInputStream(android.net.Uri.parse(restored.mediaUri))!!
            .use { it.readBytes() }
        assertEquals(originalBytes.toList(), restoredBytes.toList())
    }

    @Test
    fun restore_reinsertsGalleryPhotoWhenUriIsGone() = runTest {
        seedHistory()
        val originalUri = db.entryDao().listAll().single().mediaUri
        val originalBytes = RuntimeEnvironment.getApplication().contentResolver
            .openInputStream(android.net.Uri.parse(originalUri))!!
            .use { it.readBytes() }
        val zip = ByteArrayOutputStream()
        store.exportTo(zip)

        photos.delete(originalUri)
        db.clearAllTables()

        store.importFrom(ByteArrayInputStream(zip.toByteArray()))

        val restored = db.entryDao().listAll().single()
        assertNotEquals(originalUri, restored.mediaUri)
        assertTrue(photos.exists(restored.mediaUri))
        val restoredBytes = RuntimeEnvironment.getApplication().contentResolver
            .openInputStream(android.net.Uri.parse(restored.mediaUri))!!
            .use { it.readBytes() }
        assertEquals(originalBytes.toList(), restoredBytes.toList())
        assertEquals("2026-09-18_reflections.jpg", photos.queryDisplayName(android.net.Uri.parse(restored.mediaUri)))
    }

    @Test(expected = ProgressBackupException::class)
    fun import_rejectsZipSlip() = runTest {
        val zip = ByteArrayOutputStream()
        ZipOutputStream(zip).use { out ->
            out.putNextEntry(ZipEntry("../evil.json"))
            out.write("{}".toByteArray())
            out.closeEntry()
        }
        store.importFrom(ByteArrayInputStream(zip.toByteArray()))
    }

    @Test(expected = ProgressBackupException::class)
    fun import_rejectsEmptyZip() = runTest {
        val zip = ByteArrayOutputStream()
        ZipOutputStream(zip).use { }
        store.importFrom(ByteArrayInputStream(zip.toByteArray()))
    }

    private suspend fun seedHistory() {
        prefs.setNotifyTime(LocalTime.of(7, 15))
        prefs.setPreciseTiming(true)
        prefs.setOnboardingComplete(true)
        prefs.setCoarseCityId("sydney")
        db.seriesDao().insert(
            SeriesEntity(
                id = 4,
                title = "A Week of Hands",
                startDate = date.minusDays(1),
                endDate = date.plusDays(5),
                theme = "Hands",
                source = PromptSourceRef.LIBRARY,
            ),
        )
        db.dayPromptDao().upsert(
            DayPromptEntity(
                date = date,
                title = "Find the sky in a puddle",
                oneLiner = "Look down.",
                details = "Any reflective surface.",
                constraint = "Don't rotate.",
                theme = "Reflections",
                tips = listOf("Wait for shade"),
                source = PromptSourceRef.LIBRARY,
                libraryId = "lib-1",
                modelName = null,
                generatedAt = Instant.parse("2026-09-18T08:00:00Z"),
                status = DayStatus.COMPLETED,
                frozen = false,
                rerollUsed = true,
                seriesId = 4,
                seriesIndex = 2,
            ),
        )
        val source = File(RuntimeEnvironment.getApplication().cacheDir, "pending/shot.jpg")
        TestJpegs.write(source, width = 64, height = 48)
        val uri = photos.insertOriginal(source, MediaNaming.displayName(date, "Reflections"), capturedAt)
        val thumb = files.thumbFile(date)
        ThumbnailWriter().write(source, thumb)
        db.entryDao().upsert(
            EntryEntity(
                id = 9,
                date = date,
                mediaUri = uri.toString(),
                thumbPath = thumb.absolutePath,
                capturedAt = capturedAt,
                width = 64,
                height = 48,
                note = "wet pavement",
                importedFromGallery = false,
                createdAt = Instant.parse("2026-09-18T12:01:00Z"),
                mediaKind = MediaKind.PHOTO,
            ),
        )
        db.gamificationDao().updateStreak(
            StreakStateEntity(0, current = 4, longest = 12, freezes = 1, lastProcessedDate = date),
        )
        db.gamificationDao().unlock(
            AchievementEntity(
                id = "first_light",
                unlockedAt = Instant.parse("2026-09-18T12:02:00Z"),
                unlockedOnDate = date,
            ),
        )
    }
}
