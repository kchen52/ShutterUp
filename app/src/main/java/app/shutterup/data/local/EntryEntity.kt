package app.shutterup.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey val date: LocalDate,
    val mediaUri: String,
    val thumbPath: String,
    val capturedAt: Instant,
    val width: Int,
    val height: Int,
    val note: String?,
    val importedFromGallery: Boolean,
    val createdAt: Instant,
)
