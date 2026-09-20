package app.shutterup.data.repository

import app.shutterup.data.local.SeriesDao
import app.shutterup.domain.model.Series
import app.shutterup.domain.repository.SeriesRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomSeriesRepository @Inject constructor(
    private val dao: SeriesDao,
) : SeriesRepository {
    override fun observe(id: Long): Flow<Series?> = dao.observe(id).map { it?.toDomain() }

    override fun observeCovering(date: LocalDate): Flow<Series?> =
        dao.observeCovering(date).map { it?.toDomain() }

    override suspend fun get(id: Long): Series? = dao.get(id)?.toDomain()

    override suspend fun covering(date: LocalDate): Series? = dao.covering(date)?.toDomain()

    override suspend fun latest(): Series? = dao.latest()?.toDomain()

    override suspend fun all(): List<Series> = dao.all().map { it.toDomain() }

    override suspend fun insert(series: Series): Long = dao.insert(series.toEntity())

    override suspend fun update(series: Series) {
        dao.update(series.toEntity())
    }

    override suspend fun delete(id: Long) {
        dao.delete(id)
    }
}
