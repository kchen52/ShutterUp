package app.shutterup.data.local

import androidx.room.Room
import app.shutterup.domain.model.PromptSourceRef
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MonthlyIssueDaoTest {
    private lateinit var db: ShutterUpDatabase
    private lateinit var dao: MonthlyIssueDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            ShutterUpDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.monthlyIssueDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_isIdempotentOnYearMonth() = runTest {
        val first = issue("2026-09", "Light and glass.")
        val id = dao.insert(first)
        assertTrue(id > 0)
        val duplicate = dao.insert(first.copy(id = 0, headline = "A quieter take."))
        assertEquals(-1, duplicate)
        val stored = dao.get("2026-09")
        assertEquals("Light and glass.", stored?.headline)
        assertEquals(1, dao.all().size)
    }

    @Test
    fun dismissFromFeed_doesNotDelete() = runTest {
        dao.insert(issue("2026-09", "Light and glass."))
        dao.dismissFromFeed("2026-09")
        val stored = dao.observe("2026-09").first()
        assertEquals(true, stored?.dismissedFromFeed)
        assertEquals("Light and glass.", stored?.headline)
    }

    private fun issue(yearMonth: String, headline: String) = MonthlyIssueEntity(
        yearMonth = yearMonth,
        startDate = LocalDate.of(2026, 9, 1),
        endDate = LocalDate.of(2026, 9, 30),
        completedDayCount = 24,
        headline = headline,
        body = "Twenty-four days, mostly reflections. You wrote on nine of them.",
        dominantTheme = "Reflections",
        loudestThemes = listOf("Reflections", "Looking up"),
        source = PromptSourceRef.LIBRARY,
        generatedAt = Instant.EPOCH,
        dismissedFromFeed = false,
        modelName = null,
    )
}
