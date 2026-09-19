package app.shutterup

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import app.shutterup.di.TestTimeModule
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.testutil.SeedData
import app.shutterup.testutil.clickFirstText
import app.shutterup.testutil.initTestWorkManager
import app.shutterup.testutil.launchMainActivity
import app.shutterup.testutil.openPromptDetail
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
class SkipTest {

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
        initTestWorkManager(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        runBlocking { SeedData.onboarded(preferences, prompts, gamification) }
        scenario = launchMainActivity()
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun skipDialogExplainsStreakAndMarksSkipped() {
        composeRule.openPromptDetail()
        composeRule.waitUntilText("Skip")
        composeRule.clickFirstText("Skip")
        composeRule.waitUntilText("Skip today?")
        composeRule.onNodeWithText("streak", substring = true).assertIsDisplayed()
        composeRule.clickFirstText("Skip")
        composeRule.waitUntil(10_000) {
            runBlocking { prompts.getDay(TestTimeModule.TODAY)?.status == DayStatus.SKIPPED }
        }
        assertEquals(DayStatus.SKIPPED, runBlocking { prompts.getDay(TestTimeModule.TODAY) }?.status)
        val back = composeRule.onAllNodesWithContentDescription("Back")
        if (back.fetchSemanticsNodes().isNotEmpty()) {
            back.onFirst().performClick()
        }
        composeRule.waitUntilText("Skipped — see you tomorrow.")
    }
}
