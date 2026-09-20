package app.shutterup.domain.capture

import app.shutterup.domain.geo.SunTimes
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DaylightRemainingTest {
    private val zone = ZoneId.of("America/New_York")
    private val date = LocalDate.of(2026, 6, 21)
    private val lat = 40.7128
    private val lon = -74.0060
    private val sun = SunTimes.of(date, lat, lon, zone) as SunTimes.RiseSet

    @Test
    fun unsetLocationUsesRemainingToday() {
        val now = date.atTime(LocalTime.of(15, 0)).atZone(zone)
        assertEquals(RemainingToday.label(now), DaylightRemaining.label(now, null, null))
        assertEquals("9 hours left today", DaylightRemaining.label(now, null, null))
    }

    @Test
    fun moreThanAnHourBeforeSunsetUsesHours() {
        val now = date.atTime(sun.sunset.minusHours(2)).atZone(zone)
        assertEquals("2 hours of good light left", DaylightRemaining.label(now, lat, lon))
    }

    @Test
    fun exactlySixtyMinutesUsesTheHourForm() {
        val now = date.atTime(sun.sunset.minusMinutes(60)).atZone(zone)
        assertEquals("1 hour of good light left", DaylightRemaining.label(now, lat, lon))
    }

    @Test
    fun underAnHourUsesMinutes() {
        val now = date.atTime(sun.sunset.minusMinutes(40)).atZone(zone)
        assertEquals("40 minutes of good light left", DaylightRemaining.label(now, lat, lon))
    }

    @Test
    fun oneMinuteLeftIsSingular() {
        val now = date.atTime(sun.sunset.minusMinutes(1)).atZone(zone)
        assertEquals("1 minute of good light left", DaylightRemaining.label(now, lat, lon))
    }

    @Test
    fun afterSunsetFallsBackToRemainingToday() {
        val now = date.atTime(sun.sunset.plusMinutes(30)).atZone(zone)
        assertEquals(RemainingToday.label(now), DaylightRemaining.label(now, lat, lon))
        assertTrue(DaylightRemaining.label(now, lat, lon).endsWith("left today"))
    }

    @Test
    fun beforeSunriseFallsBackToRemainingToday() {
        val now = date.atTime(sun.sunrise.minusHours(1)).atZone(zone)
        assertEquals(RemainingToday.label(now), DaylightRemaining.label(now, lat, lon))
        assertTrue(DaylightRemaining.label(now, lat, lon).endsWith("left today"))
    }

    @Test
    fun polarDayIsPlainAndUnhurried() {
        val now = LocalDate.of(2026, 6, 21).atTime(LocalTime.of(15, 0))
            .atZone(ZoneId.of("Europe/Oslo"))
        assertEquals(
            "Good light all day",
            DaylightRemaining.label(now, latitude = 69.6496, longitude = 18.9553),
        )
    }

    @Test
    fun polarNightFallsBackToRemainingToday() {
        val now = LocalDate.of(2026, 12, 21).atTime(LocalTime.of(15, 0))
            .atZone(ZoneId.of("Europe/Oslo"))
        assertEquals(
            RemainingToday.label(now),
            DaylightRemaining.label(now, latitude = 69.6496, longitude = 18.9553),
        )
        assertEquals("9 hours left today", DaylightRemaining.label(now, 69.6496, 18.9553))
    }
}
