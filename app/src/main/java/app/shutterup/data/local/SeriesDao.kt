package app.shutterup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series WHERE id = :id")
    fun observe(id: Long): Flow<SeriesEntity?>

    @Query("SELECT * FROM series WHERE startDate <= :date AND endDate >= :date LIMIT 1")
    fun observeCovering(date: LocalDate): Flow<SeriesEntity?>

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun get(id: Long): SeriesEntity?

    @Query("SELECT * FROM series WHERE startDate <= :date AND endDate >= :date LIMIT 1")
    suspend fun covering(date: LocalDate): SeriesEntity?

    @Query("SELECT * FROM series ORDER BY endDate DESC LIMIT 1")
    suspend fun latest(): SeriesEntity?

    @Query("SELECT * FROM series ORDER BY startDate ASC")
    suspend fun all(): List<SeriesEntity>

    @Insert
    suspend fun insert(series: SeriesEntity): Long

    @Update
    suspend fun update(series: SeriesEntity)

    @Query("DELETE FROM series WHERE id = :id")
    suspend fun delete(id: Long)
}
