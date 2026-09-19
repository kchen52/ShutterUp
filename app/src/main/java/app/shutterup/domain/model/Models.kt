package app.shutterup.domain.model

import java.time.Instant
import java.time.LocalDate

/** Day lifecycle states (SPEC §3, §4.3, §8.3). */
enum class DayStatus {
    PENDING,
    COMPLETED,
    COMPLETED_NO_PHOTO,
    SKIPPED,
    MISSED,
    PAUSED,
}

/**
 * One day's prompt and its status (SPEC §10). Persisted by Room in the
 * persistence milestone; domain logic only reads this shape.
 */
data class DayPrompt(
    val date: LocalDate,
    val title: String,
    val oneLiner: String,
    val details: String,
    val constraint: String?,
    val theme: String,
    val tips: List<String>,
    val source: PromptSourceRef,
    val libraryId: String?,
    val modelName: String?,
    val generatedAt: Instant,
    val status: DayStatus,
    /** A freeze protected this day; it does not break the streak. */
    val frozen: Boolean,
    val rerollUsed: Boolean,
)

enum class PromptSourceRef {
    ON_DEVICE_AI,
    LIBRARY,
}

/** Rerolled-away prompt, kept for dedup only (SPEC §10). */
data class SupersededPrompt(
    val id: Long = 0,
    val date: LocalDate,
    val title: String,
    val theme: String,
    val generatedAt: Instant,
)

/** Still or motion capture stored with a day's prompt. */
enum class MediaKind {
    PHOTO,
    VIDEO,
}

/** One captured still or clip answering a day's prompt (SPEC §10; up to three per day). */
data class Entry(
    val id: Long = 0,
    val date: LocalDate,
    /** MediaStore content URI of the original bytes (SPEC §9). */
    val mediaUri: String,
    val thumbPath: String,
    val capturedAt: Instant,
    val width: Int,
    val height: Int,
    val note: String?,
    val importedFromGallery: Boolean,
    val createdAt: Instant,
    val mediaKind: MediaKind = MediaKind.PHOTO,
)

data class Achievement(
    val id: String,
    val unlockedAt: Instant,
    val unlockedOnDate: LocalDate,
)

data class StreakState(
    val current: Int,
    val longest: Int,
    val freezes: Int,
    val lastProcessedDate: LocalDate?,
)

data class LibraryUsage(
    val libraryId: String,
    val usedOnDate: LocalDate,
)
