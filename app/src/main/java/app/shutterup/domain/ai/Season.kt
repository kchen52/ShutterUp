package app.shutterup.domain.ai

import java.time.LocalDate
import java.time.Month

/**
 * Meteorological season for [date].
 *
 * When [latitude] is known and south of the equator, the northern mapping is
 * flipped. With no latitude (the default), the northern-hemisphere mapping is
 * kept so existing generation is unchanged.
 */
fun seasonForDate(date: LocalDate, latitude: Double? = null): Season {
    val northern = northernSeason(date.month)
    val southern = latitude != null && latitude < 0.0
    if (!southern) return northern
    return when (northern) {
        Season.WINTER -> Season.SUMMER
        Season.SPRING -> Season.AUTUMN
        Season.SUMMER -> Season.WINTER
        Season.AUTUMN -> Season.SPRING
    }
}

private fun northernSeason(month: Month): Season = when (month) {
    Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> Season.WINTER
    Month.MARCH, Month.APRIL, Month.MAY -> Season.SPRING
    Month.JUNE, Month.JULY, Month.AUGUST -> Season.SUMMER
    Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> Season.AUTUMN
}
