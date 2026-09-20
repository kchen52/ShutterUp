package app.shutterup.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.shutterup.domain.ai.GenerateMonthlyIssueUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.withTimeout

/**
 * Background assembly of The Monthly. Idempotent: [GenerateMonthlyIssueUseCase]
 * writes each finished month once. Never posts a notification.
 */
@HiltWorker
class MonthlyIssueWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val generate: GenerateMonthlyIssueUseCase,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            withTimeout(3.minutes) {
                generate.generateDue()
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
