package app.shutterup.domain.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoClipRulesTest {
    @Test
    fun tenSecondsIsSaveable_elevenIsNot() {
        assertTrue(VideoClipRules.isSaveable(10_000))
        assertFalse(VideoClipRules.isSaveable(10_001))
        assertFalse(VideoClipRules.isSaveable(0))
    }

    @Test
    fun clampWindow_keepsUserStartAndShortensEnd() {
        val window = VideoClipRules.clampWindow(startMs = 2_000, endMs = 20_000, sourceDurationMs = 15_000)
        assertEquals(2_000, window.startMs)
        assertEquals(12_000, window.endMs)
        assertEquals(10_000, window.durationMs)
    }

    @Test
    fun clampWindow_doesNotAutoCentre() {
        val window = VideoClipRules.clampWindow(0, 15_000, 15_000)
        assertEquals(0, window.startMs)
        assertEquals(10_000, window.endMs)
    }
}
