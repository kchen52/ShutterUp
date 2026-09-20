package app.shutterup.domain.ai

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * AI prompt-generation contract (SPEC §7.1). Every implementation —
 * [NanoPromptGenerator][app.shutterup.data.ai.NanoPromptGenerator] (real),
 * [LibraryPromptGenerator][app.shutterup.domain.ai.LibraryPromptGenerator] (fallback bank),
 * `FakePromptGenerator` (deterministic, debug source set) — is used through this interface,
 * so the app stays fully usable when Nano is unavailable.
 */
interface PromptGenerator {
    suspend fun availability(): Availability
    suspend fun generate(request: GenerationRequest): Result<GeneratedPrompt>
    suspend fun generateSeries(request: GenerationRequest): Result<GeneratedSeries> =
        Result.failure(UnsupportedOperationException("series generation is not supported"))
}

/** Mirrors the ML Kit `FeatureStatus` values relevant to generation. */
enum class Availability {
    AVAILABLE,
    DOWNLOADABLE,
    DOWNLOADING,
    UNAVAILABLE,
}

enum class PromptSource {
    ON_DEVICE_AI,
    LIBRARY,
}

/** Meteorological season derived from the date and, when known, the hemisphere. */
enum class Season {
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER,
}

data class GenerationRequest(
    val date: LocalDate,
    /** User-provided steer, optional (SPEC §7.5). */
    val themeFocus: String?,
    /** Last 30 shown prompt titles, for dedup. */
    val recentTitles: List<String>,
    /** Last 14 themes, for variety. */
    val recentThemes: List<String>,
    val dayOfWeek: DayOfWeek,
    val season: Season,
    val excludeConstraintKinds: Set<String> = emptySet(),
    /**
     * When set, this request is a reroll (or hole-fill) inside an existing
     * series and must stay within that series' theme.
     */
    val seriesTitle: String? = null,
)

data class GeneratedPrompt(
    /** ≤ 40 chars, fits a notification title. */
    val title: String,
    /** ≤ 100 chars, the challenge in one sentence. */
    val oneLiner: String,
    /** 2–4 sentences: what to look for, how to approach it. */
    val details: String,
    /** 1–3 short technique tips. */
    val tips: List<String>,
    /** Optional creative constraint, e.g. "No zoom". */
    val constraint: String?,
    /** ≤ 24 chars label. */
    val theme: String,
    val source: PromptSource,
    /** Populated by the Nano implementation; null for library/fake. */
    val modelName: String? = null,
)

/** Seven related prompts plus a series title (SPEC §7.8). */
data class GeneratedSeries(
    val title: String,
    val theme: String,
    val prompts: List<GeneratedPrompt>,
    val libraryIds: List<String?> = emptyList(),
)
