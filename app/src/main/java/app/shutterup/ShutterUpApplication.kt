package app.shutterup

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import app.shutterup.work.NotificationScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

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
        EntryPointAccessors.fromApplication(this, SchedulerEntryPoint::class.java)
            .notificationScheduler()
            .onSettingsChanged()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SchedulerEntryPoint {
    fun notificationScheduler(): NotificationScheduler
}
