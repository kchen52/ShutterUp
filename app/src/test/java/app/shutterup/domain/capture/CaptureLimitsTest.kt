package app.shutterup.domain.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureLimitsTest {
    @Test
    fun onePhotoAnswersOnePrompt() {
        assertEquals(1, CaptureLimits.MAX_ENTRIES_PER_DAY)
    }
}
