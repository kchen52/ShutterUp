package app.shutterup.domain.capture

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureDateValidatorTest {

    private val zone = ZoneId.of("America/New_York")
    private val today = LocalDate.of(2026, 9, 19)

    @Test
    fun sameDayInstant_isAccepted() {
        val capturedAt = today.atTime(LocalTime.NOON).atZone(zone).toInstant()
        assertTrue(CaptureDateValidator.isTakenToday(capturedAt, today, zone))
    }

    @Test
    fun yesterdayInstant_isRejected() {
        val capturedAt = today.minusDays(1).atTime(LocalTime.NOON).atZone(zone).toInstant()
        assertFalse(CaptureDateValidator.isTakenToday(capturedAt, today, zone))
    }

    @Test
    fun localMidnightBoundary_2350Accepted_0010NextDayRejected() {
        val late = today.atTime(23, 50).atZone(zone).toInstant()
        val earlyNext = today.plusDays(1).atTime(0, 10).atZone(zone).toInstant()
        assertTrue(CaptureDateValidator.isTakenToday(late, today, zone))
        assertFalse(CaptureDateValidator.isTakenToday(earlyNext, today, zone))
        assertTrue(CaptureDateValidator.isTakenToday(earlyNext, today.plusDays(1), zone))
    }

    @Test
    fun dstSpringForwardDay_stillMapsInstantsToLocalDate() {
        val dstDay = LocalDate.of(2026, 3, 8)
        val beforeGap = dstDay.atTime(1, 30).atZone(zone).toInstant()
        val afterGap = dstDay.atTime(3, 30).atZone(zone).toInstant()
        val late = dstDay.atTime(23, 50).atZone(zone).toInstant()
        val nextMorning = dstDay.plusDays(1).atTime(0, 10).atZone(zone).toInstant()
        assertTrue(CaptureDateValidator.isTakenToday(beforeGap, dstDay, zone))
        assertTrue(CaptureDateValidator.isTakenToday(afterGap, dstDay, zone))
        assertTrue(CaptureDateValidator.isTakenToday(late, dstDay, zone))
        assertFalse(CaptureDateValidator.isTakenToday(nextMorning, dstDay, zone))
        assertTrue(CaptureDateValidator.isTakenToday(nextMorning, dstDay.plusDays(1), zone))
    }
}
