package app.shutterup.data.repository

import app.shutterup.data.local.DayPromptDao
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.SupersededPrompt
import app.shutterup.domain.repository.DayPromptRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomDayPromptRepository @Inject constructor(
    private val dao: DayPromptDao,
) : DayPromptRepository {
    override fun observeDay(date: LocalDate): Flow<DayPrompt?> =
        dao.observeDay(date).map { it?.toDomain() }

    override fun observeDays(start: LocalDate, endInclusive: LocalDate): Flow<List<DayPrompt>> =
        dao.observeRange(start, endInclusive).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getDay(date: LocalDate): DayPrompt? = dao.getDay(date)?.toDomain()

    override suspend fun upsert(prompt: DayPrompt) {
        dao.upsert(prompt.toEntity())
    }

    override suspend fun recordSuperseded(prompt: SupersededPrompt) {
        dao.recordSuperseded(prompt.toEntity())
    }

    override suspend fun recentTitles(limit: Int): List<String> = dao.recentTitles(limit)

    override suspend fun recentThemes(limit: Int): List<String> = dao.recentThemes(limit)

    override suspend fun recentDays(limit: Int): List<DayPrompt> =
        dao.recentDays(limit).map { it.toDomain() }

    override suspend fun allDays(): List<DayPrompt> =
        dao.allDays().map { it.toDomain() }

    override suspend fun deleteAfter(date: LocalDate) {
        dao.deleteAfter(date)
    }

    override suspend fun deleteIndependentAfter(date: LocalDate) {
        dao.deleteIndependentAfter(date)
    }

    override suspend fun deleteDaysInSeries(seriesId: Long) {
        dao.deleteDaysInSeries(seriesId)
    }

    override fun observeDaysInSeries(seriesId: Long): Flow<List<DayPrompt>> =
        dao.observeDaysInSeries(seriesId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun daysInSeries(seriesId: Long): List<DayPrompt> =
        dao.daysInSeries(seriesId).map { it.toDomain() }

    override fun observeRepeatsOf(original: LocalDate): Flow<List<DayPrompt>> =
        dao.observeRepeatsOf(original).map { rows -> rows.map { it.toDomain() } }

    override suspend fun repeatsOf(original: LocalDate): List<DayPrompt> =
        dao.repeatsOf(original).map { it.toDomain() }
}
