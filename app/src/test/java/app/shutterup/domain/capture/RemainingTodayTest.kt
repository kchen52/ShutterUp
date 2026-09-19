package app.shutterup.domain.capture

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class RemainingTodayTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun hoursWhenAtLeastSixtyMinutesRemain() {
        val now = LocalDate.of(2026, 9, 19).atTime(LocalTime.of(15, 0)).atZone(zone)
        assertEquals("9 hours left today", RemainingToday.label(now))
    }

    @Test
    fun minutesWhenUnderAnHour() {
        val now = LocalDate.of(2026, 9, 19).atTime(LocalTime.of(23, 10)).atZone(zone)
        assertEquals("50 minutes left today", RemainingToday.label(now))
    }
}
