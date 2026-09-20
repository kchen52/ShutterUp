package app.shutterup.domain.geo

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NOAA sunrise/sunset against published civil times. Tolerance is two minutes:
 * minute-level is the contract, and published tables can differ by a minute
 * depending on rounding and the exact coordinates used for "the city".
 */
class SunTimesTest {

    @Test
    fun newYorkSummerSolstice() {
        // NYC 40.7128°N 74.0060°W, 21 Jun 2026. Published (NOAA / worldstats):
        // sunrise 05:25, sunset 20:31 EDT, ~15h 06m of daylight.
        assertWithinTwoMinutes(
            expectedRise = LocalTime.of(5, 25),
            expectedSet = LocalTime.of(20, 31),
            actual = sun(NEW_YORK, LocalDate.of(2026, 6, 21), "America/New_York"),
        )
    }

    @Test
    fun newYorkWinterSolstice() {
        // Same coordinates, 21 Dec 2026. Published: ~07:16 / 16:31 EST.
        assertWithinTwoMinutes(
            expectedRise = LocalTime.of(7, 16),
            expectedSet = LocalTime.of(16, 31),
            actual = sun(NEW_YORK, LocalDate.of(2026, 12, 21), "America/New_York"),
        )
    }

    @Test
    fun newYorkMarchEquinox() {
        // 20 Mar 2026, EDT. Published: ~06:59 / 19:08.
        assertWithinTwoMinutes(
            expectedRise = LocalTime.of(6, 59),
            expectedSet = LocalTime.of(19, 8),
            actual = sun(NEW_YORK, LocalDate.of(2026, 3, 20), "America/New_York"),
        )
    }

    @Test
    fun sydneyWinterSolsticeSouthernHemisphere() {
        // Sydney 33.8688°S 151.2093°E, 21 Jun 2026 (southern winter).
        // Published civil times: ~07:00 / 16:53 AEST.
        assertWithinTwoMinutes(
            expectedRise = LocalTime.of(7, 0),
            expectedSet = LocalTime.of(16, 53),
            actual = sun(SYDNEY, LocalDate.of(2026, 6, 21), "Australia/Sydney"),
        )
    }

    @Test
    fun sydneySummerSolstice() {
        // 21 Dec 2026, AEDT. Published: ~05:41 / 20:06.
        assertWithinTwoMinutes(
            expectedRise = LocalTime.of(5, 41),
            expectedSet = LocalTime.of(20, 6),
            actual = sun(SYDNEY, LocalDate.of(2026, 12, 21), "Australia/Sydney"),
        )
    }

    @Test
    fun capeTownSouthernSummerSolstice() {
        // Cape Town 33.9249°S 18.4241°E, 21 Dec 2026. Published: ~05:32 / 19:57 SAST.
        assertWithinTwoMinutes(
            expectedRise = LocalTime.of(5, 32),
            expectedSet = LocalTime.of(19, 57),
            actual = sun(CAPE_TOWN, LocalDate.of(2026, 12, 21), "Africa/Johannesburg"),
        )
    }

    @Test
    fun quitoOnTheEquatorAtTheEquinox() {
        // Quito 0.1807°S 78.4678°W, 20 Mar 2026. Near-equal day/night year-round.
        assertWithinTwoMinutes(
            expectedRise = LocalTime.of(6, 18),
            expectedSet = LocalTime.of(18, 24),
            actual = sun(QUITO, LocalDate.of(2026, 3, 20), "America/Guayaquil"),
        )
    }

    @Test
    fun quitoJuneSolsticeStaysNearTwelveHours() {
        // timeanddate: 06:12 / 18:19 on 21 Jun 2026.
        assertWithinTwoMinutes(
            expectedRise = LocalTime.of(6, 12),
            expectedSet = LocalTime.of(18, 19),
            actual = sun(QUITO, LocalDate.of(2026, 6, 21), "America/Guayaquil"),
        )
    }

    @Test
    fun tokyoNorthernSummerSolstice() {
        assertWithinTwoMinutes(
            expectedRise = LocalTime.of(4, 26),
            expectedSet = LocalTime.of(19, 0),
            actual = sun(TOKYO, LocalDate.of(2026, 6, 21), "Asia/Tokyo"),
        )
    }

    @Test
    fun buenosAiresSouthernSummerSolstice() {
        assertWithinTwoMinutes(
            expectedRise = LocalTime.of(5, 37),
            expectedSet = LocalTime.of(20, 6),
            actual = sun(BUENOS_AIRES, LocalDate.of(2026, 12, 21), "America/Argentina/Buenos_Aires"),
        )
    }

    @Test
    fun tromsoPolarDayOnJuneSolstice() {
        val actual = SunTimes.of(
            LocalDate.of(2026, 6, 21),
            TROMSO.first,
            TROMSO.second,
            ZoneId.of("Europe/Oslo"),
        )
        assertEquals(SunTimes.PolarDay, actual)
    }

    @Test
    fun tromsoPolarNightOnDecemberSolstice() {
        val actual = SunTimes.of(
            LocalDate.of(2026, 12, 21),
            TROMSO.first,
            TROMSO.second,
            ZoneId.of("Europe/Oslo"),
        )
        assertEquals(SunTimes.PolarNight, actual)
    }

    @Test
    fun tromsoEquinoxStillHasASunriseAndSunset() {
        assertWithinTwoMinutes(
            expectedRise = LocalTime.of(5, 44),
            expectedSet = LocalTime.of(18, 2),
            actual = sun(TROMSO, LocalDate.of(2026, 3, 20), "Europe/Oslo"),
        )
    }

    @Test
    fun usesTheInjectedZoneNotACityOffset() {
        // Same NYC coordinates, but the user is travelling on UTC. Sunrise
        // must follow the injected zone, not Eastern Time.
        val actual = sun(NEW_YORK, LocalDate.of(2026, 6, 21), "UTC")
        val eastern = sun(NEW_YORK, LocalDate.of(2026, 6, 21), "America/New_York")
        val minutesApart = kotlin.math.abs(
            actual.sunrise.toSecondOfDay() - eastern.sunrise.toSecondOfDay(),
        ) / 60
        assertTrue("UTC and EDT sunrise should differ by about 4 hours, was $minutesApart", minutesApart in 180..300)
    }

    private fun sun(
        coords: Pair<Double, Double>,
        date: LocalDate,
        zone: String,
    ): SunTimes.RiseSet {
        val result = SunTimes.of(date, coords.first, coords.second, ZoneId.of(zone))
        assertTrue("expected a rise/set on $date, got $result", result is SunTimes.RiseSet)
        return result as SunTimes.RiseSet
    }

    private fun assertWithinTwoMinutes(
        expectedRise: LocalTime,
        expectedSet: LocalTime,
        actual: SunTimes.RiseSet,
    ) {
        assertTrue(
            "sunrise ${actual.sunrise} not within 2 min of $expectedRise",
            minutesApart(expectedRise, actual.sunrise) <= 2,
        )
        assertTrue(
            "sunset ${actual.sunset} not within 2 min of $expectedSet",
            minutesApart(expectedSet, actual.sunset) <= 2,
        )
    }

    private fun minutesApart(a: LocalTime, b: LocalTime): Int =
        kotlin.math.abs(a.toSecondOfDay() - b.toSecondOfDay()) / 60

    private companion object {
        val NEW_YORK = 40.7128 to -74.0060
        val SYDNEY = -33.8688 to 151.2093
        val CAPE_TOWN = -33.9249 to 18.4241
        val QUITO = -0.1807 to -78.4678
        val TOKYO = 35.6762 to 139.6503
        val BUENOS_AIRES = -34.6037 to -58.3816
        val TROMSO = 69.6496 to 18.9553
    }
}
