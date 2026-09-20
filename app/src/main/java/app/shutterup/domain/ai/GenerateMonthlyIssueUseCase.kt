package app.shutterup.domain.ai

import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.MonthlyIssue
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.domain.monthly.CompletedDayForIssue
import app.shutterup.domain.monthly.MonthlyIssueCalendar
import app.shutterup.domain.monthly.MonthlyIssueCopy
import app.shutterup.domain.monthly.MonthlyIssueFallback
import app.shutterup.domain.monthly.MonthlyIssueSnapshot
import app.shutterup.domain.monthly.ThemeRanking
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.MonthlyIssueRepository
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

/**
 * Assembles a finished month into one issue (SPEC §7.9). Idempotent: a month
 * is written once. Zero completed days produce no issue. Copy is the
 * deterministic fallback composer. A future generator may be attempted when
 * wired as primary and available.
 */
open class GenerateMonthlyIssueUseCase @Inject constructor(
    @Named("primaryGenerator") private val primary: PromptGenerator,
    private val prompts: DayPromptRepository,
    private val entries: EntryRepository,
    private val issues: MonthlyIssueRepository,
    private val validator: MonthlyIssueValidator,
    private val clock: Clock,
    private val zone: ZoneId,
) {
    open suspend fun generateDue(): List<String> {
        val today = LocalDate.now(clock.withZone(zone))
        val completed = prompts.allDays().filter { isCompleted(it.status) }
        val earliest = completed.minOfOrNull { it.date }
        val notesByDate = notesByDate()
        val days = completed.map { day ->
            CompletedDayForIssue(
                date = day.date,
                title = day.title,
                theme = day.theme,
                note = notesByDate[day.date],
            )
        }
        val created = mutableListOf<String>()
        for (month in MonthlyIssueCalendar.dueMonths(today, earliest)) {
            val yearMonth = month.toString()
            if (issues.get(yearMonth) != null) continue
            val snapshot = MonthlyIssueSnapshot.of(
                month,
                days,
                previousCompletedCount = days.count { YearMonth.from(it.date) == month.minusMonths(1) },
            ) ?: continue
            val written = writeCopy(snapshot)
            val inserted = persist(snapshot, written)
            if (inserted) created += yearMonth
        }
        return created
    }

    private suspend fun writeCopy(snapshot: MonthlyIssueSnapshot): WrittenCopy {
        if (primary.availability() != Availability.AVAILABLE) {
            return fallbackCopy(snapshot)
        }
        primary.holdEngine()
        try {
            val request = requestFor(snapshot)
            repeat(ATTEMPTS) {
                val candidate = primary.generateMonthlyIssue(request).getOrNull() ?: return@repeat
                val copy = MonthlyIssueCopy(candidate.headline, candidate.body)
                if (validator.validate(
                        copy,
                        snapshot.notes,
                        ThemeRanking.loudest(snapshot.rankedThemes, 2),
                    ) is ValidationResult.Valid
                ) {
                    return WrittenCopy(
                        copy = copy,
                        source = PromptSourceRef.ON_DEVICE_AI,
                        modelName = candidate.modelName,
                    )
                }
            }
        } finally {
            primary.releaseEngine()
        }
        return fallbackCopy(snapshot)
    }

    private fun fallbackCopy(snapshot: MonthlyIssueSnapshot): WrittenCopy = WrittenCopy(
        copy = MonthlyIssueFallback.compose(snapshot),
        source = PromptSourceRef.LIBRARY,
        modelName = null,
    )

    private suspend fun persist(snapshot: MonthlyIssueSnapshot, written: WrittenCopy): Boolean {
        val (start, end) = MonthlyIssueCalendar.window(snapshot.yearMonth)
        val id = issues.insert(
            MonthlyIssue(
                yearMonth = snapshot.yearMonth.toString(),
                startDate = start,
                endDate = end,
                completedDayCount = snapshot.completedCount,
                headline = written.copy.headline,
                body = written.copy.body,
                dominantTheme = ThemeRanking.loudest(snapshot.rankedThemes, 1).firstOrNull().orEmpty()
                    .ifBlank { "Looking" },
                loudestThemes = ThemeRanking.loudest(snapshot.rankedThemes, 2),
                source = written.source,
                generatedAt = clock.instant(),
                dismissedFromFeed = false,
                modelName = written.modelName,
            ),
        )
        return id != -1L
    }

    private suspend fun notesByDate(): Map<LocalDate, String?> =
        entries.listAll()
            .groupBy { it.date }
            .mapValues { (_, rows) ->
                rows.mapNotNull { it.note?.takeIf { note -> note.isNotBlank() } }.firstOrNull()
            }

    private fun requestFor(snapshot: MonthlyIssueSnapshot): MonthlyIssueRequest {
        val label = snapshot.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) +
            " " + snapshot.yearMonth.year
        return MonthlyIssueRequest(
            monthLabel = label,
            completedCount = snapshot.completedCount,
            titles = snapshot.titles,
            themeCounts = snapshot.rankedThemes.map { it.theme to it.count },
            notes = snapshot.notes,
            longestRun = snapshot.longestRun,
        )
    }

    private data class WrittenCopy(
        val copy: MonthlyIssueCopy,
        val source: PromptSourceRef,
        val modelName: String?,
    )

    companion object {
        const val ATTEMPTS = 3
    }
}

private fun isCompleted(status: DayStatus): Boolean =
    status == DayStatus.COMPLETED || status == DayStatus.COMPLETED_NO_PHOTO
