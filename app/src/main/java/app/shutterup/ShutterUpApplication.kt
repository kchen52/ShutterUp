package app.shutterup

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import app.shutterup.capture.LegacyPhotoMigrator
import app.shutterup.capture.PendingCaptureRecovery
import app.shutterup.widget.TodayWidgetUpdater
import app.shutterup.work.NotificationScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class ShutterUpApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Never block the main thread at startup: scheduleNext() awaits
        // DataStore + Room and would stall first-frame rendering (and, under
        // memory pressure, make the whole system feel the stall).
        val entry = EntryPointAccessors.fromApplication(this, SchedulerEntryPoint::class.java)
        entry.appScope().launch {
            runCatching { entry.legacyPhotoMigrator().migrate() }
            runCatching { entry.pendingCaptureRecovery().recover() }
            runCatching { entry.notificationScheduler().onSettingsChanged() }
            entry.todayWidgetUpdater().refreshAsync()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SchedulerEntryPoint {
    @Named("appScope")
    fun appScope(): CoroutineScope
    fun notificationScheduler(): NotificationScheduler
    fun todayWidgetUpdater(): TodayWidgetUpdater
    fun legacyPhotoMigrator(): LegacyPhotoMigrator
    fun pendingCaptureRecovery(): PendingCaptureRecovery
}
