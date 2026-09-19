package app.shutterup.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateKickerTest {
    @Test
    fun tuesdayNineteenthSeptember() {
        assertEquals("SATURDAY 19 SEPTEMBER", dateKicker(LocalDate.of(2026, 9, 19)))
    }
}
