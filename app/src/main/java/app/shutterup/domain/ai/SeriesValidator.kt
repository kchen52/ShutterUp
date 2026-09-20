package app.shutterup.domain.ai

import app.shutterup.domain.series.SeriesCalendar

fun validateSeries(
    series: GeneratedSeries,
    validator: PromptValidator,
    recentTitles: List<String>,
    recentThemeLedes: List<ThemeLede>,
): ValidationResult {
    if (series.title.isBlank() || series.title.length > 40) {
        return ValidationResult.Invalid("series title")
    }
    if (series.prompts.size != SeriesCalendar.LENGTH) {
        return ValidationResult.Invalid("series length")
    }
    val titleCheck = validator.validate(
        GeneratedPrompt(
            title = series.title,
            oneLiner = "Stay with one idea for seven related days of looking.",
            details = "Photograph the same kind of thing from a new angle each day. Keep it doable with a phone wherever you already are.",
            tips = listOf("Keep the phone still."),
            constraint = null,
            theme = series.theme.take(24).ifBlank { "Series" },
            source = series.prompts.first().source,
        ),
        recentTitles = emptyList(),
        recentThemeLedes = emptyList(),
    )
    if (titleCheck is ValidationResult.Invalid) return titleCheck
    val titles = recentTitles.toMutableList()
    val ledes = recentThemeLedes.toMutableList()
    val seen = mutableSetOf<String>()
    for (prompt in series.prompts) {
        val key = prompt.title.lowercase().filter { it.isLetterOrDigit() }
        if (key.isNotEmpty() && !seen.add(key)) {
            return ValidationResult.Invalid("series restatement")
        }
        when (val result = validator.validate(prompt, titles, ledes)) {
            is ValidationResult.Invalid -> return result
            ValidationResult.Valid -> {
                titles += prompt.title
                ledes += ThemeLede(prompt.theme, prompt.oneLiner)
            }
        }
    }
    return ValidationResult.Valid
}
