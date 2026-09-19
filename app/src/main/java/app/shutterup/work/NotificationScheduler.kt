package app.shutterup.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.domain.scheduling.DailyNotificationScheduler
import app.shutterup.domain.scheduling.PreciseTimingPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesRepository,
    private val days: DayPromptRepository,
    private val clock: Clock,
    private val zone: ZoneId,
) {
    fun scheduleNext() = runBlocking {
        val today = LocalDate.now(clock)
        val trigger = DailyNotificationScheduler.nextTrigger(
            now = clock.instant(),
            notifyTime = prefs.observeNotifyTime().first(),
            todayStatus = days.getDay(today)?.status,
            todayNotified = prefs.observeLastNotifiedDate().first() == today,
            paused = prefs.observePaused().first(),
            zone = zone,
        )
        val precise = prefs.observePreciseTiming().first()
        setBootReceiverEnabled(precise)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val canExact = alarmManager?.canScheduleExactAlarms() == true
        val useExact = PreciseTimingPolicy.useExactAlarm(precise, canExact)
        val workManager = WorkManager.getInstance(context)
        if (useExact) {
            workManager.cancelUniqueWork(DAILY_WORK_NAME)
            if (trigger == null) {
                cancelExactAlarm(alarmManager)
                return@runBlocking
            }
            val pending = exactPendingIntent(create = true) ?: return@runBlocking
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger.toEpochMilli(),
                pending,
            )
            return@runBlocking
        }
        cancelExactAlarm(alarmManager)
        if (trigger == null) {
            workManager.cancelUniqueWork(DAILY_WORK_NAME)
            return@runBlocking
        }
        val delay = Duration.between(clock.instant(), trigger).let { duration ->
            if (duration.isNegative) Duration.ZERO else duration
        }
        val request = OneTimeWorkRequestBuilder<DailyPromptWorker>()
            .setInitialDelay(delay)
            .addTag(DAILY_WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(DAILY_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun scheduleTopUp() {
        val request = PeriodicWorkRequestBuilder<BufferTopUpWorker>(24, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresCharging(true)
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .addTag(TOP_UP_WORK_NAME)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TOP_UP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun scheduleMonthlyIssue() {
        val request = PeriodicWorkRequestBuilder<MonthlyIssueWorker>(24, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .addTag(MONTHLY_WORK_NAME)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MONTHLY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * One-shot catch-up after process start or a month boundary the device slept
     * through. KEEP: overlapping opens collapse into one run.
     */
    fun scheduleMonthlyIssueNow() {
        val request = OneTimeWorkRequestBuilder<MonthlyIssueWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .addTag(MONTHLY_NOW_NAME)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            MONTHLY_NOW_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    /** Initial enqueue plus Settings notify-time / pause / theme-focus changes (M8). */
    fun onSettingsChanged() {
        scheduleNext()
        scheduleTopUp()
        scheduleMonthlyIssue()
        scheduleMonthlyIssueNow()
    }

    /**
     * Opportunistic buffer fill after a capture completes: one-time top-up
     * without the charging constraint, so tomorrow's prompt generates while
     * the user is on the Completion screen. KEEP: overlapping saves collapse
     * into one run; the worker itself no-ops when the buffer is full.
     */
    fun scheduleTopUpNow() {
        val request = OneTimeWorkRequestBuilder<BufferTopUpWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .addTag(TOP_UP_NOW_NAME)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            TOP_UP_NOW_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun exactPendingIntent(create: Boolean): PendingIntent? {
        val intent = Intent(context, ExactAlarmReceiver::class.java).setAction(ExactAlarmReceiver.ACTION)
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, EXACT_REQUEST_CODE, intent, flags)
    }

    private fun cancelExactAlarm(alarmManager: AlarmManager?) {
        val existing = exactPendingIntent(create = false) ?: return
        alarmManager?.cancel(existing)
        existing.cancel()
    }

    private fun setBootReceiverEnabled(enabled: Boolean) {
        val component = ComponentName(context, BootCompletedReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            component,
            if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            },
            PackageManager.DONT_KILL_APP,
        )
    }

    companion object {
        const val DAILY_WORK_NAME = "daily-prompt"
        const val DAILY_WORK_TAG = "daily-prompt"
        const val TOP_UP_WORK_NAME = "buffer-topup"
        const val TOP_UP_NOW_NAME = "buffer-topup-now"
        const val TOP_UP_NOW_TAG = "buffer-topup-now"
        const val MONTHLY_WORK_NAME = "monthly-issue"
        const val MONTHLY_NOW_NAME = "monthly-issue-now"
        const val EXACT_REQUEST_CODE = 7109
    }
}
