package app.shutterup.domain.ai

import app.shutterup.domain.repository.GamificationRepository
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
