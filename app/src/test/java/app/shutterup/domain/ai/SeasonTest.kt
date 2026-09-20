package app.shutterup.domain.ai

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SeasonTest {

    @Test
    fun unsetLatitudeKeepsTheNorthernDefault() {
        assertEquals(Season.WINTER, seasonForDate(LocalDate.of(2026, 1, 15)))
        assertEquals(Season.SPRING, seasonForDate(LocalDate.of(2026, 4, 1), latitude = null))
        assertEquals(Season.SUMMER, seasonForDate(LocalDate.of(2026, 6, 21)))
        assertEquals(Season.AUTUMN, seasonForDate(LocalDate.of(2026, 9, 22)))
        assertEquals(Season.WINTER, seasonForDate(LocalDate.of(2026, 12, 21)))
    }

    @Test
    fun northernLatitudeMatchesTheDefault() {
        assertEquals(Season.SUMMER, seasonForDate(LocalDate.of(2026, 6, 21), latitude = 40.7128))
        assertEquals(Season.WINTER, seasonForDate(LocalDate.of(2026, 12, 21), latitude = 51.5074))
    }

    @Test
    fun equatorIsTreatedAsNorthernSoUnsetBehaviourHolds() {
        assertEquals(Season.SUMMER, seasonForDate(LocalDate.of(2026, 6, 21), latitude = 0.0))
    }

    @Test
    fun southernLatitudeFlipsTheSeason() {
        assertEquals(Season.WINTER, seasonForDate(LocalDate.of(2026, 6, 21), latitude = -33.8688))
        assertEquals(Season.SUMMER, seasonForDate(LocalDate.of(2026, 12, 21), latitude = -33.9249))
        assertEquals(Season.AUTUMN, seasonForDate(LocalDate.of(2026, 4, 1), latitude = -0.1807))
        assertEquals(Season.SPRING, seasonForDate(LocalDate.of(2026, 9, 22), latitude = -36.8485))
    }
}
