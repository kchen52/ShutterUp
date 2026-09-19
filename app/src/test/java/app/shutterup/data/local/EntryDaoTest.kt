package app.shutterup.data.local

import androidx.room.Room
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.PromptSourceRef
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EntryDaoTest {
    private lateinit var db: ShutterUpDatabase
    private lateinit var entryDao: EntryDao
    private lateinit var dayDao: DayPromptDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            ShutterUpDatabase::class.java,
        ).allowMainThreadQueries().build()
        entryDao = db.entryDao()
        dayDao = db.dayPromptDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsert_observe_and_update() = runTest {
        val date = LocalDate.of(2024, 6, 15)
        assertNull(entryDao.observeEntry(date).first())

        val entry = entry(date, note = "first")
        entryDao.upsert(entry)
        val stored = entryDao.observeEntry(date).first()
        assertEquals("first", stored?.note)
        assertEquals(date, stored?.date)

        val updated = stored!!.copy(note = "edited")
        entryDao.upsert(updated)
        assertEquals("edited", entryDao.observeEntry(date).first()?.note)
    }

    @Test
    fun observeRecent_ordersByDateDescendingAndLimits() = runTest {
        entryDao.upsert(entry(LocalDate.of(2024, 6, 10)))
        entryDao.upsert(entry(LocalDate.of(2024, 6, 12)))
        entryDao.upsert(entry(LocalDate.of(2024, 6, 11)))

        val recent = entryDao.observeRecent(2).first()
        assertEquals(
            listOf(LocalDate.of(2024, 6, 12), LocalDate.of(2024, 6, 11)),
            recent.map { it.date },
        )
    }

    @Test
    fun observeByTheme_filtersAndOrdersNewestFirst() = runTest {
        dayDao.upsert(dayPrompt(LocalDate.of(2024, 6, 10), theme = "Reflections"))
        dayDao.upsert(dayPrompt(LocalDate.of(2024, 6, 11), theme = "Shadows"))
        dayDao.upsert(dayPrompt(LocalDate.of(2024, 6, 12), theme = "Reflections"))

        entryDao.upsert(entry(LocalDate.of(2024, 6, 10)))
        entryDao.upsert(entry(LocalDate.of(2024, 6, 11)))
        entryDao.upsert(entry(LocalDate.of(2024, 6, 12)))

        val reflections = entryDao.observeByTheme("Reflections").first()
        assertEquals(
            listOf(LocalDate.of(2024, 6, 12), LocalDate.of(2024, 6, 10)),
            reflections.map { it.date },
        )
        assertEquals(1, entryDao.observeByTheme("Shadows").first().size)
        assertTrueEmpty("Unknown")
    }

    @Test
    fun countForDate_allowsThreeRowsOnSameDay() = runTest {
        val date = LocalDate.of(2024, 6, 15)
        repeat(3) { i ->
            entryDao.upsert(entry(date, note = "n$i").copy(mediaUri = "content://$i"))
        }
        assertEquals(3, entryDao.countForDate(date))
        assertEquals(3, entryDao.observeEntries(date).first().size)
    }

    @Test
    fun count_and_delete() = runTest {
        val keep = LocalDate.of(2024, 6, 10)
        val drop = LocalDate.of(2024, 6, 11)
        entryDao.upsert(entry(keep))
        entryDao.upsert(entry(drop))
        assertEquals(2, entryDao.count())

        entryDao.delete(drop)
        assertEquals(1, entryDao.count())
        assertNull(entryDao.observeEntry(drop).first())
        assertEquals(keep, entryDao.observeEntry(keep).first()?.date)
    }

    private suspend fun assertTrueEmpty(theme: String) {
        assertEquals(emptyList<EntryEntity>(), entryDao.observeByTheme(theme).first())
    }

    private fun entry(date: LocalDate, note: String? = null) = EntryEntity(
        date = date,
        mediaUri = "content://media/shutterup/$date",
        thumbPath = "/thumbs/$date.jpg",
        capturedAt = Instant.parse("2024-06-15T10:00:00Z"),
        width = 4000,
        height = 3000,
        note = note,
        importedFromGallery = false,
        createdAt = Instant.parse("2024-06-15T10:01:00Z"),
        mediaKind = app.shutterup.domain.model.MediaKind.PHOTO,
    )

    private fun dayPrompt(date: LocalDate, theme: String) = DayPromptEntity(
        date = date,
        title = "Title $date",
        oneLiner = "One liner",
        details = "Details",
        constraint = null,
        theme = theme,
        tips = emptyList(),
        source = PromptSourceRef.LIBRARY,
        libraryId = "lib-1",
        modelName = null,
        generatedAt = Instant.parse("2024-06-15T08:00:00Z"),
        status = DayStatus.COMPLETED,
        frozen = false,
        rerollUsed = false,
    )
}
