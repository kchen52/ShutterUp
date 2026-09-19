package app.shutterup.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.shutterup.data.notifications.NotificationHelper
import app.shutterup.domain.ai.GeneratePromptUseCase
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.domain.rollover.DayRolloverUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.first

@HiltWorker
class DailyPromptWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scheduler: NotificationScheduler,
    private val rollover: DayRolloverUseCase,
    private val prompts: GeneratePromptUseCase,
    private val days: DayPromptRepository,
    private val prefs: PreferencesRepository,
    private val notifications: NotificationHelper,
    private val clock: Clock,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now(clock)
            val paused = prefs.observePaused().first()
            rollover.rollover(today, paused)
            if (paused) {
                scheduler.scheduleNext()
                return Result.success()
            }
            prompts.ensureLibraryPrompt(today, prefs.observeThemeFocus().first())
            val day = checkNotNull(days.getDay(today))
            if (day.status == DayStatus.PENDING) {
                val posted = notifications.postPrompt(
                    date = today,
                    prompt = day,
                    rerollAvailable = !day.rerollUsed,
                )
                if (posted) {
                    prefs.setLastNotifiedDate(today)
                }
            }
            scheduler.scheduleNext()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
