package app.shutterup.data.ai.nano

import app.shutterup.domain.ai.GenerationRequest
import app.shutterup.domain.ai.GeneratedPrompt
import app.shutterup.domain.ai.GeneratedSeries
import app.shutterup.domain.ai.PromptSource

/**
 * Pure prompt-text builder + response mapper for Nano generation (SPEC §7.3).
 * Kept free of ML Kit types so it is JVM-unit-testable; the generator calls it.
 */
object NanoPromptText {
    fun systemPrompt(request: GenerationRequest): String {
        val focus = request.themeFocus?.takeIf { it.isNotBlank() }?.let {
            "The challenge MUST incorporate the theme focus \"$it\" while obeying every rule below."
        } ?: "Auto-generate a short theme label for the challenge."
        val avoidTitles = request.recentTitles.takeIf { it.isNotEmpty() }?.let {
            "Do not repeat anything close to these recent titles: ${it.joinToString("; ") { t -> "\"$t\"" }}."
        } ?: "There are no recent titles to avoid."
        val avoidThemes = request.recentThemes.takeIf { it.isNotEmpty() }?.let {
            "Prefer a theme other than: ${it.joinToString(", ")}."
        } ?: ""
        return """
            You are ShutterUp's photography-challenge writer. Write ONE phone-photography challenge for ${request.dayOfWeek}, ${request.date} (${request.season}).
            $focus
            Rules:
            - Doable with a phone in 30 minutes or less, by an ordinary person, starting wherever they are.
            - Doable anywhere: never require specific weather, places, animals, events, props, or other people.
            - Safe and respectful: no strangers' faces, children, private interiors, roads, heights, water, or trespass.
            - $avoidTitles $avoidThemes
            - Optionally add ONE creative constraint (viewpoint, no zoom, time box, colour limit, count limit).
            - Vary between abstract, observational, and playful.
            - Friendly second-person tone. No emojis, no hashtags, no URLs.
            - Mention only a phone (a phone tripod is fine); never DSLRs, lenses, drones, or filters.
            ${seriesStay(request)}
            Return only the requested structured fields.
        """.trimIndent()
    }

    fun seriesSystemPrompt(request: GenerationRequest): String {
        val focus = request.themeFocus?.takeIf { it.isNotBlank() }?.let {
            "The week MUST incorporate the theme focus \"$it\" while obeying every rule below."
        } ?: "Auto-generate a short unifying theme for the week."
        val avoidTitles = request.recentTitles.takeIf { it.isNotEmpty() }?.let {
            "Do not repeat anything close to these recent titles: ${it.joinToString("; ") { t -> "\"$t\"" }}."
        } ?: "There are no recent titles to avoid."
        val avoidThemes = request.recentThemes.takeIf { it.isNotEmpty() }?.let {
            "Prefer a theme other than: ${it.joinToString(", ")}."
        } ?: ""
        return """
            You are ShutterUp's photography-challenge writer. Write ONE seven-day series of related phone-photography challenges starting ${request.dayOfWeek}, ${request.date} (${request.season}).
            $focus
            The seven prompts belong together as a week with an arc (for example "A Week of Hands", "Seven Windows", "One Room, Seven Ways"). They must vary within the series, not restate each other.
            Rules for every prompt:
            - Doable with a phone in 30 minutes or less, by an ordinary person, starting wherever they are.
            - Doable anywhere: never require specific weather, places, animals, events, props, or other people.
            - At least four of the seven must work indoors.
            - Safe and respectful: no strangers' faces, children, private interiors, roads, heights, water, or trespass.
            - $avoidTitles $avoidThemes
            - Optionally add ONE creative constraint per prompt.
            - Friendly second-person tone. No emojis, no hashtags, no URLs.
            - Mention only a phone (a phone tripod is fine); never DSLRs, lenses, drones, or filters.
            Return only the requested structured fields: a series title, a theme label, and exactly seven prompts.
        """.trimIndent()
    }

    fun map(output: NanoPromptOutput, modelName: String?): GeneratedPrompt = GeneratedPrompt(
        title = output.title,
        oneLiner = output.oneLiner,
        details = output.details,
        tips = output.tips,
        constraint = output.constraint,
        theme = output.theme,
        source = PromptSource.ON_DEVICE_AI,
        modelName = modelName,
    )

    fun mapSeries(output: NanoSeriesOutput, modelName: String?): GeneratedSeries = GeneratedSeries(
        title = output.title,
        theme = output.theme,
        prompts = output.prompts.map { map(it, modelName) },
    )

    private fun seriesStay(request: GenerationRequest): String {
        val title = request.seriesTitle?.takeIf { it.isNotBlank() } ?: return ""
        return "This prompt belongs to the series \"$title\". Stay within that series' theme. Write a fresh prompt that still belongs to the week."
    }
}
