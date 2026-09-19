package app.shutterup.capture

import app.shutterup.domain.capture.CaptureDateValidator
import app.shutterup.domain.capture.CaptureLimits
import app.shutterup.domain.capture.CompleteCaptureResult
import app.shutterup.domain.capture.CompleteCaptureUseCase
import app.shutterup.domain.capture.MediaNaming
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.MediaKind
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed class PersistOutcome {
    data class Saved(
        val result: CompleteCaptureResult.Saved,
        val mediaUri: String,
    ) : PersistOutcome()

    data object CapReached : PersistOutcome()
    data object NotToday : PersistOutcome()
    data object MissingPrompt : PersistOutcome()
    data object StorageError : PersistOutcome()
    data object Undecodable : PersistOutcome()
}

/**
 * Verifies a pending JPEG, stream-copies it into MediaStore, writes a private
 * thumbnail, and records the [Entry] (SPEC §9). Never re-encodes the original.
 */
@Singleton
class CapturePipeline @Inject constructor(
    private val files: CaptureFileStore,
    private val archiver: MediaStorePhotoArchiver,
    private val metadata: CaptureMetadataReader,
    private val thumbs: ThumbnailWriter,
    private val completeCapture: CompleteCaptureUseCase,
    private val entries: EntryRepository,
    private val prompts: DayPromptRepository,
    private val clock: Clock,
    private val zone: ZoneId,
) {
    private val mutex = Mutex()

    suspend fun persist(pending: PendingCapture, date: LocalDate): PersistOutcome =
        mutex.withLock {
            withContext(Dispatchers.IO) { persistLocked(pending, date) }
        }

    private suspend fun persistLocked(pending: PendingCapture, date: LocalDate): PersistOutcome {
        val source = File(pending.path)
        if (!metadata.isDecodable(source)) {
            return PersistOutcome.Undecodable
        }
        val today = LocalDate.now(clock.withZone(zone))
        val capturedAt = Instant.ofEpochMilli(pending.capturedAtEpoch)
        if (date != today || !CaptureDateValidator.isCapturedToday(capturedAt, today, zone)) {
            files.deleteQuietly(source)
            return PersistOutcome.NotToday
        }
        val prompt = prompts.getDay(date) ?: return PersistOutcome.MissingPrompt
        val existing = entries.listAll().filter { it.date == date }
        if (existing.size > CaptureLimits.MAX_ENTRIES_PER_DAY) {
            return PersistOutcome.CapReached
        }
        val previousUris = existing.map { it.mediaUri }
        val note = existing.lastOrNull()?.note
        val displayName = MediaNaming.displayName(date, prompt.theme)
        val archived = try {
            archiver.insertOriginal(source, displayName, capturedAt)
        } catch (_: Exception) {
            return PersistOutcome.StorageError
        }
        val thumb = files.thumbFile(date)
        if (!thumbs.write(source, thumb)) {
            runCatching { archiver.delete(archived.toString()) }
            return PersistOutcome.StorageError
        }
        val width = pending.width.takeIf { it > 0 } ?: metadata.read(source, capturedAt).width
        val height = pending.height.takeIf { it > 0 } ?: metadata.read(source, capturedAt).height
        val entry = Entry(
            date = date,
            mediaUri = archived.toString(),
            thumbPath = thumb.absolutePath,
            capturedAt = capturedAt,
            width = width,
            height = height,
            note = note,
            importedFromGallery = pending.imported,
            createdAt = clock.instant(),
            mediaKind = MediaKind.PHOTO,
        )
        return when (val result = completeCapture(entry)) {
            CompleteCaptureResult.CapReached -> {
                runCatching { archiver.delete(archived.toString()) }
                files.deleteQuietly(thumb)
                PersistOutcome.CapReached
            }
            CompleteCaptureResult.NotToday -> {
                runCatching { archiver.delete(archived.toString()) }
                files.deleteQuietly(thumb)
                files.deleteQuietly(source)
                PersistOutcome.NotToday
            }
            CompleteCaptureResult.MissingPrompt -> {
                runCatching { archiver.delete(archived.toString()) }
                files.deleteQuietly(thumb)
                PersistOutcome.MissingPrompt
            }
            is CompleteCaptureResult.Saved -> {
                files.deleteQuietly(source)
                previousUris.filter { it != archived.toString() }.forEach { old ->
                    runCatching { archiver.delete(old) }
                    files.deleteMediaUri(old)
                }
                PersistOutcome.Saved(result, archived.toString())
            }
        }
    }
}
