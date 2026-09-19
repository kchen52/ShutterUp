package app.shutterup.data.local

import androidx.room.Room
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class GamificationDaoTest {
    private lateinit var db: ShutterUpDatabase
    private lateinit var dao: GamificationDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            ShutterUpDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.gamificationDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun unlock_and_observeAchievements() = runTest {
        assertEquals(emptyList<AchievementEntity>(), dao.observeAchievements().first())

        val first = AchievementEntity(
            id = "first_light",
            unlockedAt = Instant.parse("2024-06-15T09:00:00Z"),
            unlockedOnDate = LocalDate.of(2024, 6, 15),
        )
        val streak = AchievementEntity(
            id = "streak_7",
            unlockedAt = Instant.parse("2024-06-21T09:00:00Z"),
            unlockedOnDate = LocalDate.of(2024, 6, 21),
        )
        dao.unlock(first)
        dao.unlock(streak)

        assertEquals(setOf("first_light", "streak_7"), dao.observeAchievements().first().map { it.id }.toSet())

        val updated = first.copy(unlockedOnDate = LocalDate.of(2024, 6, 16))
        dao.unlock(updated)
        val observed = dao.observeAchievements().first().associateBy { it.id }
        assertEquals(LocalDate.of(2024, 6, 16), observed.getValue("first_light").unlockedOnDate)
        assertEquals(2, observed.size)
    }

    @Test
    fun streak_observeIsNullUntilUpdated() = runTest {
        assertNull(dao.observeStreak().first())

        val state = StreakStateEntity(
            id = 0,
            current = 3,
            longest = 5,
            freezes = 1,
            lastProcessedDate = LocalDate.of(2024, 6, 15),
        )
        dao.updateStreak(state)
        assertEquals(state, dao.observeStreak().first())

        val next = state.copy(current = 4, lastProcessedDate = LocalDate.of(2024, 6, 16))
        dao.updateStreak(next)
        assertEquals(next, dao.observeStreak().first())
    }

    @Test
    fun recordUsage_upsertUpdatesDate() = runTest {
        val libraryId = "lib-42"
        dao.recordUsage(LibraryUsageEntity(libraryId, LocalDate.of(2024, 1, 1)))
        dao.recordUsage(LibraryUsageEntity(libraryId, LocalDate.of(2024, 6, 15)))

        assertTrue(dao.libraryUsedSince(libraryId, LocalDate.of(2024, 6, 15)))
        assertFalse(dao.libraryUsedSince(libraryId, LocalDate.of(2024, 6, 16)))
    }

    @Test
    fun libraryUsedSince_respectsInclusive180DayCutoff() = runTest {
        val today = LocalDate.of(2024, 12, 1)
        val since = today.minusDays(180)
        val libraryId = "lib-cutoff"

        dao.recordUsage(LibraryUsageEntity(libraryId, since.minusDays(1)))
        assertFalse(dao.libraryUsedSince(libraryId, since))

        dao.recordUsage(LibraryUsageEntity(libraryId, since))
        assertTrue(dao.libraryUsedSince(libraryId, since))

        assertFalse(dao.libraryUsedSince("unused", since))
    }
}
