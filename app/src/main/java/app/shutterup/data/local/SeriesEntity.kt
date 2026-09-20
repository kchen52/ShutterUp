package app.shutterup.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.shutterup.domain.model.PromptSourceRef
import java.time.LocalDate

@Entity(tableName = "series")
data class SeriesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val theme: String,
    val source: PromptSourceRef,
)
