package app.shutterup.domain.ai

/** Bundled library entry (SPEC §7.7): generated schema plus [id] and [tags]. */
data class LibraryPrompt(
    val id: String,
    val title: String,
    val oneLiner: String,
    val details: String,
    val tips: List<String>,
    val constraint: String?,
    val theme: String,
    val tags: List<String>,
)

fun LibraryPrompt.toGeneratedPrompt(): GeneratedPrompt =
    GeneratedPrompt(
        title = title,
        oneLiner = oneLiner,
        details = details,
        tips = tips,
        constraint = constraint,
        theme = theme,
        source = PromptSource.LIBRARY,
    )
