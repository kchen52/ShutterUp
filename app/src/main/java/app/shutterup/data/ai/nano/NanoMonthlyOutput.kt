package app.shutterup.data.ai.nano

import com.google.mlkit.genai.schema.annotations.Generable
import com.google.mlkit.genai.schema.annotations.Guide

/**
 * Structured-output target for The Monthly (SPEC §7.9). Length and voice
 * are still enforced by [app.shutterup.domain.ai.MonthlyIssueValidator].
 */
@Generable("A short written page describing a month of phone photographs")
data class NanoMonthlyOutput(
    @Guide(description = "Short evocative phrase, at most 30 characters, sentence case, ending with a full stop. Not a list of themes. e.g. Light and glass.")
    val headline: String,
    @Guide(description = "Two or three sentences in second person, present tense, on the shape of the month. Do not re-list theme names. No exclamation marks, no praise, no guilt, no claims about what the photographs look like.")
    val body: String,
)
