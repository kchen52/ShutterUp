package app.shutterup.capture

import app.shutterup.data.notifications.NotificationHelper
import app.shutterup.domain.capture.PendingCapturePolicy
import app.shutterup.domain.capture.PendingFileDisposition
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.widget.TodayWidgetUpdater
import app.shutterup.work.NotificationScheduler
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Completes a capture if the process died after the camera wrote `cache/pending/`
 * but before MediaStore save (SPEC §14). Stale files from previous days are deleted.
 */
@Singleton
class PendingCaptureRecovery @Inject constructor(
    private val files: CaptureFileStore,
    private val metadata: CaptureMetadataReader,
    private val pipeline: CapturePipeline,
    private val prompts: DayPromptRepository,
    private val notifications: NotificationHelper,
    private val widgetUpdater: TodayWidgetUpdater,
    private val scheduler: NotificationScheduler,
    private val clock: Clock,
    private val zone: ZoneId,
) {
    suspend fun recover() = withContext(Dispatchers.IO) {
        val today = LocalDate.now(clock.withZone(zone))
        val recoverable = mutableListOf<File>()
        for (file in files.listPendingFiles()) {
            val capturedAt = metadata.capturedAtForRecovery(file)
            val disposition = PendingCapturePolicy.disposition(
                fileNonEmptyAndDecodable = metadata.isDecodable(file),
                capturedAt = capturedAt,
                today = today,
                zone = zone,
            )
            when (disposition) {
                PendingFileDisposition.DISCARD -> files.deleteQuietly(file)
                PendingFileDisposition.KEEP_IN_FLIGHT -> Unit
                PendingFileDisposition.RECOVER -> recoverable += file
            }
        }
        val prompt = prompts.getDay(today)
        val canComplete = prompt != null && prompt.status in RECOVERABLE_STATUSES
        if (!canComplete) {
            recoverable.forEach { files.deleteQuietly(it) }
            return@withContext
        }
        val newest = recoverable.maxByOrNull { it.lastModified() } ?: return@withContext
        recoverable.filter { it != newest }.forEach { files.deleteQuietly(it) }
        val meta = metadata.read(newest, metadata.capturedAtForRecovery(newest))
        val outcome = pipeline.persist(
            PendingCapture(
                path = newest.absolutePath,
                capturedAtEpoch = meta.capturedAt.toEpochMilli(),
                width = meta.width,
                height = meta.height,
                imported = false,
            ),
            today,
        )
        if (outcome is PersistOutcome.Saved) {
            runCatching { notifications.cancel(today) }
            runCatching { widgetUpdater.refresh() }
            runCatching { scheduler.scheduleTopUpNow() }
        }
    }

    private companion object {
        val RECOVERABLE_STATUSES = setOf(
            DayStatus.PENDING,
            DayStatus.COMPLETED,
            DayStatus.COMPLETED_NO_PHOTO,
        )
    }
}
