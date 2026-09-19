package app.shutterup.data.repository

import app.shutterup.data.local.AchievementEntity
import app.shutterup.data.local.DayPromptEntity
import app.shutterup.data.local.EntryEntity
import app.shutterup.data.local.GamificationDao
import app.shutterup.data.local.LibraryUsageEntity
import app.shutterup.data.local.MonthlyIssueEntity
import app.shutterup.data.local.SeriesEntity
import app.shutterup.data.local.StreakStateEntity
import app.shutterup.data.local.SupersededPromptEntity
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.LibraryUsage
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.model.SupersededPrompt
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryMappingTest {

    private val date = LocalDate.of(2026, 9, 19)
    private val instant = Instant.parse("2026-09-19T13:45:00Z")

    @Test
    fun dayPrompt_roundTrip_nullablesEmptyTipsAndFlags() {
        val entity = DayPromptEntity(
            date = date,
            title = "Window light",
            oneLiner = "Find a slice of window light.",
            details = "Stand still and wait for the light to move.",
            constraint = null,
            theme = "Light",
            tips = emptyList(),
            source = PromptSourceRef.LIBRARY,
            libraryId = null,
            modelName = null,
            generatedAt = instant,
            status = DayStatus.PENDING,
            frozen = true,
            rerollUsed = true,
        )
        val domain = entity.toDomain()
        assertEquals(entity.date, domain.date)
        assertNull(domain.constraint)
        assertNull(domain.libraryId)
        assertNull(domain.modelName)
        assertTrue(domain.tips.isEmpty())
        assertTrue(domain.frozen)
        assertTrue(domain.rerollUsed)
        assertNull(domain.seriesId)
        assertNull(domain.seriesIndex)
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun dayPrompt_roundTrip_populatedFields() {
        val entity = DayPromptEntity(
            date = date,
            title = "Kitchen still life",
            oneLiner = "Arrange breakfast as a still life.",
            details = "Use the counter as a stage.",
            constraint = "No zoom",
            theme = "Still life",
            tips = listOf("Get low", "Side light"),
            source = PromptSourceRef.ON_DEVICE_AI,
            libraryId = "lib-1",
            modelName = "nano-v2",
            generatedAt = instant,
            status = DayStatus.COMPLETED,
            frozen = false,
            rerollUsed = false,
            seriesId = 9L,
            seriesIndex = 3,
        )
        val domain = entity.toDomain()
        assertEquals(9L, domain.seriesId)
        assertEquals(3, domain.seriesIndex)
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun series_roundTrip() {
        val entity = SeriesEntity(
            id = 4,
            title = "A Week of Hands",
            startDate = date,
            endDate = date.plusDays(6),
            theme = "Hands",
            source = PromptSourceRef.ON_DEVICE_AI,
        )
        assertEquals(entity, entity.toDomain().toEntity())
    }

    @Test
    fun entry_roundTrip_nullNote() {
        val entity = EntryEntity(
            date = date,
            mediaUri = "content://media/1",
            thumbPath = "/thumbs/2026-09-19.jpg",
            capturedAt = instant,
            width = 4000,
            height = 3000,
            note = null,
            importedFromGallery = false,
            createdAt = instant,
            mediaKind = app.shutterup.domain.model.MediaKind.PHOTO,
        )
        val domain = entity.toDomain()
        assertNull(domain.note)
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun entry_roundTrip_withNoteAndImport() {
        val entity = EntryEntity(
            date = date,
            mediaUri = "content://media/2",
            thumbPath = "/thumbs/other.jpg",
            capturedAt = instant,
            width = 100,
            height = 200,
            note = "Kept the steam.",
            importedFromGallery = true,
            createdAt = instant,
            mediaKind = app.shutterup.domain.model.MediaKind.VIDEO,
        )
        assertEquals(entity, entity.toDomain().toEntity())
    }

    @Test
    fun achievement_roundTrip() {
        val entity = AchievementEntity(
            id = "first_light",
            unlockedAt = instant,
            unlockedOnDate = date,
        )
        assertEquals(entity, entity.toDomain().toEntity())
    }

    @Test
    fun streakState_roundTrip_idZeroAndNullLastProcessed() {
        val entity = StreakStateEntity(
            id = 0,
            current = 3,
            longest = 12,
            freezes = 1,
            lastProcessedDate = null,
        )
        val domain = entity.toDomain()
        assertEquals(3, domain.current)
        assertEquals(12, domain.longest)
        assertEquals(1, domain.freezes)
        assertNull(domain.lastProcessedDate)
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun streakState_toEntity_alwaysWritesIdZero() {
        val domain = StreakState(current = 4, longest = 4, freezes = 0, lastProcessedDate = date)
        val entity = domain.toEntity()
        assertEquals(0, entity.id)
        assertEquals(date, entity.lastProcessedDate)
        assertEquals(domain, entity.toDomain())
    }

    @Test
    fun supersededPrompt_toEntity_insertIdIsZero() {
        val domain = SupersededPrompt(
            id = 42,
            date = date,
            title = "Old title",
            theme = "Old theme",
            generatedAt = instant,
        )
        val entity = domain.toEntity()
        assertEquals(0, entity.id)
        assertEquals(domain.date, entity.date)
        assertEquals(domain.title, entity.title)
        assertEquals(domain.theme, entity.theme)
        assertEquals(domain.generatedAt, entity.generatedAt)
    }

    @Test
    fun libraryUsage_roundTrip() {
        val entity = LibraryUsageEntity(libraryId = "p-9", usedOnDate = date)
        val domain = entity.toDomain()
        assertEquals(LibraryUsage("p-9", date), domain)
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun monthlyIssue_roundTrip() {
        val entity = MonthlyIssueEntity(
            id = 3,
            yearMonth = "2026-09",
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30),
            completedDayCount = 24,
            headline = "Light and glass.",
            body = "Twenty-four days, mostly reflections and looking up. You wrote on nine of them.",
            dominantTheme = "Reflections",
            loudestThemes = listOf("Reflections", "Looking up"),
            source = PromptSourceRef.ON_DEVICE_AI,
            generatedAt = instant,
            dismissedFromFeed = false,
            modelName = "nano-v2",
        )
        assertEquals(entity, entity.toDomain().toEntity())
    }

    @Test
    fun observeStreak_nullRow_defaultsToZeros() = runTest {
        val repo = RoomGamificationRepository(FakeGamificationDao(streak = flowOf(null)))
        val state = repo.observeStreak().first()
        assertEquals(StreakState(0, 0, 0, null), state)
    }

    private class FakeGamificationDao(
        private val streak: Flow<StreakStateEntity?>,
    ) : GamificationDao {
        override fun observeAchievements(): Flow<List<AchievementEntity>> = flowOf(emptyList())

        override suspend fun unlock(achievement: AchievementEntity) = Unit

        override fun observeStreak(): Flow<StreakStateEntity?> = streak

        override suspend fun updateStreak(state: StreakStateEntity) = Unit

        override suspend fun recordUsage(usage: LibraryUsageEntity) = Unit

        override suspend fun libraryUsedSince(libraryId: String, since: LocalDate): Boolean = false
    }
}
