package app.shutterup.ui.detail

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Uppercase date kicker, e.g. `TUESDAY 19 SEPTEMBER`. */
fun dateKicker(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)).uppercase(Locale.ENGLISH)
