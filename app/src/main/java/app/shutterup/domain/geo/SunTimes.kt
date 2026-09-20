package app.shutterup.domain.geo

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.tan

/**
 * Sunrise and sunset for a civil date, from the NOAA solar calculator
 * (https://gml.noaa.gov/grad/solcalc/). Minute-level is the intended precision.
 *
 * Timezone comes from [zone], never from the coordinates: the device clock stays
 * authoritative if the user has travelled since picking a city.
 *
 * Polar day and polar night are returned as explicit cases when the sun does
 * not cross the refraction-corrected horizon (zenith 90.833°) that date.
 */
sealed class SunTimes {
    /**
     * @param sunriseMinute minutes from local midnight; may be negative when
     * the sun was already up at 00:00.
     * @param sunsetMinute minutes from local midnight; may be ≥ 1440 when
     * sunset falls after the next midnight.
     */
    data class RiseSet(
        val sunriseMinute: Int,
        val sunsetMinute: Int,
    ) : SunTimes() {
        val sunrise: LocalTime get() = minutesToLocalTime(sunriseMinute)
        val sunset: LocalTime get() = minutesToLocalTime(sunsetMinute)
        val sunriseIsPreviousDay: Boolean get() = sunriseMinute < 0
        val sunsetIsNextDay: Boolean get() = sunsetMinute >= MINUTES_PER_DAY
    }

    data object PolarDay : SunTimes()

    data object PolarNight : SunTimes()

    companion object {
        /**
         * Apparent sunrise/sunset, including the standard NOAA correction for
         * refraction and the solar disk (zenith 90.833°).
         */
        fun of(date: LocalDate, latitude: Double, longitude: Double, zone: ZoneId): SunTimes {
            val tzHours = date.atTime(LocalTime.NOON).atZone(zone).offset.totalSeconds / 3600.0
            val jd = julianDay(date.year, date.monthValue, date.dayOfMonth)
            val riseUtc = sunriseSetUtc(rise = true, jd = jd, latitude = latitude, longitude = longitude)
            val setUtc = sunriseSetUtc(rise = false, jd = jd, latitude = latitude, longitude = longitude)
            if (riseUtc == null || setUtc == null) {
                return polarCase(jd, latitude)
            }
            val riseRefined = sunriseSetUtc(true, jd + riseUtc / 1440.0, latitude, longitude)
            val setRefined = sunriseSetUtc(false, jd + setUtc / 1440.0, latitude, longitude)
            if (riseRefined == null || setRefined == null) {
                return polarCase(jd, latitude)
            }
            return RiseSet(
                sunriseMinute = round(riseRefined + tzHours * 60.0).toInt(),
                sunsetMinute = round(setRefined + tzHours * 60.0).toInt(),
            )
        }
    }
}

private const val MINUTES_PER_DAY = 1440
private const val SUNRISE_ZENITH_DEG = 90.833

private fun polarCase(jd: Double, latitude: Double): SunTimes {
    val decl = sunDeclination(julianCentury(jd))
    val arg = hourAngleCosine(latitude, decl)
    return if (arg < -1.0) SunTimes.PolarDay else SunTimes.PolarNight
}

/** UTC minutes from 00:00 UTC, or null when the hour angle is undefined. */
private fun sunriseSetUtc(rise: Boolean, jd: Double, latitude: Double, longitude: Double): Double? {
    val t = julianCentury(jd)
    val eqTime = equationOfTime(t)
    val decl = sunDeclination(t)
    val ha = hourAngleSunrise(latitude, decl) ?: return null
    val signed = if (rise) ha else -ha
    return 720.0 - (4.0 * (longitude + signed)) - eqTime
}

private fun hourAngleSunrise(latitude: Double, declination: Double): Double? {
    val arg = hourAngleCosine(latitude, declination)
    if (arg < -1.0 || arg > 1.0) return null
    return Math.toDegrees(acos(arg))
}

private fun hourAngleCosine(latitude: Double, declination: Double): Double {
    val lat = Math.toRadians(latitude)
    val decl = Math.toRadians(declination)
    val zenith = Math.toRadians(SUNRISE_ZENITH_DEG)
    return cos(zenith) / (cos(lat) * cos(decl)) - tan(lat) * tan(decl)
}

private fun julianDay(year: Int, month: Int, day: Int): Double {
    var y = year
    var m = month
    if (m <= 2) {
        y -= 1
        m += 12
    }
    val a = y / 100
    val b = 2 - a + a / 4
    return (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + day + b - 1524.5
}

private fun julianCentury(jd: Double): Double = (jd - 2451545.0) / 36525.0

private fun geomMeanLongSun(t: Double): Double =
    (280.46646 + t * (36000.76983 + t * 0.0003032)) % 360.0

private fun geomMeanAnomalySun(t: Double): Double =
    357.52911 + t * (35999.05029 - 0.0001537 * t)

private fun eccentricityEarthOrbit(t: Double): Double =
    0.016708634 - t * (0.000042037 + 0.0000001267 * t)

private fun sunEqOfCenter(t: Double): Double {
    val m = geomMeanAnomalySun(t)
    return sinDeg(m) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
        sinDeg(2 * m) * (0.019993 - 0.000101 * t) +
        sinDeg(3 * m) * 0.000289
}

private fun sunApparentLong(t: Double): Double {
    val trueLong = geomMeanLongSun(t) + sunEqOfCenter(t)
    return trueLong - 0.00569 - 0.00478 * sinDeg(125.04 - 1934.136 * t)
}

private fun meanObliquityEcliptic(t: Double): Double {
    val seconds = 21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))
    return 23.0 + (26.0 + seconds / 60.0) / 60.0
}

private fun obliquityCorrection(t: Double): Double =
    meanObliquityEcliptic(t) + 0.00256 * cosDeg(125.04 - 1934.136 * t)

private fun sunDeclination(t: Double): Double =
    Math.toDegrees(asin(sinDeg(obliquityCorrection(t)) * sinDeg(sunApparentLong(t))))

private fun equationOfTime(t: Double): Double {
    val l0 = geomMeanLongSun(t)
    val e = eccentricityEarthOrbit(t)
    val m = geomMeanAnomalySun(t)
    val y = tanDeg(obliquityCorrection(t) / 2.0).let { it * it }
    return 4.0 * Math.toDegrees(
        y * sinDeg(2 * l0) -
            2 * e * sinDeg(m) +
            4 * e * y * sinDeg(m) * cosDeg(2 * l0) -
            0.5 * y * y * sinDeg(4 * l0) -
            1.25 * e * e * sinDeg(2 * m),
    )
}

private fun minutesToLocalTime(minutes: Int): LocalTime {
    val wrapped = ((minutes % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
    return LocalTime.of(wrapped / 60, wrapped % 60)
}

private fun sinDeg(deg: Double): Double = sin(Math.toRadians(deg))
private fun cosDeg(deg: Double): Double = cos(Math.toRadians(deg))
private fun tanDeg(deg: Double): Double = tan(Math.toRadians(deg))
