package app.shutterup.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.shutterup.domain.model.MediaKind
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "entries",
    indices = [Index(value = ["date"])],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val mediaUri: String,
    val thumbPath: String,
    val capturedAt: Instant,
    val width: Int,
    val height: Int,
    val note: String?,
    val importedFromGallery: Boolean,
    val createdAt: Instant,
    val mediaKind: MediaKind,
)
