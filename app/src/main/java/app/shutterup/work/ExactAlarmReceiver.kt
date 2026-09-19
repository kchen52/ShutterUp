package app.shutterup.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Fires at the exact notify time (SPEC §8.2). Hands work to [DailyPromptWorker]
 * so posting, rollover, widget refresh, and reschedule stay in one place.
 */
class ExactAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val request = OneTimeWorkRequestBuilder<DailyPromptWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            NotificationScheduler.DAILY_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    companion object {
        const val ACTION = "app.shutterup.action.DAILY_EXACT_ALARM"
    }
}
