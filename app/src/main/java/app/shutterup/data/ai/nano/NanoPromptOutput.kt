package app.shutterup.data.ai.nano

import com.google.mlkit.genai.schema.annotations.Generable
import com.google.mlkit.genai.schema.annotations.Guide

/**
 * Structured-output target for Nano generation (SPEC §7.2). Length limits are
 * described, not constrained (String supports only description/enumValues), so
 * [app.shutterup.domain.ai.PromptValidator] still enforces them post-generation.
 */
@Generable("A phone-photography challenge prompt")
data class NanoPromptOutput(
    @Guide(description = "Short challenge title, at most 40 characters")
    val title: String,
    @Guide(description = "The challenge in one sentence, at most 100 characters")
    val oneLiner: String,
    @Guide(description = "Two to four sentences: what to look for and how to approach it")
    val details: String,
    @Guide(description = "One to three short technique tips", minItems = 1, maxItems = 3)
    val tips: List<String>,
    @Guide(description = "An optional creative constraint, or null when none fits")
    val constraint: String?,
    @Guide(description = "A short theme label, at most 24 characters")
    val theme: String,
)
