package app.shutterup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface DayPromptDao {
    @Query("SELECT * FROM day_prompts WHERE date = :date")
    fun observeDay(date: LocalDate): Flow<DayPromptEntity?>

    @Query("SELECT * FROM day_prompts WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DayPromptEntity>>

    @Query("SELECT * FROM day_prompts WHERE date = :date")
    suspend fun getDay(date: LocalDate): DayPromptEntity?

    @Upsert
    suspend fun upsert(prompt: DayPromptEntity)

    @Query("SELECT title FROM day_prompts ORDER BY date DESC LIMIT :limit")
    suspend fun recentTitles(limit: Int): List<String>

    @Query("SELECT theme FROM day_prompts ORDER BY date DESC LIMIT :limit")
    suspend fun recentThemes(limit: Int): List<String>

    @Query("SELECT * FROM day_prompts ORDER BY date DESC LIMIT :limit")
    suspend fun recentDays(limit: Int): List<DayPromptEntity>

    @Query("SELECT * FROM day_prompts ORDER BY date ASC")
    suspend fun allDays(): List<DayPromptEntity>

    @Query("DELETE FROM day_prompts WHERE date > :date")
    suspend fun deleteAfter(date: LocalDate)

    @Query("SELECT * FROM day_prompts WHERE seriesId = :seriesId ORDER BY date ASC")
    fun observeDaysInSeries(seriesId: Long): Flow<List<DayPromptEntity>>

    @Query("SELECT * FROM day_prompts WHERE seriesId = :seriesId ORDER BY date ASC")
    suspend fun daysInSeries(seriesId: Long): List<DayPromptEntity>

    @Query("SELECT * FROM day_prompts WHERE repeatsDate = :original ORDER BY date ASC")
    fun observeRepeatsOf(original: LocalDate): Flow<List<DayPromptEntity>>

    @Query("SELECT * FROM day_prompts WHERE repeatsDate = :original ORDER BY date ASC")
    suspend fun repeatsOf(original: LocalDate): List<DayPromptEntity>

    @Insert
    suspend fun recordSuperseded(prompt: SupersededPromptEntity)
}
