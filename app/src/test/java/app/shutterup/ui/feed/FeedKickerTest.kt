package app.shutterup.ui.feed

import app.shutterup.ui.calendar.spokenDate
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedKickerTest {
    @Test
    fun designExampleTwelfthSeptember() {
        assertEquals(
            "12 SEP · REFLECTIONS",
            feedKicker(LocalDate.of(2026, 9, 12), "Reflections"),
        )
    }

    @Test
    fun spokenDescriptionUsesMonthNameNotIso() {
        assertEquals(
            "19 September, completed",
            "${spokenDate(LocalDate.of(2026, 9, 19))}, completed",
        )
    }
}
