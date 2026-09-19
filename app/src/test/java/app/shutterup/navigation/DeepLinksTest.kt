package app.shutterup.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinksTest {
    @Test
    fun widgetShootUsesDetailHostAndAutoLaunch() {
        assertEquals(
            "shutterup://detail/2026-09-19?autoLaunchCamera=true",
            DeepLinks.detail("2026-09-19", autoLaunchCamera = true),
        )
        assertEquals(
            "shutterup://detail/2026-09-19",
            DeepLinks.detail("2026-09-19"),
        )
    }

    @Test
    fun notificationTapUsesDayHost() {
        assertEquals("shutterup://day/2026-09-19", DeepLinks.day("2026-09-19"))
        assertEquals(
            "shutterup://day/2026-09-19?autoLaunchCamera=true",
            DeepLinks.day("2026-09-19", autoLaunchCamera = true),
        )
    }

    @Test
    fun promptHostsAreRecognised() {
        assertTrue(DeepLinks.isPromptLink("shutterup", "day"))
        assertTrue(DeepLinks.isPromptLink("shutterup", "detail"))
        assertFalse(DeepLinks.isPromptLink("https", "day"))
        assertFalse(DeepLinks.isPromptLink("shutterup", "other"))
    }
}
