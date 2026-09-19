package app.shutterup.domain.monthly

import app.shutterup.domain.ai.MonthlyIssueValidator
import java.util.Locale

/**
 * Deterministic copy for a month when Nano is unavailable — the common case,
 * and the whole offline promise. Built from the same snapshot Nano would see:
 * day count, ranked themes, longest run, whether notes were written.
 *
 * Warm English, second person, present tense. Never a stat line, never guilt,
 * never a quoted note, never an exclamation.
 */
object MonthlyIssueFallback {
    fun compose(snapshot: MonthlyIssueSnapshot): MonthlyIssueCopy {
        val themes = ThemeRanking.loudest(snapshot.rankedThemes, 2)
        return MonthlyIssueCopy(
            headline = headlineFor(snapshot.completedCount, themes),
            body = bodyFor(snapshot, themes),
        )
    }

    internal fun headlineFor(completedCount: Int, themes: List<String>): String {
        val primary = themes.getOrNull(0)
        val secondary = themes.getOrNull(1)
        val candidate = when {
            completedCount <= 3 && secondary == null -> asHeadline("A quiet month")
            primary != null && secondary != null -> {
                val joined = "${titleCase(primary)} and ${primary.let { lower(secondary) }}"
                asHeadline(joined)
            }
            primary != null -> asHeadline(titleCase(primary))
            else -> asHeadline("A month of looking")
        }
        return if (candidate.length <= MonthlyIssueValidator.HEADLINE_MAX) {
            candidate
        } else if (primary != null) {
            asHeadline(titleCase(primary))
        } else {
            asHeadline("A month of looking")
        }
    }

    private fun bodyFor(snapshot: MonthlyIssueSnapshot, themes: List<String>): String {
        val n = snapshot.completedCount
        val notes = snapshot.notesCount
        val run = snapshot.longestRun
        val opening = openingSentence(n, themes)
        val second = secondSentence(n, notes, run)
        val third = thirdSentence(n, notes, run, themes, second)
        return listOfNotNull(opening, second, third).joinToString(" ")
    }

    private fun openingSentence(n: Int, themes: List<String>): String {
        val word = capitalize(dayWord(n))
        val phrase = themePhrase(themes)
        return when {
            n == 1 -> "One day of $phrase."
            themes.size <= 1 -> "$word days of $phrase."
            else -> "$word days, mostly $phrase."
        }
    }

    private fun secondSentence(n: Int, notes: Int, run: Int): String = when {
        notes > 0 && notes == n && n == 1 -> "You wrote a note that day."
        notes > 0 && notes == n -> "You wrote on every day you shot."
        notes > 0 && notes * 2 >= n && n >= 4 -> "You wrote through most of the month."
        notes > 0 -> "You wrote on ${dayWord(notes)} of them."
        n == 1 -> "The rest of the month is paper."
        n <= 3 -> "A small set, held still."
        run >= 4 -> "The longest stretch was ${dayWord(run)} days."
        else -> "The days sat a little apart."
    }

    private fun thirdSentence(
        n: Int,
        notes: Int,
        run: Int,
        themes: List<String>,
        second: String,
    ): String? {
        if (n <= 6) return null
        val secondAboutNotes = second.startsWith("You wrote")
        val secondAboutRun = second.startsWith("The longest stretch")
        return when {
            secondAboutNotes && run >= 5 ->
                "The longest stretch was ${dayWord(run)} days."
            secondAboutRun && themes.size == 1 ->
                "You kept returning to the same kind of looking."
            secondAboutNotes && themes.size == 1 && n >= 8 ->
                "You kept returning to the same kind of looking."
            !secondAboutRun && run >= 7 ->
                "The longest stretch was ${dayWord(run)} days."
            else -> null
        }
    }

    private fun themePhrase(themes: List<String>): String = when (themes.size) {
        0 -> "looking"
        1 -> lower(themes[0])
        else -> "${lower(themes[0])} and ${lower(themes[1])}"
    }

    private fun asHeadline(phrase: String): String {
        val trimmed = phrase.trim().trimEnd('.').trim()
        return "${titleCase(trimmed)}."
    }

    private fun titleCase(value: String): String {
        if (value.isEmpty()) return value
        return value.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.ENGLISH) else ch.toString()
        }
    }

    private fun lower(value: String): String = value.lowercase(Locale.ENGLISH)

    private fun capitalize(value: String): String = titleCase(value)
}

internal fun dayWord(n: Int): String = when (n) {
    1 -> "one"
    2 -> "two"
    3 -> "three"
    4 -> "four"
    5 -> "five"
    6 -> "six"
    7 -> "seven"
    8 -> "eight"
    9 -> "nine"
    10 -> "ten"
    11 -> "eleven"
    12 -> "twelve"
    13 -> "thirteen"
    14 -> "fourteen"
    15 -> "fifteen"
    16 -> "sixteen"
    17 -> "seventeen"
    18 -> "eighteen"
    19 -> "nineteen"
    20 -> "twenty"
    21 -> "twenty-one"
    22 -> "twenty-two"
    23 -> "twenty-three"
    24 -> "twenty-four"
    25 -> "twenty-five"
    26 -> "twenty-six"
    27 -> "twenty-seven"
    28 -> "twenty-eight"
    29 -> "twenty-nine"
    30 -> "thirty"
    31 -> "thirty-one"
    else -> n.toString()
}
