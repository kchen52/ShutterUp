package app.shutterup

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.testutil.completeOnboarding
import app.shutterup.testutil.initTestWorkManager
import app.shutterup.testutil.launchMainActivity
import app.shutterup.testutil.waitUntilText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 2)
    val composeRule = createEmptyComposeRule()

    @Inject
    lateinit var prompts: DayPromptRepository

    private var scenario: androidx.test.core.app.ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        hiltRule.inject()
        initTestWorkManager(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        scenario = launchMainActivity()
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun completesOnboardingAndLandsOnHomeWithPrompt() {
        composeRule.completeOnboarding()
        composeRule.waitUntilText("Test prompt", substring = true)
        composeRule.onNodeWithText("ShutterUp").assertIsDisplayed()
        composeRule.onNodeWithText("Test prompt", substring = true).assertIsDisplayed()
        val stored = runBlocking { prompts.getDay(app.shutterup.di.TestTimeModule.TODAY) }
        assertNotNull(stored)
    }
}
