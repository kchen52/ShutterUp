package app.shutterup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyIssueDao {
    @Query("SELECT * FROM monthly_issues ORDER BY yearMonth DESC")
    fun observeAll(): Flow<List<MonthlyIssueEntity>>

    @Query("SELECT * FROM monthly_issues WHERE yearMonth = :yearMonth")
    fun observe(yearMonth: String): Flow<MonthlyIssueEntity?>

    @Query("SELECT * FROM monthly_issues WHERE yearMonth = :yearMonth")
    suspend fun get(yearMonth: String): MonthlyIssueEntity?

    @Query("SELECT * FROM monthly_issues ORDER BY yearMonth DESC")
    suspend fun all(): List<MonthlyIssueEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(issue: MonthlyIssueEntity): Long

    @Query("UPDATE monthly_issues SET dismissedFromFeed = 1 WHERE yearMonth = :yearMonth")
    suspend fun dismissFromFeed(yearMonth: String)
}
