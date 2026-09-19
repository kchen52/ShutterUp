package app.shutterup.capture

import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Removes a day's photo. The day stays [DayStatus.COMPLETED_NO_PHOTO] (SPEC §4.5).
 * Gallery deletion is best-effort: a failure still clears the ShutterUp entry.
 */
class DeleteDayPhotoUseCase @Inject constructor(
    private val entries: EntryRepository,
    private val prompts: DayPromptRepository,
    private val archiver: MediaStorePhotoArchiver,
    private val files: CaptureFileStore,
) {
    suspend operator fun invoke(date: LocalDate, alsoFromGallery: Boolean) {
        withContext(Dispatchers.IO) {
            val current = entries.listAll().filter { it.date == date }
            if (alsoFromGallery) {
                current.forEach { entry ->
                    runCatching { archiver.delete(entry.mediaUri) }
                    files.deleteMediaUri(entry.mediaUri)
                }
            }
            current.forEach { entry -> files.deleteQuietly(File(entry.thumbPath)) }
            entries.delete(date)
            val prompt = prompts.getDay(date)
            if (prompt != null &&
                (prompt.status == DayStatus.COMPLETED || prompt.status == DayStatus.COMPLETED_NO_PHOTO)
            ) {
                prompts.upsert(prompt.copy(status = DayStatus.COMPLETED_NO_PHOTO))
            }
        }
    }
}
