package app.shutterup.ui.feed

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
}
