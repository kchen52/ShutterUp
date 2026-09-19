package app.shutterup.domain.scheduling

import app.shutterup.domain.model.DayStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object DailyNotificationScheduler {
    /**
     * Null = do not schedule (paused). [todayNotified] = today's notification already fired
     * (prevents double-fire when rescheduling after posting).
     *
     * Trigger instants are computed with [ZonedDateTime] in [zone] so DST is respected.
     * A [notifyTime] that falls in a spring-forward gap is resolved by shifting forward
     * (Java `ofLocal`: the local time is moved later by the length of the gap).
     */
    fun nextTrigger(
        now: Instant,
        notifyTime: LocalTime,
        todayStatus: DayStatus?,
        todayNotified: Boolean,
        paused: Boolean,
        zone: ZoneId,
    ): Instant? {
        if (paused) return null
        val today = LocalDate.ofInstant(now, zone)
        if (todayStatus.isDoneForToday()) {
            return atNotifyTime(today.plusDays(1), notifyTime, zone)
        }
        if (todayNotified) {
            return atNotifyTime(today.plusDays(1), notifyTime, zone)
        }
        val trigger = atNotifyTime(today, notifyTime, zone)
        return if (now < trigger) trigger else now.plusSeconds(60)
    }

    private fun DayStatus?.isDoneForToday(): Boolean = when (this) {
        DayStatus.COMPLETED, DayStatus.COMPLETED_NO_PHOTO, DayStatus.SKIPPED -> true
        else -> false
    }

    private fun atNotifyTime(date: LocalDate, notifyTime: LocalTime, zone: ZoneId): Instant =
        ZonedDateTime.of(date, notifyTime, zone).toInstant()
}
