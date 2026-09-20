package app.shutterup.domain.take

import app.shutterup.domain.model.DayStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecondTakeCalendarTest {
    private val today = LocalDate.of(2026, 9, 20)

    @Test
    fun todayPending_landsOnToday() {
        val target = SecondTakeCalendar.targetDate(today, DayStatus.PENDING)
        assertEquals(today, target)
        assertTrue(SecondTakeCalendar.landsToday(target, today))
        assertEquals("This becomes today's prompt.", SecondTakeCalendar.confirmationBody(target, today))
    }

    @Test
    fun todayUngenerated_landsOnToday() {
        assertEquals(today, SecondTakeCalendar.targetDate(today, null))
        assertTrue(SecondTakeCalendar.isOpen(null))
    }

    @Test
    fun todayCompleted_landsOnTomorrow() {
        val target = SecondTakeCalendar.targetDate(today, DayStatus.COMPLETED)
        assertEquals(today.plusDays(1), target)
        assertFalse(SecondTakeCalendar.landsToday(target, today))
        assertEquals("This becomes tomorrow's prompt.", SecondTakeCalendar.confirmationBody(target, today))
    }

    @Test
    fun todaySkipped_landsOnTomorrow() {
        assertEquals(
            today.plusDays(1),
            SecondTakeCalendar.targetDate(today, DayStatus.SKIPPED),
        )
    }

    @Test
    fun todayPaused_landsOnTomorrow() {
        assertEquals(
            today.plusDays(1),
            SecondTakeCalendar.targetDate(today, DayStatus.PAUSED),
        )
    }

    @Test
    fun todayCompletedNoPhoto_landsOnTomorrow() {
        assertEquals(
            today.plusDays(1),
            SecondTakeCalendar.targetDate(today, DayStatus.COMPLETED_NO_PHOTO),
        )
    }

    @Test
    fun actedOnDaysAreNotOpen() {
        assertFalse(SecondTakeCalendar.isOpen(DayStatus.COMPLETED))
        assertFalse(SecondTakeCalendar.isOpen(DayStatus.SKIPPED))
        assertFalse(SecondTakeCalendar.isOpen(DayStatus.PAUSED))
        assertFalse(SecondTakeCalendar.isOpen(DayStatus.MISSED))
        assertTrue(SecondTakeCalendar.isOpen(DayStatus.PENDING))
    }
}
