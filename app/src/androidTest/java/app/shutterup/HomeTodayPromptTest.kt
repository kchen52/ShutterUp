package app.shutterup

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.testutil.SeedData
import app.shutterup.testutil.initTestWorkManager
import app.shutterup.testutil.launchMainActivity
import app.shutterup.testutil.waitUntilText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeTodayPromptTest {

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
    fun showsTodayTitleOneLinerAndStreakRow() {
        composeRule.waitUntilText(SeedData.TODAY_TITLE)
        composeRule.onNodeWithText(SeedData.TODAY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(SeedData.TODAY_ONE_LINER).assertIsDisplayed()
        composeRule.onNode(
            hasContentDescription("3 days, 0 freezes, 0 of 19 days this month"),
        ).assertExists()
    }
}
