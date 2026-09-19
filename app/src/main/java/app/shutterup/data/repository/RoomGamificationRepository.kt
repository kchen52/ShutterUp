package app.shutterup.data.repository

import app.shutterup.data.local.GamificationDao
import app.shutterup.domain.model.Achievement
import app.shutterup.domain.model.LibraryUsage
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.repository.GamificationRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomGamificationRepository @Inject constructor(
    private val dao: GamificationDao,
) : GamificationRepository {
    override fun observeAchievements(): Flow<List<Achievement>> =
        dao.observeAchievements().map { rows -> rows.map { it.toDomain() } }

    override suspend fun unlock(achievement: Achievement) {
        dao.unlock(achievement.toEntity())
    }

    override fun observeStreak(): Flow<StreakState> =
        dao.observeStreak().map { it?.toDomain() ?: StreakState(0, 0, 0, null) }

    override suspend fun updateStreak(state: StreakState) {
        dao.updateStreak(state.toEntity())
    }

    override suspend fun recordLibraryUsage(usage: LibraryUsage) {
        dao.recordUsage(usage.toEntity())
    }

    override suspend fun libraryUsedSince(libraryId: String, since: LocalDate): Boolean =
        dao.libraryUsedSince(libraryId, since)
}
