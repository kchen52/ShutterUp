package app.shutterup.domain.take

import app.shutterup.domain.model.DayPrompt
import java.time.LocalDate

/**
 * Every take of one original, oldest first. The Day screen shows the viewed
 * take against the one before it; remaining dates are quiet links.
 */
data class TakeChain(
    val original: LocalDate,
    val dates: List<LocalDate>,
    val viewed: LocalDate,
) {
    val previous: LocalDate?
        get() {
            val index = dates.indexOf(viewed)
            return if (index > 0) dates[index - 1] else null
        }

    val laterFromOriginal: List<LocalDate>
        get() = dates.filter { it.isAfter(original) }

    /** Dates in the chain that are not the current diptych pair. */
    val rest: List<LocalDate>
        get() = dates.filter { it != viewed && it != previous }

    val isRepeat: Boolean
        get() = viewed != original && viewed in dates

    val showDiptych: Boolean
        get() = isRepeat && previous != null

    val size: Int get() = dates.size

    companion object {
        fun build(days: List<DayPrompt>, viewed: LocalDate): TakeChain {
            val byDate = days.associateBy { it.date }
            val viewedPrompt = byDate[viewed]
            val original = viewedPrompt?.repeatsDate
                ?: viewedPrompt?.date
                ?: viewed
            val members = days.filter { prompt ->
                prompt.date == original || prompt.repeatsDate == original
            }.map { it.date }.distinct().sorted()
            val dates = if (viewed in members) members else (members + viewed).sorted()
            return TakeChain(original = original, dates = dates, viewed = viewed)
        }
    }
}

/** Crop both frames to 1:1 when orientations differ; otherwise keep native. */
enum class DiptychCrop {
    SQUARE,
    NATIVE,
}

fun diptychCrop(widthA: Int, heightA: Int, widthB: Int, heightB: Int): DiptychCrop {
    if (widthA <= 0 || heightA <= 0 || widthB <= 0 || heightB <= 0) {
        return DiptychCrop.SQUARE
    }
    val portraitA = heightA >= widthA
    val portraitB = heightB >= widthB
    return if (portraitA == portraitB) DiptychCrop.NATIVE else DiptychCrop.SQUARE
}
