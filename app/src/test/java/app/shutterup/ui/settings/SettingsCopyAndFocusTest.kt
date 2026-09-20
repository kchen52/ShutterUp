package app.shutterup.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun libraryStatusUsesDesignCopy() {
        assertEquals("Built-in library", SettingsCopy.LIBRARY_STATUS)
        assertEquals(
            "Daily prompts come from a curated bank on this phone.",
            SettingsCopy.LIBRARY_SUPPORTING,
        )
        assertEquals("Choosing today's prompt.", SettingsCopy.CHOOSING_PROMPT)
        assertFalse(SettingsCopy.LIBRARY_SUPPORTING.contains("!"))
        assertFalse(SettingsCopy.CHOOSING_PROMPT.contains("!"))
    }

    @Test
    fun privacyBodyMatchesStubLead() {
        assertEquals(
            "Everything stays on your phone. ShutterUp has no internet access.",
            SettingsCopy.ABOUT_LINE,
        )
        assertEquals(SettingsCopy.ABOUT_LINE, SettingsCopy.PRIVACY_BODY.take(SettingsCopy.ABOUT_LINE.length))
        assertFalse(SettingsCopy.PRIVACY_BODY.contains("Gemini Nano"))
    }

    @Test
    fun seriesCopyMatchesBrief() {
        assertEquals("Series", SettingsCopy.SERIES)
        assertEquals(
            "Some weeks arrive as a set of seven related prompts instead of seven separate ones. Turning this on starts a series tomorrow. Today's prompt stays.",
            SettingsCopy.SERIES_SUPPORTING,
        )
        assertEquals(
            "Some weeks arrive as a set of seven related prompts instead of seven separate ones. The current series finishes even if you turn this off.",
            SettingsCopy.SERIES_SUPPORTING_ON,
        )
        assertFalse(SettingsCopy.SERIES_SUPPORTING.contains("!"))
        assertFalse(SettingsCopy.SERIES_SUPPORTING_ON.contains("!"))
    }

    @Test
    fun whereYouAreCopyIsPlainAndOffline() {
        assertEquals("Where you are", SettingsCopy.WHERE_YOU_ARE)
        assertEquals("Not set", SettingsCopy.WHERE_YOU_ARE_UNSET)
        assertEquals(
            "A city-level guess for daylight. No location permission, nothing leaves the phone.",
            SettingsCopy.WHERE_YOU_ARE_SUPPORTING,
        )
        assertFalse(SettingsCopy.WHERE_YOU_ARE_SUPPORTING.contains("!"))
    }

    @Test
    fun backupCopyIsQuietAndOffline() {
        assertEquals("Backup progress", SettingsCopy.BACKUP)
        assertEquals("Restore progress", SettingsCopy.RESTORE)
        assertFalse(SettingsCopy.BACKUP_SUPPORTING.contains("!"))
        assertFalse(SettingsCopy.RESTORE_SUPPORTING.contains("!"))
        assertFalse(SettingsCopy.RESTORE_BODY.contains("!"))
        assertFalse(SettingsCopy.BACKUP_FAILED.contains("!"))
        assertTrue(SettingsCopy.BACKUP_SUPPORTING.contains("Gallery"))
        assertTrue(SettingsCopy.RESTORE_SUPPORTING.contains("Gallery"))
    }
}
