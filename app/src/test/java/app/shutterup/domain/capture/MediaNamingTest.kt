package app.shutterup.domain.capture

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaNamingTest {

    @Test
    fun slug_negativeSpace() {
        assertEquals("negative-space", MediaNaming.slug("Negative Space"))
    }

    @Test
    fun slug_punctuationBecomesSingleHyphen() {
        assertEquals("black-white", MediaNaming.slug("Black & White!"))
    }

    @Test
    fun slug_stripsDiacritics() {
        assertEquals("cafe-mornings", MediaNaming.slug("Café Mornings"))
    }

    @Test
    fun slug_emptyOrPunctuationOnly_isUntitled() {
        assertEquals("untitled", MediaNaming.slug(""))
        assertEquals("untitled", MediaNaming.slug("!!!"))
    }

    @Test
    fun displayName_isoDateUnderscoreSlugJpg() {
        val date = LocalDate.of(2026, 9, 19)
        assertEquals("2026-09-19_negative-space.jpg", MediaNaming.displayName(date, "Negative Space"))
    }

    @Test
    fun displayName_zeroPadsMonthAndDay() {
        val date = LocalDate.of(2026, 1, 5)
        assertEquals("2026-01-05_untitled.jpg", MediaNaming.displayName(date, "!!!"))
    }
}
