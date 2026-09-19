package app.shutterup.data.repository

import app.shutterup.data.local.EntryDao
import app.shutterup.domain.model.Entry
import app.shutterup.domain.repository.EntryRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomEntryRepository @Inject constructor(
    private val dao: EntryDao,
) : EntryRepository {
    override fun observeEntry(date: LocalDate): Flow<Entry?> =
        dao.observeEntry(date).map { it?.toDomain() }

    override fun observeRecentEntries(limit: Int): Flow<List<Entry>> =
        dao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeEntriesByTheme(theme: String): Flow<List<Entry>> =
        dao.observeByTheme(theme).map { rows -> rows.map { it.toDomain() } }

    override suspend fun upsert(entry: Entry) {
        dao.upsert(entry.toEntity())
    }

    override suspend fun delete(date: LocalDate) {
        dao.delete(date)
    }

    override suspend fun count(): Int = dao.count()
}
