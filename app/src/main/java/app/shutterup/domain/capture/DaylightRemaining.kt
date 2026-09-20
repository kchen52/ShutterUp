package app.shutterup.domain.capture

import app.shutterup.domain.geo.SunTimes
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Prompt Detail remaining-light line. Falls back to [RemainingToday] when no
 * location is set, before sunrise, after sunset, or during polar night.
 */
object DaylightRemaining {
    const val POLAR_DAY = "Good light all day"

    fun label(now: ZonedDateTime, latitude: Double?, longitude: Double?): String {
        if (latitude == null || longitude == null) return RemainingToday.label(now)
        return when (val sun = SunTimes.of(now.toLocalDate(), latitude, longitude, now.zone)) {
            SunTimes.PolarNight -> RemainingToday.label(now)
            SunTimes.PolarDay -> POLAR_DAY
            is SunTimes.RiseSet -> labelWhileTheSunIsUp(now, sun)
        }
    }

    private fun labelWhileTheSunIsUp(now: ZonedDateTime, sun: SunTimes.RiseSet): String {
        val sunrise = now.toLocalDate().atTime(sun.sunrise).atZone(now.zone).let { stamp ->
            if (sun.sunriseIsPreviousDay) stamp.minusDays(1) else stamp
        }
        val sunset = now.toLocalDate().atTime(sun.sunset).atZone(now.zone).let { stamp ->
            if (sun.sunsetIsNextDay) stamp.plusDays(1) else stamp
        }
        if (now.isBefore(sunrise) || !now.isBefore(sunset)) {
            return RemainingToday.label(now)
        }
        val minutes = ChronoUnit.MINUTES.between(now, sunset).coerceAtLeast(0L)
        return if (minutes >= 60L) {
            val hours = minutes / 60L
            if (hours == 1L) "1 hour of good light left" else "$hours hours of good light left"
        } else {
            if (minutes == 1L) "1 minute of good light left" else "$minutes minutes of good light left"
        }
    }
}
