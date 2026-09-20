package app.shutterup.domain.take

import app.shutterup.domain.model.DayStatus
import java.time.LocalDate

/** A past day with a photograph can be shot again. */
object SecondTakeEligibility {
    fun isRepeatable(
        date: LocalDate,
        today: LocalDate,
        status: DayStatus?,
        hasPhotograph: Boolean,
    ): Boolean =
        date.isBefore(today) &&
            hasPhotograph &&
            status == DayStatus.COMPLETED

    fun originalDate(repeatsDate: LocalDate?, date: LocalDate): LocalDate =
        repeatsDate ?: date
}
