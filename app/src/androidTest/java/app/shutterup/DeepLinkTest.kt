package app.shutterup

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import app.shutterup.di.TestTimeModule
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.navigation.DeepLinks
import app.shutterup.testutil.CameraIntents
import app.shutterup.testutil.SeedData
import app.shutterup.testutil.initTestWorkManager
import app.shutterup.testutil.launchMainActivity
import app.shutterup.testutil.waitUntilText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DeepLinkTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 2)
    val intentsRule = IntentsRule()

    @get:Rule(order = 3)
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
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun dayLinkOpensPromptDetail() {
        scenario = launchMainActivity(dayIntent(autoLaunch = false))
        composeRule.waitUntilText(SeedData.TODAY_TITLE)
        composeRule.waitUntilText("Reroll")
        composeRule.waitUntilText("Shoot")
    }

    @Test
    fun autoLaunchCameraFiresCaptureIntent() {
        CameraIntents.stubSuccessfulCapture()
        scenario = launchMainActivity(dayIntent(autoLaunch = true))
        composeRule.waitUntil(15_000) {
            runBlocking { prompts.getDay(TestTimeModule.TODAY)?.status == DayStatus.COMPLETED } ||
                composeRule.onAllNodesWithText("First light.").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Done").fetchSemanticsNodes().isNotEmpty()
        }
        CameraIntents.assertCaptureLaunched()
        assertEquals(
            DayStatus.COMPLETED,
            runBlocking { prompts.getDay(TestTimeModule.TODAY) }?.status,
        )
    }

    private fun dayIntent(autoLaunch: Boolean): Intent {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return Intent(Intent.ACTION_VIEW, Uri.parse(DeepLinks.day(TestTimeModule.TODAY.toString(), autoLaunch))).apply {
            setClass(context, MainActivity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
