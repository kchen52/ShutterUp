package app.shutterup.data.repository

import app.shutterup.data.local.MonthlyIssueDao
import app.shutterup.domain.model.MonthlyIssue
import app.shutterup.domain.repository.MonthlyIssueRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomMonthlyIssueRepository @Inject constructor(
    private val dao: MonthlyIssueDao,
) : MonthlyIssueRepository {
    override fun observeAll(): Flow<List<MonthlyIssue>> = dao.observeAll().map { rows ->
        rows.map { it.toDomain() }
    }

    override fun observe(yearMonth: String): Flow<MonthlyIssue?> =
        dao.observe(yearMonth).map { it?.toDomain() }

    override suspend fun get(yearMonth: String): MonthlyIssue? = dao.get(yearMonth)?.toDomain()

    override suspend fun all(): List<MonthlyIssue> = dao.all().map { it.toDomain() }

    override suspend fun insert(issue: MonthlyIssue): Long = dao.insert(issue.toEntity())

    override suspend fun dismissFromFeed(yearMonth: String) {
        dao.dismissFromFeed(yearMonth)
    }
}
