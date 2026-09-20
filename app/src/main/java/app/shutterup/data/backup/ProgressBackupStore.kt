package app.shutterup.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import app.shutterup.capture.CaptureFileStore
import app.shutterup.capture.MediaStorePhotoArchiver
import app.shutterup.capture.ThumbnailWriter
import app.shutterup.data.local.DayPromptDao
import app.shutterup.data.local.EntryDao
import app.shutterup.data.local.EntryEntity
import app.shutterup.data.local.GamificationDao
import app.shutterup.data.local.MonthlyIssueDao
import app.shutterup.data.local.SeriesDao
import app.shutterup.data.local.ShutterUpDatabase
import app.shutterup.domain.capture.MediaNaming
import app.shutterup.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Writes and restores a ZIP of Room + prefs + thumbnails + Gallery originals.
 * Photos already live in Pictures/ShutterUp and usually survive uninstall;
 * copies in the ZIP cover the case where they do not.
 */
@Singleton
class ProgressBackupStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ShutterUpDatabase,
    private val prompts: DayPromptDao,
    private val entries: EntryDao,
    private val gamification: GamificationDao,
    private val series: SeriesDao,
    private val monthlyIssues: MonthlyIssueDao,
    private val preferences: PreferencesRepository,
    private val files: CaptureFileStore,
    private val photos: MediaStorePhotoArchiver,
    private val thumbs: ThumbnailWriter,
    private val clock: Clock,
) {
    suspend fun exportTo(output: OutputStream) {
        val payload = snapshot()
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(ProgressBackupFormat.JSON_NAME))
            zip.write(ProgressBackupJson.encode(payload).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            payload.entries.forEach { row ->
                copyZipFile(zip, row.thumbZip, File(row.entity.thumbPath))
                row.photoZip?.let { name ->
                    files.openReadable(row.entity.mediaUri)?.use { input ->
                        zip.putNextEntry(ZipEntry(name))
                        input.copyTo(zip)
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    suspend fun importFrom(input: InputStream) {
        val scratch = File(context.cacheDir, "backup-restore-${System.nanoTime()}")
        scratch.mkdirs()
        try {
            unzip(input, scratch)
            val jsonFile = File(scratch, ProgressBackupFormat.JSON_NAME)
            if (!jsonFile.isFile) {
                throw ProgressBackupException("That file isn't a ShutterUp backup.")
            }
            val payload = ProgressBackupJson.decode(jsonFile.readText(Charsets.UTF_8))
            restore(payload, scratch)
        } finally {
            scratch.deleteRecursively()
        }
    }

    private suspend fun snapshot(): ProgressBackup {
        val notify = preferences.observeNotifyTime().first()
        val lastNotified = preferences.observeLastNotifiedDate().first()
        val dayRows = prompts.allDays()
        val themeByDate = dayRows.associate { it.date to it.theme }
        val entryRows = entries.listAll().map { entity ->
            val displayName = runCatching { photos.queryDisplayName(Uri.parse(entity.mediaUri)) }.getOrNull()
                ?: MediaNaming.displayName(entity.date, themeByDate[entity.date] ?: "untitled")
            val stem = "${entity.date}_${entity.id}.jpg"
            val thumbFile = File(entity.thumbPath)
            val hasOriginal = files.openReadable(entity.mediaUri)?.use { true } ?: false
            EntryBackup(
                entity = entity,
                displayName = displayName,
                thumbZip = if (thumbFile.isFile) ProgressBackupFormat.THUMBS_DIR + stem else null,
                photoZip = if (hasOriginal) ProgressBackupFormat.PHOTOS_DIR + stem else null,
            )
        }
        return ProgressBackup(
            format = ProgressBackupFormat.VERSION,
            createdAt = clock.instant(),
            prefs = PrefsBackup(
                notifyHour = notify.hour,
                notifyMinute = notify.minute,
                preciseTiming = preferences.observePreciseTiming().first(),
                themeFocus = preferences.observeThemeFocus().first(),
                seriesEnabled = preferences.observeSeriesEnabled().first(),
                paused = preferences.observePaused().first(),
                onboardingComplete = preferences.observeOnboardingComplete().first(),
                debugUseFakeAi = preferences.observeDebugUseFakeAi().first(),
                lastNotifiedDate = lastNotified?.toString(),
                coarseCityId = preferences.observeCoarseCityId().first(),
            ),
            prompts = dayRows,
            superseded = prompts.allSuperseded(),
            entries = entryRows,
            achievements = gamification.allAchievements(),
            streak = gamification.getStreak(),
            libraryUsage = gamification.allLibraryUsage(),
            series = series.all(),
            monthlyIssues = monthlyIssues.all(),
        )
    }

    private suspend fun restore(payload: ProgressBackup, scratch: File) {
        database.clearAllTables()
        database.withTransaction {
            if (payload.series.isNotEmpty()) series.insertAll(payload.series)
            if (payload.prompts.isNotEmpty()) prompts.upsertAll(payload.prompts)
            if (payload.superseded.isNotEmpty()) prompts.insertSuperseded(payload.superseded)
            if (payload.achievements.isNotEmpty()) gamification.unlockAll(payload.achievements)
            payload.streak?.let { gamification.updateStreak(it) }
            if (payload.libraryUsage.isNotEmpty()) gamification.recordUsageAll(payload.libraryUsage)
            if (payload.monthlyIssues.isNotEmpty()) monthlyIssues.insertAll(payload.monthlyIssues)
            if (payload.entries.isNotEmpty()) {
                val restored = payload.entries.map { row -> relinkEntry(row, scratch) }
                entries.upsertAll(restored)
            }
        }
        restorePrefs(payload.prefs)
    }

    private fun relinkEntry(row: EntryBackup, scratch: File): EntryEntity {
        val mediaUri = relinkOriginal(row, scratch)
        val thumbDest = files.thumbFile(row.entity.date)
        restoreThumb(row, scratch, thumbDest, mediaUri)
        return row.entity.copy(
            mediaUri = mediaUri,
            thumbPath = thumbDest.absolutePath,
        )
    }

    private fun restoreThumb(row: EntryBackup, scratch: File, dest: File, mediaUri: String) {
        val zipped = row.thumbZip?.let { safeChild(scratch, it) }
        if (zipped != null && zipped.isFile) {
            dest.parentFile?.mkdirs()
            zipped.copyTo(dest, overwrite = true)
            return
        }
        val photo = row.photoZip?.let { safeChild(scratch, it) }
        if (photo != null && photo.isFile && thumbs.write(photo, dest)) return
        files.openReadable(mediaUri)?.use { input ->
            val tmp = File.createTempFile("thumb-src", ".jpg", files.pendingDir())
            try {
                tmp.outputStream().use { input.copyTo(it) }
                thumbs.write(tmp, dest)
            } finally {
                tmp.delete()
            }
        }
    }

    private fun relinkOriginal(row: EntryBackup, scratch: File): String {
        val existing = row.entity.mediaUri
        if (photos.exists(existing)) return existing
        val byName = row.displayName?.let { photos.findByDisplayName(it) }
        if (byName != null) return byName.toString()
        val zipped = row.photoZip?.let { safeChild(scratch, it) }
        if (zipped != null && zipped.isFile) {
            val displayName = row.displayName
                ?: MediaNaming.displayName(row.entity.date, "untitled")
            return photos.insertOriginal(zipped, displayName, row.entity.capturedAt).toString()
        }
        return existing
    }

    private suspend fun restorePrefs(prefs: PrefsBackup) {
        preferences.setNotifyTime(LocalTime.of(prefs.notifyHour, prefs.notifyMinute))
        preferences.setPreciseTiming(prefs.preciseTiming)
        preferences.setThemeFocus(prefs.themeFocus)
        preferences.setSeriesEnabled(prefs.seriesEnabled)
        preferences.setPaused(prefs.paused)
        preferences.setOnboardingComplete(prefs.onboardingComplete)
        preferences.setDebugUseFakeAi(prefs.debugUseFakeAi)
        preferences.setLastNotifiedDate(prefs.lastNotifiedDate?.let(LocalDate::parse))
        preferences.setCoarseCityId(prefs.coarseCityId)
    }

    private fun copyZipFile(zip: ZipOutputStream, name: String?, source: File) {
        if (name == null || !source.isFile) return
        zip.putNextEntry(ZipEntry(name))
        source.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun unzip(input: InputStream, dest: File) {
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.replace('\\', '/')
                if (name.contains("..") || name.startsWith("/")) {
                    throw ProgressBackupException("That file isn't a ShutterUp backup.")
                }
                val out = File(dest, name)
                val canonical = out.canonicalFile
                if (!canonical.path.startsWith(dest.canonicalFile.path)) {
                    throw ProgressBackupException("That file isn't a ShutterUp backup.")
                }
                if (entry.isDirectory) {
                    canonical.mkdirs()
                } else {
                    canonical.parentFile?.mkdirs()
                    canonical.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
            }
        }
    }

    private fun safeChild(root: File, relative: String): File? {
        val name = relative.replace('\\', '/')
        if (name.contains("..") || name.startsWith("/")) return null
        val child = File(root, name).canonicalFile
        return child.takeIf { it.path.startsWith(root.canonicalFile.path) }
    }
}
