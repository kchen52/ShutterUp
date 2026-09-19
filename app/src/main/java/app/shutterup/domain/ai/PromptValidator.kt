package app.shutterup.domain.ai

import javax.inject.Inject
import javax.inject.Named

data class ThemeLede(
    val theme: String,
    val oneLiner: String,
)

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val reason: String) : ValidationResult
}

val DEFAULT_GEAR_TERMS: Set<String> = setOf(
    "dslr",
    "mirrorless",
    "50mm",
    "35mm",
    "85mm",
    "telephoto",
    "drone",
    "nd filter",
    "polarizer",
    "polarizing",
    "speedlight",
)

/**
 * Post-filter for generated prompts (SPEC §7.4). First failing check wins.
 *
 * Blocklist and gear checks are case-insensitive **substring** matches on purpose:
 * that is conservative (e.g. "roof" also hits "proof") so the model is steered
 * away from near-matches rather than allowed through on a word-boundary technicality.
 * "tripod" is not a gear term: a phone tripod is allowed.
 */
class PromptValidator @Inject constructor(
    @Named("blockedTerms") private val blockedTerms: Set<String>,
    @Named("gearTerms") private val gearTerms: Set<String>,
) {
    fun validate(
        candidate: GeneratedPrompt,
        recentTitles: List<String>,
        recentThemeLedes: List<ThemeLede>,
    ): ValidationResult {
        schemaCheck(candidate)?.let { return it }
        titleDedupCheck(candidate.title, recentTitles)?.let { return it }
        themeLedeDedupCheck(candidate, recentThemeLedes)?.let { return it }
        blocklistCheck(candidate)?.let { return it }
        gearCheck(candidate)?.let { return it }
        urlCheck(candidate)?.let { return it }
        hashtagCheck(candidate)?.let { return it }
        emojiCheck(candidate)?.let { return it }
        return ValidationResult.Valid
    }

    private fun schemaCheck(candidate: GeneratedPrompt): ValidationResult.Invalid? {
        if (candidate.title.length > 40) {
            return ValidationResult.Invalid("title length")
        }
        if (candidate.oneLiner.length > 100) {
            return ValidationResult.Invalid("oneLiner length")
        }
        if (candidate.theme.length > 24) {
            return ValidationResult.Invalid("theme length")
        }
        if (candidate.tips.size !in 1..3 || candidate.tips.any { it.isBlank() }) {
            return ValidationResult.Invalid("tips count")
        }
        val sentences = sentenceCount(candidate.details)
        if (sentences !in 2..4) {
            return ValidationResult.Invalid("details sentence count")
        }
        return null
    }

    private fun titleDedupCheck(title: String, recentTitles: List<String>): ValidationResult.Invalid? {
        val key = normalize(title)
        if (key.isEmpty()) return null
        val recent = recentTitles.map { normalize(it) }.toSet()
        return if (key in recent) ValidationResult.Invalid("title duplicate") else null
    }

    private fun themeLedeDedupCheck(
        candidate: GeneratedPrompt,
        recentThemeLedes: List<ThemeLede>,
    ): ValidationResult.Invalid? {
        val key = themeLedeKey(candidate.theme, candidate.oneLiner)
        if (recentThemeLedes.any { themeLedeKey(it.theme, it.oneLiner) == key }) {
            return ValidationResult.Invalid("theme lede duplicate")
        }
        return null
    }

    private fun blocklistCheck(candidate: GeneratedPrompt): ValidationResult.Invalid? {
        val haystacks = textFieldsForBlocklist(candidate)
        for (term in blockedTerms) {
            if (term.isBlank()) continue
            if (haystacks.any { it.contains(term, ignoreCase = true) }) {
                return ValidationResult.Invalid("blocklist")
            }
        }
        return null
    }

    private fun gearCheck(candidate: GeneratedPrompt): ValidationResult.Invalid? {
        val haystacks = textFieldsForGear(candidate)
        for (term in gearTerms) {
            if (term.isBlank()) continue
            if (haystacks.any { it.contains(term, ignoreCase = true) }) {
                return ValidationResult.Invalid("gear")
            }
        }
        return null
    }

    private fun urlCheck(candidate: GeneratedPrompt): ValidationResult.Invalid? {
        val haystacks = allTextFields(candidate)
        val found = haystacks.any { text ->
            text.contains("http://", ignoreCase = true) ||
                text.contains("https://", ignoreCase = true) ||
                text.contains("www.", ignoreCase = true)
        }
        return if (found) ValidationResult.Invalid("url") else null
    }

    private fun hashtagCheck(candidate: GeneratedPrompt): ValidationResult.Invalid? {
        val found = allTextFields(candidate).any { text ->
            val chars = text.toCharArray()
            for (i in 0 until chars.size - 1) {
                if (chars[i] == '#' && isWordChar(chars[i + 1])) return@any true
            }
            false
        }
        return if (found) ValidationResult.Invalid("hashtag") else null
    }

    private fun emojiCheck(candidate: GeneratedPrompt): ValidationResult.Invalid? {
        val found = allTextFields(candidate).any { text -> text.any { it.isEmojiLike() } }
        return if (found) ValidationResult.Invalid("emoji") else null
    }

    private fun textFieldsForBlocklist(candidate: GeneratedPrompt): List<String> =
        buildList {
            add(candidate.title)
            add(candidate.oneLiner)
            add(candidate.details)
            addAll(candidate.tips)
            candidate.constraint?.let { add(it) }
        }

    private fun textFieldsForGear(candidate: GeneratedPrompt): List<String> =
        buildList {
            add(candidate.details)
            addAll(candidate.tips)
            candidate.constraint?.let { add(it) }
        }

    private fun allTextFields(candidate: GeneratedPrompt): List<String> =
        buildList {
            add(candidate.title)
            add(candidate.oneLiner)
            add(candidate.details)
            addAll(candidate.tips)
            candidate.constraint?.let { add(it) }
            add(candidate.theme)
        }
}

private fun sentenceCount(details: String): Int =
    details.split(Regex("[.!?]+"))
        .map { it.trim() }
        .count { it.isNotEmpty() }

private fun normalize(value: String): String =
    buildString(value.length) {
        for (ch in value) {
            if (ch.isLetterOrDigit()) append(ch.lowercaseChar())
        }
    }

private fun themeLedeKey(theme: String, oneLiner: String): String {
    val words = oneLiner.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(4)
    return normalize(theme) + '\u0000' + normalize(words.joinToString(" "))
}

private fun isWordChar(ch: Char): Boolean = ch == '_' || ch.isLetterOrDigit()

private fun Char.isEmojiLike(): Boolean {
    if (isSurrogate()) return true
    val c = code
    return c in 0x2600..0x27BF ||
        c in 0x2B00..0x2BFF ||
        c in 0xFE00..0xFE0F ||
        c == 0x200D ||
        c == 0x20E3 ||
        c == 0x00A9 ||
        c == 0x00AE ||
        c == 0x2139
}
