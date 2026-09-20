package app.shutterup.domain.take

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TakeIntervalTest {
    private val first = LocalDate.of(2026, 6, 28)

    @Test
    fun oneDay() {
        assertEquals("a day on", TakeInterval.phrase(first, first.plusDays(1)))
    }

    @Test
    fun twelveDays() {
        assertEquals("12 days on", TakeInterval.phrase(first, first.plusDays(12)))
    }

    @Test
    fun aMonth() {
        val from = LocalDate.of(2026, 8, 20)
        val to = LocalDate.of(2026, 9, 20)
        assertEquals("a month on", TakeInterval.phrase(from, to))
    }

    @Test
    fun eightyThreeDays_staysAsDays() {
        val from = LocalDate.of(2026, 6, 28)
        val to = LocalDate.of(2026, 9, 19)
        assertEquals(83, java.time.temporal.ChronoUnit.DAYS.between(from, to))
        assertEquals("83 days on", TakeInterval.phrase(from, to))
    }

    @Test
    fun aYear() {
        assertEquals("a year on", TakeInterval.phrase(first, first.plusYears(1)))
    }

    @Test
    fun twoYears() {
        assertEquals("2 years on", TakeInterval.phrase(first, first.plusYears(2)))
    }

    @Test
    fun dateKickerUppercase() {
        assertEquals("19 SEPTEMBER", TakeInterval.dateKicker(LocalDate.of(2026, 9, 19)))
    }

    @Test
    fun laterTakesLine_oneAndMany() {
        val a = LocalDate.of(2026, 12, 11)
        val b = LocalDate.of(2027, 2, 2)
        val c = LocalDate.of(2027, 3, 1)
        assertEquals("Shot again on 11 December.", TakeInterval.laterTakesLine(listOf(a)))
        assertEquals("Shot again on 11 December and 2 February.", TakeInterval.laterTakesLine(listOf(a, b)))
        assertEquals("Shot again on 11 December and 2 more.", TakeInterval.laterTakesLine(listOf(a, b, c)))
        assertEquals(null, TakeInterval.laterTakesLine(emptyList()))
    }
}
