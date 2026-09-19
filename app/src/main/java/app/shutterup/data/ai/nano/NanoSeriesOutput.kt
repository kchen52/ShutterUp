package app.shutterup.data.ai.nano

import com.google.mlkit.genai.schema.annotations.Generable
import com.google.mlkit.genai.schema.annotations.Guide

/**
 * Structured-output target for a seven-day series (SPEC §7.8).
 */
@Generable("A week of related phone-photography challenges")
data class NanoSeriesOutput(
    @Guide(description = "Short series title, at most 40 characters, e.g. A Week of Hands")
    val title: String,
    @Guide(description = "Unifying theme label, at most 24 characters")
    val theme: String,
    @Guide(description = "Exactly seven related prompts that belong to this series", minItems = 7, maxItems = 7)
    val prompts: List<NanoPromptOutput>,
)
