package app.shutterup.domain.gamification

import app.shutterup.domain.model.DayStatus
import java.time.LocalDate

data class DayRecord(
    val date: LocalDate,
    val status: DayStatus,
    val frozen: Boolean,
)

object StreakCalculator {

    fun currentStreak(days: List<DayRecord>, today: LocalDate): Int {
        if (days.isEmpty()) return 0
        val byDate = days.associateBy { it.date }
        val todayRecord = byDate[today]
        val start = if (todayRecord == null || todayRecord.status == DayStatus.PENDING) {
            today.minusDays(1)
        } else {
            today
        }
        val earliest = byDate.keys.minOrNull() ?: return 0
        var date = start
        var streak = 0
        while (!date.isBefore(earliest)) {
            val record = byDate[date] ?: break
            when (classify(record)) {
                DayKind.COMPLETED -> streak++
                DayKind.NEUTRAL -> Unit
                DayKind.BREAK -> break
            }
            date = date.minusDays(1)
        }
        return streak
    }

    fun longestStreak(days: List<DayRecord>): Int {
        if (days.isEmpty()) return 0
        val ordered = days.associateBy { it.date }.values.sortedBy { it.date }
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (record in ordered) {
            if (previous != null && record.date != previous.plusDays(1)) {
                run = 0
            }
            when (classify(record)) {
                DayKind.COMPLETED -> {
                    run++
                    if (run > best) best = run
                }
                DayKind.NEUTRAL -> Unit
                DayKind.BREAK -> run = 0
            }
            previous = record.date
        }
        return best
    }

    private enum class DayKind { COMPLETED, NEUTRAL, BREAK }

    private fun classify(record: DayRecord): DayKind = when (record.status) {
        DayStatus.COMPLETED, DayStatus.COMPLETED_NO_PHOTO -> DayKind.COMPLETED
        DayStatus.PAUSED -> DayKind.NEUTRAL
        DayStatus.MISSED, DayStatus.SKIPPED ->
            if (record.frozen) DayKind.NEUTRAL else DayKind.BREAK
        DayStatus.PENDING -> DayKind.BREAK
    }
}
