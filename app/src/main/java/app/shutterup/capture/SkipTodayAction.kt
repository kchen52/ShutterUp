package app.shutterup.capture

import app.shutterup.data.notifications.NotificationHelper
import app.shutterup.domain.capture.SkipDayUseCase
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Marks today skipped and cancels the day's notification (SPEC §8.1). */
class SkipTodayAction @Inject constructor(
    private val skipDay: SkipDayUseCase,
    private val notifications: NotificationHelper,
    private val clock: Clock,
    private val zone: ZoneId,
) {
    suspend operator fun invoke(): Boolean {
        val today = LocalDate.now(clock.withZone(zone))
        val skipped = skipDay()
        if (skipped) notifications.cancel(today)
        return skipped
    }
}
