package app.shutterup

import android.Manifest
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.then
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import app.shutterup.di.TestTimeModule
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.testutil.SeedData
import app.shutterup.testutil.initTestWorkManager
import app.shutterup.ui.navigation.ShutterUpNavGraph
import app.shutterup.ui.theme.ShutterUpTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AdaptiveLayoutTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var preferences: PreferencesRepository
    @Inject lateinit var prompts: DayPromptRepository
    @Inject lateinit var gamification: GamificationRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        initTestWorkManager(composeRule.activity.applicationContext)
        runBlocking { SeedData.onboarded(preferences, prompts, gamification) }
    }

    @Test
    fun compactShowsSinglePane() {
        setNavGraph(DpSize(400.dp, 900.dp))
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(SeedData.TODAY_TITLE).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(SeedData.TODAY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("Shoot").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertDoesNotExist()
        org.junit.Assert.assertEquals(
            1,
            composeRule.onAllNodesWithText(SeedData.TODAY_TITLE).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun expandedShowsListDetail() {
        setNavGraph(DpSize(1000.dp, 800.dp))
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(SeedData.TODAY_TITLE).fetchSemanticsNodes().size >= 2
        }
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        org.junit.Assert.assertTrue(
            "expanded Home is list-detail: title on the list and on the pane",
            composeRule.onAllNodesWithText(SeedData.TODAY_TITLE).fetchSemanticsNodes().size >= 2,
        )
    }

    private fun setNavGraph(size: DpSize) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(size) then
                    DeviceConfigurationOverride.WindowSize(size),
            ) {
                ShutterUpTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        ShutterUpNavGraph(
                            navController = rememberNavController(),
                            todayIso = TestTimeModule.TODAY.toString(),
                        )
                    }
                }
            }
        }
    }
}
