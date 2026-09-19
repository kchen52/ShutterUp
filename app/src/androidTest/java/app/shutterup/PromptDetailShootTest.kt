package app.shutterup

import android.Manifest
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import app.shutterup.di.TestTimeModule
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.testutil.CameraIntents
import app.shutterup.testutil.SeedData
import app.shutterup.testutil.clickShoot
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
class PromptDetailShootTest {

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
        initTestWorkManager(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        runBlocking { SeedData.onboarded(preferences, prompts, gamification) }
        CameraIntents.stubSuccessfulCapture()
        scenario = launchMainActivity()
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun shootFiresCaptureIntentAndCompletesDay() {
        composeRule.waitUntilText(SeedData.TODAY_TITLE)
        composeRule.clickShoot()
        composeRule.waitUntil(15_000) {
            runBlocking { prompts.getDay(TestTimeModule.TODAY)?.status == DayStatus.COMPLETED } ||
                composeRule.onAllNodesWithText("First light.").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Done").fetchSemanticsNodes().isNotEmpty()
        }
        CameraIntents.assertCaptureLaunched()
        val day = runBlocking { prompts.getDay(TestTimeModule.TODAY) }
        assertEquals(DayStatus.COMPLETED, day?.status)
    }
}
