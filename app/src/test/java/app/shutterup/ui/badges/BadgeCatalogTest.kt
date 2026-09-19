package app.shutterup.ui.badges

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeCatalogTest {

    @Test
    fun sectionsFollowDesignOrder() {
        assertEquals(
            listOf("Streaks", "Count", "Explorer", "Time of day", "Special"),
            BadgeSections.map { it.title },
        )
        assertEquals(
            listOf(
                BadgeIds.STREAK_7,
                BadgeIds.STREAK_30,
                BadgeIds.STREAK_100,
                BadgeIds.STREAK_365,
                BadgeIds.TOTAL_10,
                BadgeIds.TOTAL_50,
                BadgeIds.TOTAL_100,
                BadgeIds.TOTAL_250,
                BadgeIds.TOTAL_500,
                BadgeIds.EXPLORER_10,
                BadgeIds.EXPLORER_25,
                BadgeIds.EARLY_BIRD,
                BadgeIds.NIGHT_OWL,
                BadgeIds.FIRST_LIGHT,
                BadgeIds.PERFECT_MONTH,
                BadgeIds.COMEBACK,
                BadgeIds.ICEBERG,
                BadgeIds.CURATOR,
            ),
            badgeIdsInSectionOrder(),
        )
    }

    @Test
    fun catalogCoversAllEighteenIds() {
        val ids = listOf(
            BadgeIds.FIRST_LIGHT,
            BadgeIds.STREAK_7,
            BadgeIds.STREAK_30,
            BadgeIds.STREAK_100,
            BadgeIds.STREAK_365,
            BadgeIds.TOTAL_10,
            BadgeIds.TOTAL_50,
            BadgeIds.TOTAL_100,
            BadgeIds.TOTAL_250,
            BadgeIds.TOTAL_500,
            BadgeIds.EXPLORER_10,
            BadgeIds.EXPLORER_25,
            BadgeIds.EARLY_BIRD,
            BadgeIds.NIGHT_OWL,
            BadgeIds.PERFECT_MONTH,
            BadgeIds.COMEBACK,
            BadgeIds.ICEBERG,
            BadgeIds.CURATOR,
        )
        assertEquals(18, ids.toSet().size)
        assertEquals(ids.toSet(), BadgeCatalog.keys)
        assertEquals(ids.toSet(), badgeIdsInSectionOrder().toSet())
    }

    @Test
    fun displayNameIsNeverTheHint() {
        badgeIdsInSectionOrder().forEach { id ->
            assertEquals(BadgeCatalog.getValue(id).unlockedName, badgeDisplayName(id))
            assertEquals(BadgeCatalog.getValue(id).lockedHint, badgeLockedHint(id))
            assertTrue(badgeDisplayName(id) != badgeLockedHint(id))
        }
    }

    @Test
    fun gridCaptionUsesHintOnlyWhenLocked() {
        assertEquals("Month of Light", badgeGridCaption(BadgeIds.STREAK_30, unlocked = true))
        assertEquals("Complete 30 days in a row", badgeGridCaption(BadgeIds.STREAK_30, unlocked = false))
    }

    @Test
    fun explorerCaptionsAreDifferentiated() {
        assertEquals("Theme Explorer · 10", badgeDisplayName(BadgeIds.EXPLORER_10))
        assertEquals("Theme Explorer · 25", badgeDisplayName(BadgeIds.EXPLORER_25))
    }

    @Test
    fun designCopyIsVerbatim() {
        assertEquals(
            "Every eligible day of a month. No gaps.",
            badgeDescription(BadgeIds.PERFECT_MONTH),
        )
        assertEquals(
            "Back after three or more quiet days.",
            badgeDescription(BadgeIds.COMEBACK),
        )
    }

    @Test
    fun hintsUseNumerals() {
        assertEquals("5 times near notify", badgeLockedHint(BadgeIds.EARLY_BIRD))
        assertEquals("After 21:00, 5 times", badgeLockedHint(BadgeIds.NIGHT_OWL))
    }

    @Test
    fun shutterCount250And500ShareFourDots() {
        assertEquals(4, BadgeCatalog.getValue(BadgeIds.TOTAL_250).tierDots)
        assertEquals(4, BadgeCatalog.getValue(BadgeIds.TOTAL_500).tierDots)
        assertEquals(8, BadgeCatalog.getValue(BadgeIds.TOTAL_250).glyphParam)
        assertEquals(9, BadgeCatalog.getValue(BadgeIds.TOTAL_500).glyphParam)
    }
}
