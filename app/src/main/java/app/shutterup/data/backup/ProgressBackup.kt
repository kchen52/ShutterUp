package app.shutterup.data.backup

import app.shutterup.data.local.AchievementEntity
import app.shutterup.data.local.DayPromptEntity
import app.shutterup.data.local.EntryEntity
import app.shutterup.data.local.LibraryUsageEntity
import app.shutterup.data.local.MonthlyIssueEntity
import app.shutterup.data.local.SeriesEntity
import app.shutterup.data.local.StreakStateEntity
import app.shutterup.data.local.SupersededPromptEntity
import java.time.Instant
import java.time.LocalDate

/** On-device ZIP of progress + photo copies so an uninstall does not wipe history. */
object ProgressBackupFormat {
    const val VERSION = 1
    const val JSON_NAME = "progress.json"
    const val THUMBS_DIR = "thumbs/"
    const val PHOTOS_DIR = "photos/"
    const val MIME = "application/zip"

    fun fileName(date: LocalDate): String = "ShutterUp-backup-$date.zip"
}

class ProgressBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class PrefsBackup(
    val notifyHour: Int,
    val notifyMinute: Int,
    val preciseTiming: Boolean,
    val themeFocus: String?,
    val seriesEnabled: Boolean,
    val paused: Boolean,
    val onboardingComplete: Boolean,
    val debugUseFakeAi: Boolean,
    val lastNotifiedDate: String?,
    val coarseCityId: String?,
)

data class EntryBackup(
    val entity: EntryEntity,
    val displayName: String?,
    val thumbZip: String?,
    val photoZip: String?,
)

data class ProgressBackup(
    val format: Int,
    val createdAt: Instant,
    val prefs: PrefsBackup,
    val prompts: List<DayPromptEntity>,
    val superseded: List<SupersededPromptEntity>,
    val entries: List<EntryBackup>,
    val achievements: List<AchievementEntity>,
    val streak: StreakStateEntity?,
    val libraryUsage: List<LibraryUsageEntity>,
    val series: List<SeriesEntity>,
    val monthlyIssues: List<MonthlyIssueEntity>,
)
