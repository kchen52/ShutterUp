package app.shutterup.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "streak_state")
data class StreakStateEntity(
    @PrimaryKey val id: Int,
    val current: Int,
    val longest: Int,
    val freezes: Int,
    val lastProcessedDate: LocalDate?,
)
