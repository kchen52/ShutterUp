package app.shutterup.work

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Re-evaluates exact vs WorkManager when the user grants or revokes
 * [android.Manifest.permission.SCHEDULE_EXACT_ALARM] (SPEC §8.2).
 */
@AndroidEntryPoint
class ExactAlarmPermissionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var scheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            scheduler.scheduleNext()
        }
    }
}
