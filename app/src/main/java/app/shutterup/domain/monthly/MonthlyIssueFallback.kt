package app.shutterup.domain.monthly

import java.time.YearMonth
import java.util.Locale

/**
 * Deterministic copy for a month when Nano is unavailable — the common case,
 * and the whole offline promise. Headline comes from the hand-written theme
 * bank. Body is the shape of the month only: day count, notes, longest run,
 * a quiet nod to the month before. Themes live in the kicker, not here.
 *
 * Warm English, second person, present tense. Never a stat line, never guilt,
 * never a quoted note, never an exclamation.
 */
object MonthlyIssueFallback {
    fun compose(snapshot: MonthlyIssueSnapshot): MonthlyIssueCopy {
        val themes = ThemeRanking.loudest(snapshot.rankedThemes, 2)
        return MonthlyIssueCopy(
            headline = headlineFor(snapshot.yearMonth, themes),
            body = bodyFor(snapshot),
        )
    }

    internal fun headlineFor(yearMonth: YearMonth, themes: List<String>): String {
        val primary = themes.firstOrNull()?.takeIf { it.isNotBlank() }
        return if (primary != null) {
            MonthlyIssueHeadlines.phraseFor(primary, yearMonth)
        } else {
            MonthlyIssueHeadlines.phraseFor("Looking", yearMonth)
        }
    }

    private fun bodyFor(snapshot: MonthlyIssueSnapshot): String {
        val n = snapshot.completedCount
        val opening = openingSentence(n)
        val shape = shapeSentence(n, snapshot.notesCount, snapshot.longestRun)
        val previous = previousSentence(n, snapshot.previousCompletedCount)
        return listOfNotNull(opening, shape, previous).joinToString(" ")
    }

    private fun openingSentence(n: Int): String = when (n) {
        1 -> "One day."
        else -> "${capitalize(dayWord(n))} days."
    }

    private fun shapeSentence(n: Int, notes: Int, run: Int): String {
        val notesPart = notesClause(n, notes)
        val runPart = runClause(n, run)
        return when {
            notesPart != null && runPart != null -> "${capitalize(notesPart)}, and $runPart."
            notesPart != null -> "${capitalize(notesPart)}."
            runPart != null -> "${capitalize(runPart)}."
            n == 1 -> "The rest of the month is paper."
            n <= 3 -> "A small set, held still."
            else -> "The days sat a little apart."
        }
    }

    private fun notesClause(n: Int, notes: Int): String? = when {
        notes <= 0 -> null
        notes == n && n == 1 -> "you wrote a note that day"
        notes == n -> "you wrote on every day you shot"
        notes * 2 >= n && n >= 4 -> "you wrote through most of the month"
        else -> "you wrote on ${dayWord(notes)} of them"
    }

    private fun runClause(n: Int, run: Int): String? =
        if (run >= 5 && run < n) "the longest stretch was ${dayWord(run)}" else null

    private fun previousSentence(n: Int, previous: Int?): String? {
        if (previous == null || previous <= 0) return null
        return when {
            n > previous -> "A little more than the month before."
            n == previous && n >= 4 -> "About the same as the month before."
            else -> null
        }
    }

    private fun capitalize(value: String): String = value.replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(Locale.ENGLISH) else ch.toString()
    }
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
