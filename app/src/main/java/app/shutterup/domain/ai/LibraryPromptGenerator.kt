package app.shutterup.domain.ai

import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.series.librarySeriesTitle
import java.time.Clock
import kotlin.random.Random

/**
 * Bundled-bank generator (SPEC §7.7). Selection excludes ids used since
 * [GenerationRequest.date] minus 180 days; if that filter empties the list,
 * the full library is used (exhaustion fallback). Among remaining entries,
 * tag-matching [GenerationRequest.themeFocus] is preferred, else uniform random.
 */
class LibraryPromptGenerator(
    private val source: LibraryPromptSource,
    private val gamification: GamificationRepository,
    @Suppress("unused") private val clock: Clock,
    private val random: Random,
) : PromptGenerator {

    override suspend fun availability(): Availability = Availability.AVAILABLE

    override suspend fun generate(request: GenerationRequest): Result<GeneratedPrompt> =
        runCatching { pick(request).toGeneratedPrompt() }

    /**
     * Picks one [LibraryPrompt] using the 180-day / theme-focus rules.
     * Fails only when [LibraryPromptSource.loadAll] returns an empty list.
     */
    suspend fun pick(request: GenerationRequest): LibraryPrompt {
        val all = source.loadAll()
        if (all.isEmpty()) {
            error("prompt library is empty")
        }
        val cutoff = request.date.minusDays(180)
        val unused = all.filter { prompt -> !gamification.libraryUsedSince(prompt.id, cutoff) }
        val candidates = unused.ifEmpty { all }
        val preferred = candidates.filter { prompt -> tagsMatchFocus(prompt.tags, request.themeFocus) }
        val pool = preferred.ifEmpty { candidates }
        return pool.random(random)
    }

    /** Resolves a library id by exact title; null when the title is not a library entry. */
    suspend fun findIdByTitle(title: String): String? =
        source.loadAll().firstOrNull { it.title == title }?.id

    override suspend fun generateSeries(request: GenerationRequest): Result<GeneratedSeries> =
        runCatching { pickSeries(request) ?: error("no library theme has seven unused prompts") }

    /**
     * Builds a seven-prompt series from one library theme with at least seven
     * unused entries (180-day exclusion). Returns null when none qualify.
     */
    suspend fun pickSeries(request: GenerationRequest): GeneratedSeries? {
        val all = source.loadAll()
        if (all.isEmpty()) return null
        val cutoff = request.date.minusDays(180)
        val unused = all.filter { prompt -> !gamification.libraryUsedSince(prompt.id, cutoff) }
        val unusedByTheme = unused.groupBy { it.theme }
        val eligible = unusedByTheme.filter { it.value.size >= 7 }
        if (eligible.isEmpty()) return null
        val focused = eligible.filter { (theme, prompts) ->
            tagsMatchFocus(prompts.flatMap { it.tags }.distinct(), request.themeFocus) ||
                (!request.themeFocus.isNullOrBlank() && theme.contains(request.themeFocus, ignoreCase = true))
        }
        val matching = focused.ifEmpty { eligible }
        val indoorEnough = matching.filter { (_, prompts) ->
            prompts.count { prompt -> prompt.tags.any { tag -> tag.equals("indoor", ignoreCase = true) } } >= 4
        }
        val pool = indoorEnough.ifEmpty { matching }
        val fresh = pool.filterKeys { theme ->
            request.recentThemes.none { recent -> recent.equals(theme, ignoreCase = true) }
        }
        val chosen = (fresh.ifEmpty { pool })
        val theme = chosen.keys.toList().random(random)
        val seven = chosen.getValue(theme).shuffled(random).take(7)
        return GeneratedSeries(
            title = librarySeriesTitle(theme),
            theme = theme,
            prompts = seven.map { it.toGeneratedPrompt() },
            libraryIds = seven.map { it.id },
        )
    }

    /**
     * In-series reroll: another unused prompt from [theme], excluding [excludeTitles].
     */
    suspend fun pickFromTheme(
        request: GenerationRequest,
        theme: String,
        excludeTitles: Set<String>,
    ): LibraryPrompt? {
        val all = source.loadAll().filter { it.theme.equals(theme, ignoreCase = true) }
        if (all.isEmpty()) return null
        val cutoff = request.date.minusDays(180)
        val unused = all.filter { prompt -> !gamification.libraryUsedSince(prompt.id, cutoff) }
        val excluded = excludeTitles.map { it.lowercase() }.toSet()
        fun notExcluded(prompt: LibraryPrompt): Boolean =
            prompt.title.lowercase() !in excluded
        val preferred = unused.filter(::notExcluded).ifEmpty { unused }
        val pool = preferred.ifEmpty { all.filter(::notExcluded).ifEmpty { all } }
        return pool.random(random)
    }
}

private fun tagsMatchFocus(tags: List<String>, themeFocus: String?): Boolean {
    if (themeFocus.isNullOrBlank()) return false
    val keywords = themeFocus.lowercase()
        .split(Regex("[^a-z0-9]+"))
        .filter { it.isNotEmpty() }
    if (keywords.isEmpty()) return false
    val loweredTags = tags.map { it.lowercase() }.filter { it.isNotEmpty() }
    if (loweredTags.isEmpty()) return false
    return keywords.any { keyword ->
        loweredTags.any { tag -> tag.contains(keyword) || keyword.contains(tag) }
    }
}
