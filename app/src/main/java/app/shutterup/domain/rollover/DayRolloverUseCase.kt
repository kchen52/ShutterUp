package app.shutterup.domain.rollover

import app.shutterup.domain.gamification.DayRecord
import app.shutterup.domain.gamification.StreakCalculator
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.GamificationRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

data class RolloverResult(
    val processed: List<LocalDate>,
    val missed: List<LocalDate>,
    val freezesConsumed: Int,
    val freezesEarned: Int,
)

class DayRolloverUseCase @Inject constructor(
    private val prompts: DayPromptRepository,
    private val gamification: GamificationRepository,
    private val clock: Clock,
) {
    /**
     * Idempotent. Processes every date from (lastProcessedDate+1, or today on first run) through [today].
     *
     * Freeze consumption happens only on PENDING→MISSED transitions (and MISSED placeholders).
     * The explicit skip path must consume a freeze synchronously when marking a day SKIPPED;
     * this use case never rewrites SKIPPED rows so earned freezes cannot retro-freeze the past.
     */
    suspend fun rollover(today: LocalDate, paused: Boolean): RolloverResult {
        val state = gamification.observeStreak().first()
        val from = state.lastProcessedDate?.plusDays(1) ?: today
        if (from.isAfter(today)) {
            return RolloverResult(
                processed = emptyList(),
                missed = emptyList(),
                freezesConsumed = 0,
                freezesEarned = 0,
            )
        }

        val processed = mutableListOf<LocalDate>()
        val missed = mutableListOf<LocalDate>()
        var freezes = state.freezes
        var consumed = 0

        var date = from
        while (!date.isAfter(today)) {
            processed += date
            val existing = prompts.getDay(date)
            val isTodayLive = date == today && !paused
            if (!isTodayLive) {
                if (paused) {
                    applyPaused(date, existing)
                } else if (date.isBefore(today)) {
                    val markedMissed = applyMissed(date, existing, freezes)
                    if (markedMissed != null) {
                        missed += date
                        if (markedMissed) {
                            freezes--
                            consumed++
                        }
                    }
                }
            }
            date = date.plusDays(1)
        }

        val history = prompts.recentDays(730).map { DayRecord(it.date, it.status, it.frozen) }
        val newCurrent = StreakCalculator.currentStreak(history, today)
        val newLongest = maxOf(state.longest, StreakCalculator.longestStreak(history))
        val earned = if (newCurrent / 7 > state.current / 7 && freezes < 2) {
            freezes++
            1
        } else {
            0
        }
        gamification.updateStreak(
            StreakState(
                current = newCurrent,
                longest = newLongest,
                freezes = freezes,
                lastProcessedDate = today,
            ),
        )
        return RolloverResult(
            processed = processed,
            missed = missed,
            freezesConsumed = consumed,
            freezesEarned = earned,
        )
    }

    private suspend fun applyPaused(date: LocalDate, existing: DayPrompt?) {
        when {
            existing == null -> prompts.upsert(pausedPlaceholder(date))
            existing.status == DayStatus.PENDING -> prompts.upsert(existing.copy(status = DayStatus.PAUSED))
        }
    }

    private suspend fun applyMissed(date: LocalDate, existing: DayPrompt?, freezes: Int): Boolean? {
        val consume = freezes > 0
        return when {
            existing == null -> {
                prompts.upsert(missedPlaceholder(date, frozen = consume))
                consume
            }
            existing.status == DayStatus.PENDING -> {
                prompts.upsert(existing.copy(status = DayStatus.MISSED, frozen = consume))
                consume
            }
            else -> null
        }
    }

    private fun pausedPlaceholder(date: LocalDate): DayPrompt = DayPrompt(
        date = date,
        title = "Paused",
        oneLiner = "No prompt today.",
        details = "Prompts and notifications are paused. Paused days do not affect the streak.",
        constraint = null,
        theme = "Paused",
        tips = emptyList(),
        source = PromptSourceRef.LIBRARY,
        libraryId = null,
        modelName = null,
        generatedAt = clock.instant(),
        status = DayStatus.PAUSED,
        frozen = false,
        rerollUsed = false,
    )

    private fun missedPlaceholder(date: LocalDate, frozen: Boolean): DayPrompt = DayPrompt(
        date = date,
        title = "Missed day",
        oneLiner = "No photo was taken.",
        details = "This day passed without a completed prompt.",
        constraint = null,
        theme = "Missed",
        tips = emptyList(),
        source = PromptSourceRef.LIBRARY,
        libraryId = null,
        modelName = null,
        generatedAt = clock.instant(),
        status = DayStatus.MISSED,
        frozen = frozen,
        rerollUsed = false,
    )
}
