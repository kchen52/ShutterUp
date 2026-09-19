package app.shutterup.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.shutterup.domain.ai.GeneratePromptUseCase
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.domain.rollover.DayRolloverUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

@HiltWorker
class BufferTopUpWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val rollover: DayRolloverUseCase,
    private val prompts: GeneratePromptUseCase,
    private val prefs: PreferencesRepository,
    private val clock: Clock,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now(clock)
            val paused = prefs.observePaused().first()
            val focus = prefs.observeThemeFocus().first()
            withTimeout(3.minutes) {
                rollover.rollover(today, paused)
                prompts.topUpBuffer(today, focus, 2)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
