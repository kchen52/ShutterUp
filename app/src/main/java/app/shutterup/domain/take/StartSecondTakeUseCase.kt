package app.shutterup.domain.take

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.SupersededPrompt
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

sealed class StartSecondTakeResult {
    data class Placed(val date: LocalDate, val landsToday: Boolean) : StartSecondTakeResult()
    data object NotRepeatable : StartSecondTakeResult()
    data object TargetTaken : StartSecondTakeResult()
}

data class SecondTakePreview(
    val target: LocalDate,
    val landsToday: Boolean,
    val confirmationBody: String,
)

/**
 * Copies a past day's prompt onto today (if still pending) or tomorrow,
 * recorded as a repeat of the original date. Detaches the target from an
 * active Series without otherwise changing that week.
 */
class StartSecondTakeUseCase @Inject constructor(
    private val prompts: DayPromptRepository,
    private val entries: EntryRepository,
    private val clock: Clock,
    private val zone: ZoneId,
) {
    suspend fun preview(sourceDate: LocalDate): SecondTakePreview? {
        if (!isRepeatable(sourceDate)) return null
        val today = today()
        val target = SecondTakeCalendar.targetDate(today, prompts.getDay(today)?.status)
        if (!SecondTakeCalendar.isOpen(prompts.getDay(target)?.status)) return null
        return SecondTakePreview(
            target = target,
            landsToday = SecondTakeCalendar.landsToday(target, today),
            confirmationBody = SecondTakeCalendar.confirmationBody(target, today),
        )
    }

    suspend operator fun invoke(sourceDate: LocalDate): StartSecondTakeResult {
        val source = prompts.getDay(sourceDate) ?: return StartSecondTakeResult.NotRepeatable
        if (!isRepeatable(sourceDate)) return StartSecondTakeResult.NotRepeatable
        val today = today()
        val target = SecondTakeCalendar.targetDate(today, prompts.getDay(today)?.status)
        val existing = prompts.getDay(target)
        if (!SecondTakeCalendar.isOpen(existing?.status)) {
            return StartSecondTakeResult.TargetTaken
        }
        if (existing != null) {
            prompts.recordSuperseded(
                SupersededPrompt(
                    date = target,
                    title = existing.title,
                    theme = existing.theme,
                    generatedAt = existing.generatedAt,
                ),
            )
        }
        val original = SecondTakeEligibility.originalDate(source.repeatsDate, source.date)
        prompts.upsert(
            source.copy(
                date = target,
                generatedAt = clock.instant(),
                status = DayStatus.PENDING,
                frozen = false,
                rerollUsed = false,
                seriesId = null,
                seriesIndex = null,
                repeatsDate = original,
            ),
        )
        return StartSecondTakeResult.Placed(
            date = target,
            landsToday = SecondTakeCalendar.landsToday(target, today),
        )
    }

    private suspend fun isRepeatable(sourceDate: LocalDate): Boolean {
        val source = prompts.getDay(sourceDate) ?: return false
        val hasPhotograph = entries.countForDate(sourceDate) > 0
        return SecondTakeEligibility.isRepeatable(
            date = sourceDate,
            today = today(),
            status = source.status,
            hasPhotograph = hasPhotograph,
        )
    }

    private fun today(): LocalDate = LocalDate.now(clock.withZone(zone))
}
