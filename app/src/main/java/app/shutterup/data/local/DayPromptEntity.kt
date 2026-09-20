package app.shutterup.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.PromptSourceRef
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "day_prompts",
    indices = [
        Index(value = ["seriesId"]),
        Index(value = ["repeatsDate"]),
    ],
)
data class DayPromptEntity(
    @PrimaryKey val date: LocalDate,
    val title: String,
    val oneLiner: String,
    val details: String,
    val constraint: String?,
    val theme: String,
    val tips: List<String>,
    val source: PromptSourceRef,
    val libraryId: String?,
    val modelName: String?,
    val generatedAt: Instant,
    val status: DayStatus,
    val frozen: Boolean,
    val rerollUsed: Boolean,
    val seriesId: Long? = null,
    val seriesIndex: Int? = null,
    val repeatsDate: LocalDate? = null,
)
