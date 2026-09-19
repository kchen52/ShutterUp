package app.shutterup.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "library_usage")
data class LibraryUsageEntity(
    @PrimaryKey val libraryId: String,
    val usedOnDate: LocalDate,
)
