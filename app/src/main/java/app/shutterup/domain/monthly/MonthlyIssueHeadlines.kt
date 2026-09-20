package app.shutterup.domain.monthly

import java.time.YearMonth
import java.util.Locale

/**
 * Hand-written month headlines keyed to the 27 library themes, two each so
 * consecutive months of the same theme do not repeat. Quiet, concrete,
 * sentence case, full stop. Unknown Nano themes fall back to the theme
 * itself as a bare phrase.
 */
object MonthlyIssueHeadlines {
    fun phraseFor(theme: String, yearMonth: YearMonth): String =
        phraseFor(theme, variantIndex(yearMonth))

    fun phraseFor(theme: String, variantIndex: Int): String {
        val variants = BANK[normalize(theme)]
        if (variants != null && variants.isNotEmpty()) {
            val index = variantIndex.mod(variants.size)
            return variants[index]
        }
        return asHeadline(theme)
    }

    fun variantIndex(yearMonth: YearMonth): Int =
        (yearMonth.year + yearMonth.monthValue).mod(2)

    internal val BANK: Map<String, List<String>> = mapOf(
        normalize("Black and White") to listOf("Grey on grey.", "Ink and paper."),
        normalize("Color Pop") to listOf("One loud thing.", "One bright note."),
        normalize("Cozy Corners") to listOf("Close to the wall.", "A small room."),
        normalize("From Below") to listOf("From the floor.", "Underneath."),
        normalize("Geometry Hunt") to listOf("Straight edges.", "Corners and lines."),
        normalize("Green Neighbors") to listOf("A leaf at home.", "Something growing."),
        normalize("Hands at Work") to listOf("Your own hands.", "At the sink."),
        normalize("Kitchen Stories") to listOf("By the kettle.", "On the counter."),
        normalize("Letters and Numbers") to listOf("Type on a door.", "A number, close."),
        normalize("Looking Up") to listOf("Ceiling and sky.", "Above your head."),
        normalize("Morning Light") to listOf("Early and low.", "First light in."),
        normalize("Morning Rituals") to listOf("The same kettle.", "Before you leave."),
        normalize("Motion Trails") to listOf("Something passing.", "A thing in motion."),
        normalize("My Daily Carry") to listOf("What you brought.", "In the pocket."),
        normalize("Negative Space") to listOf("Mostly air.", "What you left out."),
        normalize("Night Lights") to listOf("After dark.", "Windows still on."),
        normalize("Opposites") to listOf("Two of a kind.", "Side by side."),
        normalize("Overcast Mood") to listOf("A grey week.", "Soft and even."),
        normalize("Quiet Streets") to listOf("Almost empty.", "The empty pavement."),
        normalize("Reflections") to listOf("Second skies.", "Glass looking back."),
        normalize("Shadows") to listOf("Where light stops.", "A long afternoon."),
        normalize("Sky Diary") to listOf("A little weather.", "That day's sky."),
        normalize("Still Life") to listOf("Things at rest.", "On the table."),
        normalize("Textures Up Close") to listOf("Very close in.", "Grain and weave."),
        normalize("Tiny Worlds") to listOf("A smaller scale.", "Down at your feet."),
        normalize("Wild Patterns") to listOf("Repeat, then not.", "The same, again."),
        normalize("Windows and Doors") to listOf("Through the glass.", "An open frame."),
    )

    private fun asHeadline(theme: String): String {
        val trimmed = theme.trim().trimEnd('.').trim()
        if (trimmed.isEmpty()) return "A month of looking."
        val titled = trimmed.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.ENGLISH) else ch.toString()
        }
        val candidate = "$titled."
        return if (candidate.length <= 30) candidate else "A month of looking."
    }

    private fun normalize(theme: String): String = theme.trim().lowercase(Locale.ENGLISH)
}
