package app.shutterup.capture

import android.Manifest
import app.shutterup.data.ai.FakePromptGenerator
import app.shutterup.data.notifications.NotificationHelper
import app.shutterup.di.SwitchingPromptGenerator
import app.shutterup.domain.ai.Availability
import app.shutterup.domain.ai.GeneratedMonthlyIssue
import app.shutterup.domain.ai.GeneratedPrompt
import app.shutterup.domain.ai.GeneratedSeries
import app.shutterup.domain.ai.GenerationRequest
import app.shutterup.domain.ai.MonthlyIssueRequest
import app.shutterup.domain.ai.PromptGenerator
import app.shutterup.domain.ai.PromptSource
import app.shutterup.domain.ai.Season
import app.shutterup.domain.capture.SkipDayUseCase
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.testutil.FakeDayPrompts
import app.shutterup.testutil.FakeGamification
import app.shutterup.testutil.FakePrefs
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SkipTodayActionTest {
    private val today = LocalDate.of(2026, 9, 19)
    private val zone = ZoneOffset.UTC
    private val clock = Clock.fixed(today.atTime(12, 0).toInstant(zone), zone)

    @Before
    fun grantNotifications() {
        Shadows.shadowOf(RuntimeEnvironment.getApplication())
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun successfulSkip_cancelsTodaysNotification() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val helper = NotificationHelper(context)
        val prompt = samplePrompt(today)
        helper.postPrompt(today, prompt, rerollAvailable = true)
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        assertTrue(Shadows.shadowOf(manager).getNotification(today.toEpochDay().toInt()) != null)
        val prompts = FakeDayPrompts(mutableListOf(prompt))
        val action = SkipTodayAction(
            skipDay = SkipDayUseCase(prompts, FakeGamification(), clock, zone),
            notifications = helper,
            clock = clock,
            zone = zone,
        )
        assertTrue(action())
        assertEquals(DayStatus.SKIPPED, prompts.getDay(today)?.status)
        assertNull(Shadows.shadowOf(manager).getNotification(today.toEpochDay().toInt()))
    }

    @Test
    fun skipWhenNotPending_doesNotCancel() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val helper = NotificationHelper(context)
        val prompt = samplePrompt(today).copy(status = DayStatus.COMPLETED)
        helper.postPrompt(today, prompt, rerollAvailable = false)
        val action = SkipTodayAction(
            skipDay = SkipDayUseCase(
                FakeDayPrompts(mutableListOf(prompt)),
                FakeGamification(),
                clock,
                zone,
            ),
            notifications = helper,
            clock = clock,
            zone = zone,
        )
        assertFalse(action())
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        assertTrue(Shadows.shadowOf(manager).getNotification(today.toEpochDay().toInt()) != null)
    }

    private fun samplePrompt(date: LocalDate) = DayPrompt(
        date = date,
        title = "Steam Maps",
        oneLiner = "Turn kitchen steam into contour lines of light.",
        details = "Wait for a kettle.",
        constraint = null,
        theme = "Steam",
        tips = emptyList(),
        source = PromptSourceRef.LIBRARY,
        libraryId = "lib-1",
        modelName = null,
        generatedAt = Instant.EPOCH,
        status = DayStatus.PENDING,
        frozen = false,
        rerollUsed = false,
    )
}

class SwitchingPromptGeneratorTest {
    private val request = GenerationRequest(
        date = LocalDate.of(2026, 9, 19),
        themeFocus = null,
        recentTitles = emptyList(),
        recentThemes = emptyList(),
        dayOfWeek = DayOfWeek.SATURDAY,
        season = Season.AUTUMN,
    )

    @Test
    fun defaultsToOnDevice() = runTest {
        val prefs = FakePrefs()
        val switcher = SwitchingPromptGenerator(
            onDevice = ScriptedGenerator("nano"),
            fake = FakePromptGenerator(),
            preferences = prefs,
        )
        assertEquals("nano", switcher.generate(request).getOrThrow().title)
    }

    @Test
    fun toggleSwapsAtRuntime() = runTest {
        val prefs = FakePrefs()
        val switcher = SwitchingPromptGenerator(
            onDevice = ScriptedGenerator("nano"),
            fake = FakePromptGenerator(),
            preferences = prefs,
        )
        prefs.setDebugUseFakeAi(true)
        assertTrue(switcher.generate(request).getOrThrow().title.startsWith("Test prompt"))
        assertEquals(Availability.AVAILABLE, switcher.availability())
        prefs.setDebugUseFakeAi(false)
        assertEquals("nano", switcher.generate(request).getOrThrow().title)
    }

    @Test
    fun missingFake_staysOnDeviceEvenWhenPrefIsOn() = runTest {
        val prefs = FakePrefs()
        prefs.setDebugUseFakeAi(true)
        val switcher = SwitchingPromptGenerator(
            onDevice = ScriptedGenerator("nano"),
            fake = null,
            preferences = prefs,
        )
        assertEquals("nano", switcher.generate(request).getOrThrow().title)
    }

    /**
     * The interface defaults for series and monthly generation return failure, so a
     * delegate that forgets to forward them silently strands Nano on the library path
     * instead of failing to compile (SPEC §7.1).
     */
    @Test
    fun forwardsSeriesToTheActiveGenerator() = runTest {
        val prefs = FakePrefs()
        val switcher = SwitchingPromptGenerator(
            onDevice = ScriptedGenerator("nano"),
            fake = FakePromptGenerator(),
            preferences = prefs,
        )
        assertEquals("nano series", switcher.generateSeries(request).getOrThrow().title)
    }

    @Test
    fun forwardsMonthlyIssueToTheActiveGenerator() = runTest {
        val prefs = FakePrefs()
        val switcher = SwitchingPromptGenerator(
            onDevice = ScriptedGenerator("nano"),
            fake = FakePromptGenerator(),
            preferences = prefs,
        )
        val issue = switcher.generateMonthlyIssue(monthlyRequest).getOrThrow()
        assertEquals("nano headline", issue.headline)
    }

    @Test
    fun holdEngine_pinsTheActiveGeneratorAcrossAToggle() = runTest {
        val prefs = FakePrefs()
        val onDevice = ScriptedGenerator("nano")
        val fake = ScriptedGenerator("fake")
        val switcher = SwitchingPromptGenerator(
            onDevice = onDevice,
            fake = fake,
            preferences = prefs,
        )
        switcher.holdEngine()
        prefs.setDebugUseFakeAi(true)
        assertEquals("nano", switcher.generate(request).getOrThrow().title)
        switcher.releaseEngine()
        assertEquals(1, onDevice.engineHolds)
        assertEquals(1, onDevice.engineReleases)
        assertEquals(0, fake.engineHolds)
        assertEquals("fake", switcher.generate(request).getOrThrow().title)
    }

    private val monthlyRequest = MonthlyIssueRequest(
        monthLabel = "September",
        completedCount = 24,
        titles = emptyList(),
        themeCounts = emptyList(),
        notes = emptyList(),
        longestRun = 11,
    )

    private class ScriptedGenerator(private val title: String) : PromptGenerator {
        var engineHolds: Int = 0
            private set
        var engineReleases: Int = 0
            private set

        override suspend fun availability() = Availability.AVAILABLE

        override suspend fun holdEngine() {
            engineHolds += 1
        }

        override suspend fun releaseEngine() {
            engineReleases += 1
        }

        override suspend fun generate(request: GenerationRequest) = Result.success(
            GeneratedPrompt(
                title = title,
                oneLiner = "one",
                details = "details",
                tips = emptyList(),
                constraint = null,
                theme = "Theme",
                source = PromptSource.ON_DEVICE_AI,
            ),
        )

        override suspend fun generateSeries(request: GenerationRequest) = Result.success(
            GeneratedSeries(
                title = "$title series",
                theme = "Theme",
                prompts = emptyList(),
            ),
        )

        override suspend fun generateMonthlyIssue(request: MonthlyIssueRequest) = Result.success(
            GeneratedMonthlyIssue(headline = "$title headline", body = "body"),
        )
    }
}
