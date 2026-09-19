package app.shutterup.domain.ai

import java.time.LocalDate
import java.time.Month

fun seasonForDate(date: LocalDate): Season =
    when (date.month) {
        Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> Season.WINTER
        Month.MARCH, Month.APRIL, Month.MAY -> Season.SPRING
        Month.JUNE, Month.JULY, Month.AUGUST -> Season.SUMMER
        Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> Season.AUTUMN
    }
