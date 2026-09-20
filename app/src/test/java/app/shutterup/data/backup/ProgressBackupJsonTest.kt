package app.shutterup.data.backup

import app.shutterup.data.local.AchievementEntity
import app.shutterup.data.local.DayPromptEntity
import app.shutterup.data.local.EntryEntity
import app.shutterup.data.local.LibraryUsageEntity
import app.shutterup.data.local.MonthlyIssueEntity
import app.shutterup.data.local.SeriesEntity
import app.shutterup.data.local.StreakStateEntity
import app.shutterup.data.local.SupersededPromptEntity
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.MediaKind
import app.shutterup.domain.model.PromptSourceRef
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressBackupJsonTest {
    @Test
    fun roundTripPreservesProgress() {
        val restored = ProgressBackupJson.decode(ProgressBackupJson.encode(sampleBackup()))
        assertEquals(sampleBackup(), restored)
    }

    @Test
    fun missingOptionalFieldsStayNull() {
        val json = """
            {
              "format": 1,
              "createdAt": "2026-09-20T16:00:00Z",
              "prefs": { "notifyHour": 8, "notifyMinute": 30 }
            }
        """.trimIndent()
        val backup = ProgressBackupJson.decode(json)
        assertEquals(8, backup.prefs.notifyHour)
        assertEquals(30, backup.prefs.notifyMinute)
        assertNull(backup.prefs.themeFocus)
        assertEquals(emptyList<DayPromptEntity>(), backup.prompts)
        assertNull(backup.streak)
    }

    @Test(expected = ProgressBackupException::class)
    fun unknownFormatIsRejected() {
        ProgressBackupJson.decode("""{"format":99,"createdAt":"2026-09-20T16:00:00Z"}""")
    }

    @Test(expected = ProgressBackupException::class)
    fun garbageIsRejected() {
        ProgressBackupJson.decode("not json")
    }

    @Test
    fun fileNameUsesIsoDate() {
        assertEquals(
            "ShutterUp-backup-2026-09-20.zip",
            ProgressBackupFormat.fileName(LocalDate.of(2026, 9, 20)),
        )
    }
}

private fun sampleBackup(): ProgressBackup {
    val date = LocalDate.of(2026, 9, 18)
    return ProgressBackup(
        format = 1,
        createdAt = Instant.parse("2026-09-20T16:00:00Z"),
        prefs = PrefsBackup(
            notifyHour = 7,
            notifyMinute = 15,
            preciseTiming = true,
            themeFocus = "windows",
            seriesEnabled = true,
            paused = false,
            onboardingComplete = true,
            debugUseFakeAi = false,
            lastNotifiedDate = "2026-09-18",
            coarseCityId = "sydney",
        ),
        prompts = listOf(
            DayPromptEntity(
                date = date,
                title = "Find the sky in a puddle",
                oneLiner = "Look down.",
                details = "Any reflective surface.",
                constraint = "Don't rotate.",
                theme = "Reflections",
                tips = listOf("Wait for shade"),
                source = PromptSourceRef.LIBRARY,
                libraryId = "lib-1",
                modelName = null,
                generatedAt = Instant.parse("2026-09-18T08:00:00Z"),
                status = DayStatus.COMPLETED,
                frozen = false,
                rerollUsed = true,
                seriesId = 4,
                seriesIndex = 2,
                repeatsDate = null,
            ),
        ),
        superseded = listOf(
            SupersededPromptEntity(
                id = 3,
                date = date,
                title = "Old title",
                theme = "Light",
                generatedAt = Instant.parse("2026-09-18T07:00:00Z"),
            ),
        ),
        entries = listOf(
            EntryBackup(
                entity = EntryEntity(
                    id = 9,
                    date = date,
                    mediaUri = "content://media/external/images/media/12",
                    thumbPath = "/data/thumbs/2026-09-18.jpg",
                    capturedAt = Instant.parse("2026-09-18T12:00:00Z"),
                    width = 4000,
                    height = 3000,
                    note = "wet pavement",
                    importedFromGallery = false,
                    createdAt = Instant.parse("2026-09-18T12:01:00Z"),
                    mediaKind = MediaKind.PHOTO,
                ),
                displayName = "2026-09-18_reflections.jpg",
                thumbZip = "thumbs/2026-09-18_9.jpg",
                photoZip = "photos/2026-09-18_9.jpg",
            ),
        ),
        achievements = listOf(
            AchievementEntity(
                id = "first_light",
                unlockedAt = Instant.parse("2026-09-18T12:02:00Z"),
                unlockedOnDate = date,
            ),
        ),
        streak = StreakStateEntity(
            id = 0,
            current = 4,
            longest = 12,
            freezes = 1,
            lastProcessedDate = date,
        ),
        libraryUsage = listOf(LibraryUsageEntity("lib-1", date)),
        series = listOf(
            SeriesEntity(
                id = 4,
                title = "A Week of Hands",
                startDate = date.minusDays(1),
                endDate = date.plusDays(5),
                theme = "Hands",
                source = PromptSourceRef.LIBRARY,
            ),
        ),
        monthlyIssues = listOf(
            MonthlyIssueEntity(
                id = 2,
                yearMonth = "2026-08",
                startDate = LocalDate.of(2026, 8, 1),
                endDate = LocalDate.of(2026, 8, 31),
                completedDayCount = 11,
                headline = "Grey week",
                body = "A quiet month.",
                dominantTheme = "Reflections",
                loudestThemes = listOf("Reflections", "Looking up"),
                source = PromptSourceRef.LIBRARY,
                generatedAt = Instant.parse("2026-09-01T08:00:00Z"),
                dismissedFromFeed = true,
                modelName = null,
            ),
        ),
    )
}
