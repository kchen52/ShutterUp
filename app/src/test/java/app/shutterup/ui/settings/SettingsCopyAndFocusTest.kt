package app.shutterup.ui.settings

import app.shutterup.domain.ai.Availability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsCopyAndFocusTest {

    @Test
    fun themeFocusIsCappedAtSixtyAndBlankBecomesNull() {
        assertNull(sanitizeThemeFocus("   "))
        assertEquals("windows", sanitizeThemeFocus("  windows  "))
        val long = "a".repeat(80)
        assertEquals(60, checkNotNull(sanitizeThemeFocus(long)).length)
    }

    @Test
    fun aiStatusUsesDesignCopy() {
        assertEquals(SettingsCopy.AI_READY, aiStatusLine(Availability.AVAILABLE, null))
        assertEquals("Ready · Gemini Nano v2", aiStatusLine(Availability.AVAILABLE, "Gemini Nano v2"))
        assertEquals(SettingsCopy.AI_PREPARING, aiStatusLine(Availability.DOWNLOADING, null))
        assertEquals(SettingsCopy.AI_UNAVAILABLE_STATUS, aiStatusLine(Availability.UNAVAILABLE, null))
        assertEquals(SettingsCopy.AI_UNAVAILABLE, aiSupporting(Availability.UNAVAILABLE))
        assertNull(aiSupporting(Availability.AVAILABLE))
    }

    @Test
    fun privacyBodyMatchesStubLead() {
        assertEquals(
            "Everything stays on your phone. ShutterUp has no internet access.",
            SettingsCopy.ABOUT_LINE,
        )
        assertEquals(SettingsCopy.ABOUT_LINE, SettingsCopy.PRIVACY_BODY.take(SettingsCopy.ABOUT_LINE.length))
    }
}
