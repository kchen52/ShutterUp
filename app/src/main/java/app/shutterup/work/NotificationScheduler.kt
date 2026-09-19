package app.shutterup.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.domain.scheduling.DailyNotificationScheduler
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
        val workManager = WorkManager.getInstance(context)
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

    /** Initial enqueue plus Settings notify-time / pause / theme-focus changes (M8). */
    fun onSettingsChanged() {
        scheduleNext()
        scheduleTopUp()
    }

    companion object {
        const val DAILY_WORK_NAME = "daily-prompt"
        const val DAILY_WORK_TAG = "daily-prompt"
        const val TOP_UP_WORK_NAME = "buffer-topup"
    }
}
