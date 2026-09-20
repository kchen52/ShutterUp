package app.shutterup.domain.repository

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.LibraryUsage
import app.shutterup.domain.model.Series
import app.shutterup.domain.model.SupersededPrompt
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Room-backed in the persistence milestone (SPEC §10). */
interface DayPromptRepository {
    fun observeDay(date: LocalDate): Flow<DayPrompt?>
    fun observeDays(start: LocalDate, endInclusive: LocalDate): Flow<List<DayPrompt>>
    suspend fun getDay(date: LocalDate): DayPrompt?
    suspend fun upsert(prompt: DayPrompt)
    suspend fun recordSuperseded(prompt: SupersededPrompt)
    suspend fun recentTitles(limit: Int): List<String>
    suspend fun recentThemes(limit: Int): List<String>
    /** Newest first; backs dedup and reroll history. */
    suspend fun recentDays(limit: Int): List<DayPrompt>
    /** Every persisted day, oldest first. */
    suspend fun allDays(): List<DayPrompt>
    /** Drops un-shown future buffer prompts after [date] (SPEC §7.5 / §14). */
    suspend fun deleteAfter(date: LocalDate)
    fun observeDaysInSeries(seriesId: Long): Flow<List<DayPrompt>>
    suspend fun daysInSeries(seriesId: Long): List<DayPrompt>
}

interface SeriesRepository {
    fun observe(id: Long): Flow<Series?>
    fun observeCovering(date: LocalDate): Flow<Series?>
    suspend fun get(id: Long): Series?
    suspend fun covering(date: LocalDate): Series?
    suspend fun latest(): Series?
    suspend fun all(): List<Series>
    suspend fun insert(series: Series): Long
    suspend fun update(series: Series)
    suspend fun delete(id: Long)
}

interface EntryRepository {
    fun observeEntry(date: LocalDate): Flow<Entry?>
    fun observeEntries(date: LocalDate): Flow<List<Entry>>
    fun observeRecentEntries(limit: Int): Flow<List<Entry>>
    fun observeEntriesByTheme(theme: String): Flow<List<Entry>>
    suspend fun upsert(entry: Entry)
    suspend fun delete(date: LocalDate)
    suspend fun count(): Int
    suspend fun countForDate(date: LocalDate): Int
    suspend fun listAll(): List<Entry>
}

interface GamificationRepository {
    fun observeAchievements(): Flow<List<app.shutterup.domain.model.Achievement>>
    suspend fun unlock(achievement: app.shutterup.domain.model.Achievement)
    fun observeStreak(): Flow<app.shutterup.domain.model.StreakState>
    suspend fun updateStreak(state: app.shutterup.domain.model.StreakState)
    suspend fun recordLibraryUsage(usage: LibraryUsage)
    suspend fun libraryUsedSince(libraryId: String, since: LocalDate): Boolean
}
