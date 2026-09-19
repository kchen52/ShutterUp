package app.shutterup.domain.capture

import app.shutterup.domain.gamification.DayRecord
import app.shutterup.domain.gamification.StreakCalculator
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.GamificationRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Marks today [DayStatus.SKIPPED], consuming a freeze when one is held. */
class SkipDayUseCase @Inject constructor(
    private val prompts: DayPromptRepository,
    private val gamification: GamificationRepository,
    private val clock: Clock,
    private val zone: ZoneId,
) {
    suspend operator fun invoke(): Boolean {
        val today = LocalDate.now(clock.withZone(zone))
        val prompt = prompts.getDay(today) ?: return false
        if (prompt.status != DayStatus.PENDING) return false
        val state = gamification.observeStreak().first()
        val consume = state.freezes > 0
        prompts.upsert(prompt.copy(status = DayStatus.SKIPPED, frozen = consume))
        val history = prompts.allDays().map { DayRecord(it.date, it.status, it.frozen) }
        val current = StreakCalculator.currentStreak(history, today)
        val longest = maxOf(state.longest, StreakCalculator.longestStreak(history))
        gamification.updateStreak(
            StreakState(
                current = current,
                longest = longest,
                freezes = if (consume) state.freezes - 1 else state.freezes,
                lastProcessedDate = listOfNotNull(state.lastProcessedDate, today).maxOrNull(),
            ),
        )
        return true
    }
}
