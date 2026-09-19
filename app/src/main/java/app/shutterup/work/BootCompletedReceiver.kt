package app.shutterup.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Exact-alarm path only: alarms do not survive reboot (SPEC §8.2).
 * The component is disabled unless Precise timing is on.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var scheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduler.scheduleNext()
        }
    }
}
