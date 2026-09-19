package app.shutterup.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE date = :date ORDER BY createdAt ASC")
    fun observeEntries(date: LocalDate): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE date = :date ORDER BY createdAt DESC LIMIT 1")
    fun observeEntry(date: LocalDate): Flow<EntryEntity?>

    @Query("SELECT * FROM entries ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<EntryEntity>>

    @Query(
        """
        SELECT entries.* FROM entries
        INNER JOIN day_prompts ON entries.date = day_prompts.date
        WHERE day_prompts.theme = :theme
        ORDER BY entries.date DESC, entries.createdAt DESC
        """,
    )
    fun observeByTheme(theme: String): Flow<List<EntryEntity>>

    @Upsert
    suspend fun upsert(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE date = :date")
    suspend fun delete(date: LocalDate)

    @Query("SELECT COUNT(*) FROM entries")
    suspend fun count(): Int

    /** App-level cap is [app.shutterup.domain.capture.CaptureLimits.MAX_ENTRIES_PER_DAY] (1). */
    @Query("SELECT COUNT(*) FROM entries WHERE date = :date")
    suspend fun countForDate(date: LocalDate): Int

    @Query("SELECT * FROM entries ORDER BY date ASC, createdAt ASC")
    suspend fun listAll(): List<EntryEntity>
}
