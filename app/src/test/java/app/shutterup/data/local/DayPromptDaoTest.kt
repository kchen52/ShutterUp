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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DayPromptDaoTest {
    private lateinit var db: ShutterUpDatabase
    private lateinit var dao: DayPromptDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            ShutterUpDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.dayPromptDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsert_get_and_observeDay() = runTest {
        assertNull(dao.getDay(LocalDate.of(2024, 6, 15)))
        assertNull(dao.observeDay(LocalDate.of(2024, 6, 15)).first())

        val prompt = dayPrompt(LocalDate.of(2024, 6, 15), title = "First light")
        dao.upsert(prompt)

        assertEquals(prompt, dao.getDay(prompt.date))
        assertEquals(prompt, dao.observeDay(prompt.date).first())

        val updated = prompt.copy(title = "Rerolled light", rerollUsed = true)
        dao.upsert(updated)
        assertEquals(updated, dao.getDay(prompt.date))
        assertEquals(updated, dao.observeDay(prompt.date).first())
    }

    @Test
    fun observeRange_ordersByDateAscending() = runTest {
        dao.upsert(dayPrompt(LocalDate.of(2024, 6, 17), title = "C"))
        dao.upsert(dayPrompt(LocalDate.of(2024, 6, 15), title = "A"))
        dao.upsert(dayPrompt(LocalDate.of(2024, 6, 16), title = "B"))
        dao.upsert(dayPrompt(LocalDate.of(2024, 6, 18), title = "outside"))

        val range = dao.observeRange(LocalDate.of(2024, 6, 15), LocalDate.of(2024, 6, 17)).first()
        assertEquals(listOf("A", "B", "C"), range.map { it.title })
    }

    @Test
    fun recentTitles_themes_and_days_respectLimitAndNewestFirst() = runTest {
        dao.upsert(dayPrompt(LocalDate.of(2024, 6, 10), title = "T10", theme = "Theme10"))
        dao.upsert(dayPrompt(LocalDate.of(2024, 6, 12), title = "T12", theme = "Theme12"))
        dao.upsert(dayPrompt(LocalDate.of(2024, 6, 11), title = "T11", theme = "Theme11"))

        assertEquals(listOf("T12", "T11"), dao.recentTitles(2))
        assertEquals(listOf("Theme12", "Theme11"), dao.recentThemes(2))
        val recent = dao.recentDays(2)
        assertEquals(listOf(LocalDate.of(2024, 6, 12), LocalDate.of(2024, 6, 11)), recent.map { it.date })
        assertEquals(3, dao.recentDays(10).size)
    }

    @Test
    fun recordSuperseded_persistsRow() = runTest {
        val superseded = SupersededPromptEntity(
            date = LocalDate.of(2024, 6, 15),
            title = "Old title",
            theme = "Reflections",
            generatedAt = Instant.parse("2024-06-15T08:00:00Z"),
        )
        dao.recordSuperseded(superseded)

        val cursor = db.query("SELECT title, theme FROM superseded_prompts", emptyArray())
        assertTrue(cursor.moveToFirst())
        assertEquals("Old title", cursor.getString(0))
        assertEquals("Reflections", cursor.getString(1))
        assertEquals(1, cursor.count)
        cursor.close()
    }

    @Test
    fun deleteAfter_keepsTodayAndDropsFuture() = runTest {
        dao.upsert(dayPrompt(LocalDate.of(2024, 6, 15), title = "Today"))
        dao.upsert(dayPrompt(LocalDate.of(2024, 6, 16), title = "Tomorrow"))
        dao.upsert(dayPrompt(LocalDate.of(2024, 6, 17), title = "Later"))
        dao.deleteAfter(LocalDate.of(2024, 6, 15))
        assertEquals("Today", dao.getDay(LocalDate.of(2024, 6, 15))?.title)
        assertNull(dao.getDay(LocalDate.of(2024, 6, 16)))
        assertNull(dao.getDay(LocalDate.of(2024, 6, 17)))
    }

    @Test
    fun daysInSeries_returnsOnlyMatchingRows() = runTest {
        dao.upsert(dayPrompt(LocalDate.of(2024, 6, 15), title = "S1").copy(seriesId = 4, seriesIndex = 1))
        dao.upsert(dayPrompt(LocalDate.of(2024, 6, 16), title = "S2").copy(seriesId = 4, seriesIndex = 2))
        dao.upsert(dayPrompt(LocalDate.of(2024, 6, 17), title = "Other").copy(seriesId = 9, seriesIndex = 1))
        val days = dao.daysInSeries(4)
        assertEquals(listOf("S1", "S2"), days.map { it.title })
    }

    private fun dayPrompt(
        date: LocalDate,
        title: String,
        theme: String = "Light",
    ) = DayPromptEntity(
        date = date,
        title = title,
        oneLiner = "Shoot $title",
        details = "Details for $title",
        constraint = "No zoom",
        theme = theme,
        tips = listOf("Look up", "Wait for shade"),
        source = PromptSourceRef.ON_DEVICE_AI,
        libraryId = null,
        modelName = "nano-v2",
        generatedAt = Instant.parse("2024-06-15T08:00:00Z"),
        status = DayStatus.PENDING,
        frozen = false,
        rerollUsed = false,
    )
}
