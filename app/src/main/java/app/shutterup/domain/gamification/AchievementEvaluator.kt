package app.shutterup.domain.gamification

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

data class AchievementInput(
    val days: List<DayPrompt>,
    val entries: List<Entry>,
    val currentStreak: Int,
    val notifyTime: LocalTime,
    val zone: ZoneId,
    val today: LocalDate,
)

object AchievementEvaluator {

    fun evaluate(input: AchievementInput, alreadyUnlocked: Set<String>): Set<String> {
        val completedDays = input.days.filter { it.status.isCompleted() }
        val completedCount = completedDays.size
        val distinctThemes = completedDays.map { it.theme }.toSet().size
        val earlyBirdCount = input.entries.count { entry ->
            val local = entry.capturedAt.atZone(input.zone).toLocalTime()
            inNotifyWindow(local, input.notifyTime)
        }
        val nightOwlCount = input.entries.count { entry ->
            entry.capturedAt.atZone(input.zone).toLocalTime().hour >= 21
        }
        val notedCount = input.entries.count { !it.note.isNullOrBlank() }

        val candidates = mutableSetOf<String>()
        if (completedCount >= 1) candidates += "first_light"
        if (input.currentStreak >= 7) candidates += "streak_7"
        if (input.currentStreak >= 30) candidates += "streak_30"
        if (input.currentStreak >= 100) candidates += "streak_100"
        if (input.currentStreak >= 365) candidates += "streak_365"
        if (completedCount >= 10) candidates += "total_10"
        if (completedCount >= 50) candidates += "total_50"
        if (completedCount >= 100) candidates += "total_100"
        if (completedCount >= 250) candidates += "total_250"
        if (completedCount >= 500) candidates += "total_500"
        if (distinctThemes >= 10) candidates += "explorer_10"
        if (distinctThemes >= 25) candidates += "explorer_25"
        if (earlyBirdCount >= 5) candidates += "early_bird"
        if (nightOwlCount >= 5) candidates += "night_owl"
        if (hasPerfectMonth(input.days, input.today)) candidates += "perfect_month"
        if (hasComeback(input.days)) candidates += "comeback"
        if (input.days.any { it.frozen }) candidates += "iceberg"
        if (notedCount >= 25) candidates += "curator"
        return candidates - alreadyUnlocked
    }

    private fun DayStatus.isCompleted(): Boolean =
        this == DayStatus.COMPLETED || this == DayStatus.COMPLETED_NO_PHOTO

    private fun inNotifyWindow(captured: LocalTime, notifyTime: LocalTime): Boolean {
        val end = notifyTime.plusMinutes(60)
        return if (end.isAfter(notifyTime)) {
            !captured.isBefore(notifyTime) && captured.isBefore(end)
        } else {
            !captured.isBefore(notifyTime) || captured.isBefore(end)
        }
    }

    private fun hasPerfectMonth(days: List<DayPrompt>, today: LocalDate): Boolean {
        val byDate = days.associateBy { it.date }
        val currentMonth = YearMonth.from(today)
        val months = days.map { YearMonth.from(it.date) }.toSet() + currentMonth
        return months.any { month -> isPerfectMonth(month, currentMonth, today, byDate) }
    }

    private fun isPerfectMonth(
        month: YearMonth,
        currentMonth: YearMonth,
        today: LocalDate,
        byDate: Map<LocalDate, DayPrompt>,
    ): Boolean {
        if (month.isAfter(currentMonth)) return false
        if (month == currentMonth && today != month.atEndOfMonth()) return false
        val start = month.atDay(1)
        val end = month.atEndOfMonth().let { if (it.isAfter(today)) today else it }
        var date = start
        var sawCompleted = false
        while (!date.isAfter(end)) {
            val day = byDate[date] ?: return false
            when (day.status) {
                DayStatus.PAUSED -> Unit
                DayStatus.COMPLETED, DayStatus.COMPLETED_NO_PHOTO -> sawCompleted = true
                else -> return false
            }
            date = date.plusDays(1)
        }
        return sawCompleted
    }

    private fun hasComeback(days: List<DayPrompt>): Boolean {
        val byDate = days.associateBy { it.date }
        for (day in days) {
            if (!day.status.isCompleted()) continue
            val d1 = byDate[day.date.minusDays(1)] ?: continue
            val d2 = byDate[day.date.minusDays(2)] ?: continue
            val d3 = byDate[day.date.minusDays(3)] ?: continue
            if (d1.status.isQuiet() && d2.status.isQuiet() && d3.status.isQuiet()) {
                return true
            }
        }
        return false
    }

    private fun DayStatus.isQuiet(): Boolean =
        this == DayStatus.MISSED || this == DayStatus.SKIPPED
}
