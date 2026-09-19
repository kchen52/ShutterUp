package app.shutterup.domain.capture

import app.shutterup.domain.gamification.AchievementEvaluator
import app.shutterup.domain.gamification.AchievementInput
import app.shutterup.domain.gamification.DayRecord
import app.shutterup.domain.gamification.StreakCalculator
import app.shutterup.domain.model.Achievement
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Outcome of persisting one capture for a day. */
sealed class CompleteCaptureResult {
    data class Saved(
        val newlyUnlocked: List<Achievement>,
        val streak: StreakState,
        val freezeEarned: Boolean,
        val previousStreak: Int,
    ) : CompleteCaptureResult()

    data object CapReached : CompleteCaptureResult()
    data object NotToday : CompleteCaptureResult()
    data object MissingPrompt : CompleteCaptureResult()
}

/**
 * Inserts an [Entry], marks the day [DayStatus.COMPLETED], then recomputes
 * streaks and achievements via [StreakCalculator] and [AchievementEvaluator].
 */
class CompleteCaptureUseCase @Inject constructor(
    private val prompts: DayPromptRepository,
    private val entries: EntryRepository,
    private val gamification: GamificationRepository,
    private val preferences: PreferencesRepository,
    private val clock: Clock,
    private val zone: ZoneId,
) {
    suspend operator fun invoke(entry: Entry): CompleteCaptureResult {
        val today = LocalDate.now(clock.withZone(zone))
        if (!CaptureDateValidator.isCapturedToday(entry.capturedAt, today, zone)) {
            return CompleteCaptureResult.NotToday
        }
        if (entry.date != today) {
            return CompleteCaptureResult.NotToday
        }
        val existing = entries.countForDate(today)
        if (existing > CaptureLimits.MAX_ENTRIES_PER_DAY) {
            return CompleteCaptureResult.CapReached
        }
        val prompt = prompts.getDay(today) ?: return CompleteCaptureResult.MissingPrompt
        if (existing == CaptureLimits.MAX_ENTRIES_PER_DAY) {
            // Retake: one photo answers the prompt (SPEC §1.2 / §4.3).
            entries.delete(today)
        }
        entries.upsert(entry)
        if (prompt.status == DayStatus.PENDING) {
            prompts.upsert(prompt.copy(status = DayStatus.COMPLETED))
        }
        val history = prompts.allDays()
        val records = history.map { DayRecord(it.date, it.status, it.frozen) }
        val previous = gamification.observeStreak().first()
        val newCurrent = StreakCalculator.currentStreak(records, today)
        val newLongest = maxOf(previous.longest, StreakCalculator.longestStreak(records))
        var freezes = previous.freezes
        val freezeEarned = newCurrent / 7 > previous.current / 7 && freezes < 2
        if (freezeEarned) freezes++
        val streak = StreakState(
            current = newCurrent,
            longest = newLongest,
            freezes = freezes,
            lastProcessedDate = listOfNotNull(previous.lastProcessedDate, today).maxOrNull(),
        )
        gamification.updateStreak(streak)
        val unlocked = gamification.observeAchievements().first().map { it.id }.toSet()
        val notifyTime = preferences.observeNotifyTime().first()
        val fresh = AchievementEvaluator.evaluate(
            AchievementInput(
                days = prompts.allDays(),
                entries = entries.listAll(),
                currentStreak = newCurrent,
                notifyTime = notifyTime,
                zone = zone,
                today = today,
            ),
            unlocked,
        )
        val now = clock.instant()
        val achievements = fresh.sorted().map { id ->
            Achievement(id = id, unlockedAt = now, unlockedOnDate = today)
        }
        for (achievement in achievements) {
            gamification.unlock(achievement)
        }
        return CompleteCaptureResult.Saved(
            newlyUnlocked = achievements,
            streak = streak,
            freezeEarned = freezeEarned,
            previousStreak = previous.current,
        )
    }
}
