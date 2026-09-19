package app.shutterup.domain.monthly

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeRankingTest {
    @Test
    fun ranksByCountDescending() {
        val ranked = ThemeRanking.rank(
            listOf("Reflections", "Looking up", "Reflections", "Low light", "Reflections", "Looking up"),
        )
        assertEquals(
            listOf(
                ThemeCount("Reflections", 3),
                ThemeCount("Looking up", 2),
                ThemeCount("Low light", 1),
            ),
            ranked,
        )
        assertEquals(listOf("Reflections", "Looking up"), ThemeRanking.loudest(ranked))
    }

    @Test
    fun tiesBreakAlphabetically() {
        val ranked = ThemeRanking.rank(listOf("Steam", "Glass", "Steam", "Glass"))
        assertEquals(
            listOf(ThemeCount("Glass", 2), ThemeCount("Steam", 2)),
            ranked,
        )
    }

    @Test
    fun mergesCaseInsensitiveLabelsKeepingFirstSpelling() {
        val ranked = ThemeRanking.rank(listOf("Reflections", "reflections", "REFLECTIONS", "Light"))
        assertEquals(ThemeCount("Reflections", 3), ranked.first())
        assertEquals(ThemeCount("Light", 1), ranked[1])
    }

    @Test
    fun skipsBlankThemes() {
        val ranked = ThemeRanking.rank(listOf("  ", "", "Hands", "Hands"))
        assertEquals(listOf(ThemeCount("Hands", 2)), ranked)
    }

    @Test
    fun emptyInput() {
        assertEquals(emptyList<ThemeCount>(), ThemeRanking.rank(emptyList()))
        assertEquals(emptyList<String>(), ThemeRanking.loudest(emptyList()))
    }

    @Test
    fun loudestLimitOne() {
        val ranked = ThemeRanking.rank(listOf("A", "B", "A"))
        assertEquals(listOf("A"), ThemeRanking.loudest(ranked, limit = 1))
    }
}
