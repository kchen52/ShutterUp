package app.shutterup.ui.badges

import app.shutterup.domain.model.Achievement
import app.shutterup.domain.model.StreakState
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgesViewModelTest {

    @Test
    fun sectionsFollowDesignOrder() {
        val state = badgesUiState(
            achievements = emptyList(),
            streak = StreakState(0, 0, 0, null),
            selectedId = null,
        )
        assertEquals(
            listOf("Streaks", "Count", "Explorer", "Time of day", "Special"),
            state.sections.map { it.title },
        )
        assertEquals(badgeIdsInSectionOrder(), state.sections.flatMap { section -> section.badges.map { it.id } })
    }

    @Test
    fun lockedCaptionsAreHints_unlockedUseNames() {
        val first = Achievement(
            id = BadgeIds.FIRST_LIGHT,
            unlockedAt = Instant.parse("2026-09-01T08:00:00Z"),
            unlockedOnDate = LocalDate.of(2026, 9, 1),
        )
        val state = badgesUiState(
            achievements = listOf(first),
            streak = StreakState(14, 30, 1, LocalDate.of(2026, 9, 19)),
            selectedId = BadgeIds.STREAK_30,
        )
        assertEquals(14, state.currentStreak)
        assertEquals(30, state.longestStreak)
        val month = state.sections[0].badges.first { it.id == BadgeIds.STREAK_30 }
        assertFalse(month.unlocked)
        assertEquals("Complete 30 days in a row", month.caption)
        assertEquals("Every eligible day of a month. No gaps.", badgeDescription(BadgeIds.PERFECT_MONTH))
        val light = state.sections[4].badges.first { it.id == BadgeIds.FIRST_LIGHT }
        assertTrue(light.unlocked)
        assertEquals("First Light", light.caption)
        assertEquals("1 September 2026", light.unlockedOnLabel)
        assertEquals(BadgeIds.STREAK_30, state.selected?.id)
        assertEquals("30 days completed in a row.", month.description)
    }

    @Test
    fun noneUnlockedStillShowsFullGrid() {
        val state = badgesUiState(emptyList(), StreakState(0, 0, 0, null), null)
        assertEquals(18, state.sections.sumOf { it.badges.size })
        assertTrue(state.sections.flatMap { it.badges }.all { !it.unlocked })
        assertNull(state.selected)
    }
}
