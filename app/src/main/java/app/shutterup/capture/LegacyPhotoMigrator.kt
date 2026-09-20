package app.shutterup.capture

import app.shutterup.domain.capture.MediaNaming
import app.shutterup.domain.capture.MigrationDecision
import app.shutterup.domain.capture.PhotoMigrationPolicy
import app.shutterup.domain.model.Entry
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One-time copy of app-private originals into Pictures/ShutterUp.
 * Idempotent, per-entry, safe to interrupt: already-migrated URIs are skipped,
 * unreadable files keep working off their thumbnail.
 */
@Singleton
class LegacyPhotoMigrator @Inject constructor(
    private val entries: EntryRepository,
    private val prompts: DayPromptRepository,
    private val archiver: MediaStorePhotoArchiver,
    private val files: CaptureFileStore,
) {
    suspend fun migrate() = withContext(Dispatchers.IO) {
        for (entry in entries.listAll()) {
            migrateOne(entry)
        }
    }

    internal suspend fun migrateOne(entry: Entry) {
        val privateFile = files.resolvePrivateFile(entry.mediaUri)
        val sourceReadable = privateFile != null || files.openReadable(entry.mediaUri) != null
        when (PhotoMigrationPolicy.decide(entry.mediaUri, sourceReadable)) {
            MigrationDecision.AlreadyMigrated, MigrationDecision.KeepThumbnailFallback -> return
            MigrationDecision.CopyToMediaStore -> {
                val displayName = MediaNaming.displayName(
                    entry.date,
                    prompts.getDay(entry.date)?.theme ?: "untitled",
                )
                try {
                    val uri = if (privateFile != null) {
                        archiver.insertOriginal(privateFile, displayName, entry.capturedAt)
                    } else {
                        files.openReadable(entry.mediaUri)?.use { input ->
                            archiver.insertOriginalFromStream(
                                input,
                                sizeHint = 0L,
                                displayName = displayName,
                                capturedAt = entry.capturedAt,
                            )
                        } ?: return
                    }
                    entries.upsert(entry.copy(mediaUri = uri.toString()))
                    files.deleteQuietly(privateFile)
                } catch (_: Exception) {
                    // Thumbnail remains the fallback (SPEC §14).
                }
            }
        }
    }
}
