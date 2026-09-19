package app.shutterup.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface GamificationDao {
    @Query("SELECT * FROM achievements")
    fun observeAchievements(): Flow<List<AchievementEntity>>

    @Upsert
    suspend fun unlock(achievement: AchievementEntity)

    @Query("SELECT * FROM streak_state WHERE id = 0")
    fun observeStreak(): Flow<StreakStateEntity?>

    @Upsert
    suspend fun updateStreak(state: StreakStateEntity)

    @Upsert
    suspend fun recordUsage(usage: LibraryUsageEntity)

    @Query(
        "SELECT EXISTS(SELECT 1 FROM library_usage WHERE libraryId = :libraryId AND usedOnDate >= :since)",
    )
    suspend fun libraryUsedSince(libraryId: String, since: LocalDate): Boolean
}
