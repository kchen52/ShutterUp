package app.shutterup.domain.monthly

/**
 * Theme counts for a month. Ties break alphabetically (case-insensitive) so
 * the same days always produce the same two "loudest" labels.
 */
object ThemeRanking {
    fun rank(themes: List<String>): List<ThemeCount> {
        val counts = linkedMapOf<String, Int>()
        for (raw in themes) {
            val theme = raw.trim()
            if (theme.isEmpty()) continue
            val key = counts.keys.find { it.equals(theme, ignoreCase = true) } ?: theme
            counts[key] = (counts[key] ?: 0) + 1
        }
        return counts.entries
            .map { ThemeCount(it.key, it.value) }
            .sortedWith(compareByDescending<ThemeCount> { it.count }.thenBy { it.theme.lowercase() })
    }

    fun loudest(ranked: List<ThemeCount>, limit: Int = 2): List<String> =
        ranked.take(limit).map { it.theme }
}
