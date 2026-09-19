package app.shutterup.domain.ai

import app.shutterup.domain.monthly.MonthlyIssueCopy

/**
 * Post-filter for a month's headline and body (SPEC §7.9). First failing
 * check wins. Rejects length, punctuation, emoji, hashtags, URLs, praise,
 * guilt, invented visual detail, and verbatim notes.
 */
class MonthlyIssueValidator {
    fun validate(
        copy: MonthlyIssueCopy,
        notes: List<String> = emptyList(),
    ): ValidationResult {
        headlineCheck(copy.headline)?.let { return it }
        bodyCheck(copy.body)?.let { return it }
        punctuationCheck(copy)?.let { return it }
        urlCheck(copy)?.let { return it }
        hashtagCheck(copy)?.let { return it }
        emojiCheck(copy)?.let { return it }
        praiseCheck(copy)?.let { return it }
        guiltCheck(copy)?.let { return it }
        visualDetailCheck(copy)?.let { return it }
        verbatimNoteCheck(copy, notes)?.let { return it }
        return ValidationResult.Valid
    }

    private fun headlineCheck(headline: String): ValidationResult.Invalid? {
        val trimmed = headline.trim()
        if (trimmed.isEmpty()) return ValidationResult.Invalid("headline empty")
        if (trimmed.length > HEADLINE_MAX) return ValidationResult.Invalid("headline length")
        if (!trimmed.endsWith('.')) return ValidationResult.Invalid("headline period")
        if (trimmed.count { it == '.' } != 1) return ValidationResult.Invalid("headline period")
        val withoutPeriod = trimmed.dropLast(1)
        if (withoutPeriod.isEmpty()) return ValidationResult.Invalid("headline empty")
        if (!withoutPeriod.first().isUpperCase()) return ValidationResult.Invalid("headline case")
        return null
    }

    private fun bodyCheck(body: String): ValidationResult.Invalid? {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return ValidationResult.Invalid("body empty")
        if (trimmed.length > BODY_MAX) return ValidationResult.Invalid("body length")
        val sentences = sentenceCount(trimmed)
        if (sentences !in 2..3) return ValidationResult.Invalid("body sentence count")
        return null
    }

    private fun punctuationCheck(copy: MonthlyIssueCopy): ValidationResult.Invalid? {
        val fields = listOf(copy.headline, copy.body)
        if (fields.any { it.contains('!') }) return ValidationResult.Invalid("exclamation")
        return null
    }

    private fun urlCheck(copy: MonthlyIssueCopy): ValidationResult.Invalid? {
        val fields = listOf(copy.headline, copy.body)
        val found = fields.any { text ->
            text.contains("http://", ignoreCase = true) ||
                text.contains("https://", ignoreCase = true) ||
                text.contains("www.", ignoreCase = true)
        }
        return if (found) ValidationResult.Invalid("url") else null
    }

    private fun hashtagCheck(copy: MonthlyIssueCopy): ValidationResult.Invalid? {
        val found = listOf(copy.headline, copy.body).any { text ->
            val chars = text.toCharArray()
            for (i in 0 until chars.size - 1) {
                if (chars[i] == '#' && isWordChar(chars[i + 1])) return@any true
            }
            false
        }
        return if (found) ValidationResult.Invalid("hashtag") else null
    }

    private fun emojiCheck(copy: MonthlyIssueCopy): ValidationResult.Invalid? {
        val found = listOf(copy.headline, copy.body).any { text -> text.any { it.isIssueEmojiLike() } }
        return if (found) ValidationResult.Invalid("emoji") else null
    }

    private fun praiseCheck(copy: MonthlyIssueCopy): ValidationResult.Invalid? {
        val haystack = "${copy.headline} ${copy.body}".lowercase()
        for (phrase in PRAISE) {
            if (haystack.contains(phrase)) return ValidationResult.Invalid("praise")
        }
        return null
    }

    private fun guiltCheck(copy: MonthlyIssueCopy): ValidationResult.Invalid? {
        val haystack = "${copy.headline} ${copy.body}".lowercase()
        for (phrase in GUILT) {
            if (haystack.contains(phrase)) return ValidationResult.Invalid("guilt")
        }
        return null
    }

    private fun visualDetailCheck(copy: MonthlyIssueCopy): ValidationResult.Invalid? {
        val haystack = "${copy.headline} ${copy.body}".lowercase()
        for (phrase in VISUAL_CLAIMS) {
            if (haystack.contains(phrase)) return ValidationResult.Invalid("visual detail")
        }
        return null
    }

    private fun verbatimNoteCheck(
        copy: MonthlyIssueCopy,
        notes: List<String>,
    ): ValidationResult.Invalid? {
        val haystack = "${copy.headline} ${copy.body}".lowercase()
        for (note in notes) {
            val snippet = note.trim()
            if (snippet.length < 8) continue
            if (haystack.contains(snippet.lowercase())) {
                return ValidationResult.Invalid("verbatim note")
            }
        }
        return null
    }

    companion object {
        const val HEADLINE_MAX = 30
        const val BODY_MAX = 480
    }
}

private val PRAISE = listOf(
    "great work",
    "amazing month",
    "amazing work",
    "well done",
    "so proud",
    "congratulations",
    "impressive",
    "you did it",
    "keep it up",
    "fantastic",
    "wonderful month",
    "beautiful photos",
    "stunning",
    "you crushed",
    "nice job",
    "good job",
    "proud of you",
)

private val GUILT = listOf(
    "you missed",
    "missed days",
    "you didn't",
    "you did not",
    "should have",
    "failed",
    "broke your",
    "fell behind",
    "you skipped",
    "incomplete month",
    "don't forget",
    "do not forget",
    "only managed",
    "you forgot",
)

private val VISUAL_CLAIMS = listOf(
    "your photos",
    "the photos show",
    "in the pictures",
    "the images",
    "your pictures",
    "looks blurry",
    "overexposed",
    "underexposed",
    "in the frame you",
    "the photographs",
    "what the photos",
    "you photographed a",
)

private fun sentenceCount(text: String): Int =
    text.split(Regex("[.!?]+"))
        .map { it.trim() }
        .count { it.isNotEmpty() }

private fun isWordChar(ch: Char): Boolean = ch == '_' || ch.isLetterOrDigit()

private fun Char.isIssueEmojiLike(): Boolean {
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
