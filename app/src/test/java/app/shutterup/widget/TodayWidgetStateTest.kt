package app.shutterup.widget

import app.shutterup.domain.model.DayStatus
import app.shutterup.ui.detail.samplePrompt
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayWidgetStateTest {
    private val date = LocalDate.of(2026, 9, 19)

    @Test
    fun mapsThemeConstraintStreakAndKicker() {
        val state = TodayWidgetState.from(
            date = date,
            prompt = samplePrompt(),
            streakDays = 14,
            paused = false,
            thumbPath = null,
        )
        assertEquals("SATURDAY · REFLECTIONS", state.kicker)
        assertEquals("Don't rotate the photo afterwards.", state.constraint)
        assertEquals("14 days", state.streakLabel)
        assertEquals("Find the sky in a puddle", state.title)
        assertFalse(state.completed)
        assertEquals("2026-09-19", state.dateIso)
    }

    @Test
    fun pausedUsesTitleSlot() {
        val state = TodayWidgetState.from(date, samplePrompt(), 3, paused = true, thumbPath = null)
        assertEquals("Paused", state.title)
        assertFalse(state.completed)
    }

    @Test
    fun completedFlagsPhotoDays() {
        val prompt = samplePrompt().copy(status = DayStatus.COMPLETED)
        val state = TodayWidgetState.from(date, prompt, 1, paused = false, thumbPath = "/tmp/t.jpg")
        assertTrue(state.completed)
        assertEquals("1 day", state.streakLabel)
        assertEquals("/tmp/t.jpg", state.thumbPath)
    }
}
