package app.shutterup

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.testutil.SeedData
import app.shutterup.testutil.clickFirstText
import app.shutterup.testutil.initTestWorkManager
import app.shutterup.testutil.launchMainActivity
import app.shutterup.testutil.waitUntilText
import app.shutterup.ui.settings.SettingsCopy
import app.shutterup.work.NotificationScheduler
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 2)
    val composeRule = createEmptyComposeRule()

    @Inject lateinit var preferences: PreferencesRepository
    @Inject lateinit var prompts: DayPromptRepository
    @Inject lateinit var gamification: GamificationRepository

    private var scenario: androidx.test.core.app.ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        hiltRule.inject()
        initTestWorkManager(ApplicationProvider.getApplicationContext())
        runBlocking { SeedData.onboarded(preferences, prompts, gamification) }
        scenario = launchMainActivity()
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun changingNotifyTimePersistsAndReschedules() {
        composeRule.waitUntilText(SeedData.TODAY_TITLE)
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.waitUntilText(SettingsCopy.NOTIFICATION_TIME)
        composeRule.onNodeWithText(SettingsCopy.NOTIFICATION_TIME).performClick()
        composeRule.waitUntilText("Save")
        setPickerToFourPm()
        composeRule.clickFirstText("Save")
        composeRule.waitUntil(10_000) {
            runBlocking { preferences.observeNotifyTime().first() == LocalTime.of(16, 0) }
        }
        assertEquals(LocalTime.of(16, 0), runBlocking { preferences.observeNotifyTime().first() })

        val context = ApplicationProvider.getApplicationContext<Context>()
        val driver = WorkManagerTestInitHelper.getTestDriver(context)
        assertNotNull("WorkManager TestDriver should be installed", driver)
        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(NotificationScheduler.DAILY_WORK_NAME)
            .get()
        assertTrue(infos.isNotEmpty())
        assertTrue(infos.any { it.state == WorkInfo.State.ENQUEUED })
        val next = infos.first().nextScheduleTimeMillis
        assertTrue(
            "16:00 is one hour after the fixed 15:00 clock, so the worker delay is ~1h; next=$next",
            next >= System.currentTimeMillis() + 45L * 60L * 1000L,
        )
    }

    private fun setPickerToFourPm() {
        val switchToInput = composeRule.onAllNodesWithContentDescription(
            "Switch to text input mode",
            substring = true,
        )
        if (switchToInput.fetchSemanticsNodes().isNotEmpty()) {
            switchToInput.onFirst().performClick()
        }
        val pm = composeRule.onAllNodesWithText("PM")
        if (pm.fetchSemanticsNodes().isNotEmpty()) {
            pm.onFirst().performClick()
        }
        val four = composeRule.onAllNodesWithContentDescription("4 o'clock")
        if (four.fetchSemanticsNodes().isNotEmpty()) {
            four.onFirst().performClick()
        } else {
            composeRule.onNodeWithText("4").performClick()
        }
    }
}
