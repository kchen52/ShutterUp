package app.shutterup.domain.scheduling

/**
 * Chooses the daily-prompt trigger path (SPEC §8.2).
 *
 * Exact alarms are used only when Precise timing is on **and**
 * `AlarmManager.canScheduleExactAlarms()` is true. Otherwise the inexact
 * WorkManager path is used (including when the special permission is revoked).
 */
object PreciseTimingPolicy {
    fun useExactAlarm(preciseEnabled: Boolean, canScheduleExactAlarms: Boolean): Boolean =
        preciseEnabled && canScheduleExactAlarms
}
