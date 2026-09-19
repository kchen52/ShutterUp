package app.shutterup.domain.capture

import java.text.Normalizer
import java.time.LocalDate

object MediaNaming {
    fun slug(theme: String): String {
        val nfd = Normalizer.normalize(theme, Normalizer.Form.NFD)
        val withoutDiacritics = buildString(nfd.length) {
            for (ch in nfd) {
                if (ch.category != CharCategory.NON_SPACING_MARK) {
                    append(ch)
                }
            }
        }
        val runs = Regex("[a-z0-9]+").findAll(withoutDiacritics.lowercase()).map { it.value }
        val joined = runs.joinToString("-")
        return joined.ifEmpty { "untitled" }
    }

    fun displayName(date: LocalDate, theme: String): String {
        return "${date}_${slug(theme)}.jpg"
    }
}
