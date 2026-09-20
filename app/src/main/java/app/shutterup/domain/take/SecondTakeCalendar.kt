package app.shutterup.domain.take

import app.shutterup.domain.model.DayStatus
import java.time.LocalDate

/**
 * Where a second take lands. Same predictability Series uses for a new week:
 * today if today is still [DayStatus.PENDING] (or ungenerated), otherwise
 * tomorrow. Never overwrites a day the user has already acted on.
 */
object SecondTakeCalendar {
    fun targetDate(today: LocalDate, todayStatus: DayStatus?): LocalDate =
        if (isOpen(todayStatus)) today else today.plusDays(1)

    fun isOpen(status: DayStatus?): Boolean =
        status == null || status == DayStatus.PENDING

    fun landsToday(target: LocalDate, today: LocalDate): Boolean = target == today

    fun confirmationBody(target: LocalDate, today: LocalDate): String =
        if (landsToday(target, today)) {
            "This becomes today's prompt."
        } else {
            "This becomes tomorrow's prompt."
        }
}
