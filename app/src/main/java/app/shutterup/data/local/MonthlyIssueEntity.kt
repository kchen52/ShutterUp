package app.shutterup.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.shutterup.domain.model.PromptSourceRef
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "monthly_issues",
    indices = [Index(value = ["yearMonth"], unique = true)],
)
data class MonthlyIssueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val yearMonth: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val completedDayCount: Int,
    val headline: String,
    val body: String,
    val dominantTheme: String,
    val loudestThemes: List<String>,
    val source: PromptSourceRef,
    val generatedAt: Instant,
    val dismissedFromFeed: Boolean = false,
    val modelName: String? = null,
)
