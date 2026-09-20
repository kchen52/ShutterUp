package app.shutterup.domain.take

import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.MediaKind
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.domain.model.SupersededPrompt
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StartSecondTakeUseCaseTest {
    private val today = LocalDate.of(2026, 9, 20)
    private val past = LocalDate.of(2026, 6, 28)
    private val clock = Clock.fixed(Instant.parse("2026-09-20T12:00:00Z"), ZoneOffset.UTC)
    private val zone = ZoneOffset.UTC

    @Test
    fun todayPending_placesOnTodayAndCopiesThePrompt() = runBlocking {
        val prompts = MemoryPrompts().apply {
            put(completed(past))
            put(pending(today, title = "Buffer prompt", seriesId = 4, seriesIndex = 2))
        }
        val entries = MemoryEntries().apply { putPhoto(past) }
        val useCase = StartSecondTakeUseCase(prompts, entries, clock, zone)
        val preview = useCase.preview(past)!!
        assertEquals(today, preview.target)
        assertTrue(preview.landsToday)
        assertEquals("This becomes today's prompt.", preview.confirmationBody)

        val result = useCase(past) as StartSecondTakeResult.Placed
        assertEquals(today, result.date)
        val placed = prompts.getDay(today)!!
        assertEquals("Find the sky in a puddle", placed.title)
        assertEquals("Turn the world upside down.", placed.oneLiner)
        assertEquals("Look down.", placed.details)
        assertEquals("Reflections", placed.theme)
        assertEquals(listOf("Tap to focus."), placed.tips)
        assertEquals("Don't rotate.", placed.constraint)
        assertEquals(past, placed.repeatsDate)
        assertEquals(DayStatus.PENDING, placed.status)
        assertNull(placed.seriesId)
        assertNull(placed.seriesIndex)
        assertEquals("Buffer prompt", prompts.superseded.single().title)
    }

    @Test
    fun todayCompleted_placesOnTomorrow() = runBlocking {
        val tomorrow = today.plusDays(1)
        val prompts = MemoryPrompts().apply {
            put(completed(past))
            put(completed(today))
            put(pending(tomorrow, title = "Tomorrow buffer", seriesId = 9, seriesIndex = 3))
        }
        val entries = MemoryEntries().apply {
            putPhoto(past)
            putPhoto(today)
        }
        val useCase = StartSecondTakeUseCase(prompts, entries, clock, zone)
        val preview = useCase.preview(past)!!
        assertEquals(tomorrow, preview.target)
        assertEquals("This becomes tomorrow's prompt.", preview.confirmationBody)
        val result = useCase(past) as StartSecondTakeResult.Placed
        assertEquals(tomorrow, result.date)
        val placed = prompts.getDay(tomorrow)!!
        assertNull(placed.seriesId)
        assertNull(placed.seriesIndex)
        assertEquals(past, placed.repeatsDate)
        assertEquals(DayStatus.COMPLETED, prompts.getDay(today)?.status)
        assertEquals("Tomorrow buffer", prompts.superseded.single().title)
    }

    @Test
    fun todaySkipped_placesOnTomorrow() = runBlocking {
        val prompts = MemoryPrompts().apply {
            put(completed(past))
            put(pending(today).copy(status = DayStatus.SKIPPED))
        }
        val entries = MemoryEntries().apply { putPhoto(past) }
        val result = StartSecondTakeUseCase(prompts, entries, clock, zone)(past)
            as StartSecondTakeResult.Placed
        assertEquals(today.plusDays(1), result.date)
    }

    @Test
    fun todayPaused_placesOnTomorrow() = runBlocking {
        val prompts = MemoryPrompts().apply {
            put(completed(past))
            put(pending(today).copy(status = DayStatus.PAUSED))
        }
        val entries = MemoryEntries().apply { putPhoto(past) }
        val result = StartSecondTakeUseCase(prompts, entries, clock, zone)(past)
            as StartSecondTakeResult.Placed
        assertEquals(today.plusDays(1), result.date)
    }

    @Test
    fun seriesDetachment_clearsOnlyTheTargetDay() = runBlocking {
        val d1 = today
        val d2 = today.plusDays(1)
        val d3 = today.plusDays(2)
        val prompts = MemoryPrompts().apply {
            put(completed(past))
            put(pending(d1, title = "S1", seriesId = 4, seriesIndex = 1))
            put(pending(d2, title = "S2", seriesId = 4, seriesIndex = 2))
            put(pending(d3, title = "S3", seriesId = 4, seriesIndex = 3))
        }
        val entries = MemoryEntries().apply { putPhoto(past) }
        StartSecondTakeUseCase(prompts, entries, clock, zone)(past)
        assertNull(prompts.getDay(d1)?.seriesId)
        assertNull(prompts.getDay(d1)?.seriesIndex)
        assertEquals(4L, prompts.getDay(d2)?.seriesId)
        assertEquals(2, prompts.getDay(d2)?.seriesIndex)
        assertEquals(4L, prompts.getDay(d3)?.seriesId)
        assertEquals(listOf(d2, d3).map { it to 4L }, prompts.daysInSeries(4).map { it.date to it.seriesId })
    }

    @Test
    fun repeatingASecondTake_pointsAtTheOriginal() = runBlocking {
        val second = LocalDate.of(2026, 8, 1)
        val prompts = MemoryPrompts().apply {
            put(completed(past))
            put(completed(second, repeatsDate = past))
            put(pending(today))
        }
        val entries = MemoryEntries().apply {
            putPhoto(past)
            putPhoto(second)
        }
        StartSecondTakeUseCase(prompts, entries, clock, zone)(second)
        assertEquals(past, prompts.getDay(today)?.repeatsDate)
    }

    @Test
    fun neverOverwritesADayAlreadyActedOn() = runBlocking {
        val prompts = MemoryPrompts().apply {
            put(completed(past))
            put(completed(today))
            put(completed(today.plusDays(1)))
        }
        val entries = MemoryEntries().apply {
            putPhoto(past)
            putPhoto(today)
            putPhoto(today.plusDays(1))
        }
        val result = StartSecondTakeUseCase(prompts, entries, clock, zone)(past)
        assertEquals(StartSecondTakeResult.TargetTaken, result)
        assertEquals("Find the sky in a puddle", prompts.getDay(today.plusDays(1))?.title)
    }

    @Test
    fun notRepeatableWithoutAPhotograph() = runBlocking {
        val prompts = MemoryPrompts().apply { put(completed(past)) }
        val result = StartSecondTakeUseCase(prompts, MemoryEntries(), clock, zone)(past)
        assertEquals(StartSecondTakeResult.NotRepeatable, result)
    }

    private fun completed(date: LocalDate, repeatsDate: LocalDate? = null) = DayPrompt(
        date = date,
        title = "Find the sky in a puddle",
        oneLiner = "Turn the world upside down.",
        details = "Look down.",
        constraint = "Don't rotate.",
        theme = "Reflections",
        tips = listOf("Tap to focus."),
        source = PromptSourceRef.LIBRARY,
        libraryId = "lib-1",
        modelName = null,
        generatedAt = Instant.parse("2026-06-28T08:00:00Z"),
        status = DayStatus.COMPLETED,
        frozen = false,
        rerollUsed = false,
        repeatsDate = repeatsDate,
    )

    private fun pending(
        date: LocalDate,
        title: String = "Pending",
        seriesId: Long? = null,
        seriesIndex: Int? = null,
    ) = completed(date).copy(
        title = title,
        status = DayStatus.PENDING,
        seriesId = seriesId,
        seriesIndex = seriesIndex,
        repeatsDate = null,
    )
}

private class MemoryPrompts : DayPromptRepository {
    private val days = linkedMapOf<LocalDate, DayPrompt>()
    val superseded = mutableListOf<SupersededPrompt>()

    fun put(prompt: DayPrompt) {
        days[prompt.date] = prompt
    }

    override fun observeDay(date: LocalDate) = flowOf(days[date])
    override fun observeDays(start: LocalDate, endInclusive: LocalDate) =
        flowOf(days.values.filter { it.date in start..endInclusive })
    override suspend fun getDay(date: LocalDate) = days[date]
    override suspend fun upsert(prompt: DayPrompt) {
        days[prompt.date] = prompt
    }
    override suspend fun recordSuperseded(prompt: SupersededPrompt) {
        superseded += prompt
    }
    override suspend fun recentTitles(limit: Int) = emptyList<String>()
    override suspend fun recentThemes(limit: Int) = emptyList<String>()
    override suspend fun recentDays(limit: Int) = days.values.toList()
    override suspend fun allDays() = days.values.sortedBy { it.date }
    override suspend fun deleteAfter(date: LocalDate) = Unit
    override fun observeDaysInSeries(seriesId: Long) =
        flowOf(days.values.filter { it.seriesId == seriesId }.sortedBy { it.date })
    override suspend fun daysInSeries(seriesId: Long) =
        days.values.filter { it.seriesId == seriesId }.sortedBy { it.date }
}

private class MemoryEntries : EntryRepository {
    private val rows = mutableListOf<Entry>()
    fun putPhoto(date: LocalDate) {
        rows += Entry(
            id = rows.size + 1L,
            date = date,
            mediaUri = "content://media/$date",
            thumbPath = "/thumbs/$date.jpg",
            capturedAt = Instant.parse("2026-06-28T10:00:00Z"),
            width = 1200,
            height = 1600,
            note = null,
            importedFromGallery = false,
            createdAt = Instant.parse("2026-06-28T10:00:00Z"),
            mediaKind = MediaKind.PHOTO,
        )
    }
    override fun observeEntry(date: LocalDate) = flowOf(rows.lastOrNull { it.date == date })
    override fun observeEntries(date: LocalDate) = flowOf(rows.filter { it.date == date })
    override fun observeRecentEntries(limit: Int) = flowOf(rows.take(limit))
    override fun observeEntriesByTheme(theme: String) = flowOf(emptyList<Entry>())
    override suspend fun upsert(entry: Entry) {
        rows += entry
    }
    override suspend fun delete(date: LocalDate) {
        rows.removeAll { it.date == date }
    }
    override suspend fun count() = rows.size
    override suspend fun countForDate(date: LocalDate) = rows.count { it.date == date }
    override suspend fun listAll() = rows.toList()
}
