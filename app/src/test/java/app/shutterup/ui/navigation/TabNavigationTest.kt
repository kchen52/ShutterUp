package app.shutterup.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TabNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun returningToTodayAfterSettingsShowsHome() {
        val nav = setUpTabs()

        composeRule.runOnIdle { nav.navigateSettings() }
        composeRule.onNodeWithText("settings-screen").assertIsDisplayed()

        composeRule.runOnIdle { nav.navigateTab(ShutterUpDestinations.CALENDAR) }
        composeRule.onNodeWithText("calendar-screen").assertIsDisplayed()

        composeRule.runOnIdle { nav.navigateTab(ShutterUpDestinations.HOME) }
        composeRule.onNodeWithText("home-screen").assertIsDisplayed()
        composeRule.onNodeWithText("settings-screen").assertDoesNotExist()
        assertEquals(ShutterUpDestinations.HOME, nav.currentDestination?.route)
    }

    @Test
    fun clickingTodayWhileOnSettingsShowsHome() {
        val nav = setUpTabs()

        composeRule.runOnIdle { nav.navigateSettings() }
        composeRule.onNodeWithText("settings-screen").assertIsDisplayed()

        composeRule.runOnIdle { nav.navigateTab(ShutterUpDestinations.HOME) }
        composeRule.onNodeWithText("home-screen").assertIsDisplayed()
        composeRule.onNodeWithText("settings-screen").assertDoesNotExist()
        assertEquals(ShutterUpDestinations.HOME, nav.currentDestination?.route)
    }

    @Test
    fun calendarStateStillRestoresAfterVisitingToday() {
        val nav = setUpTabs()

        composeRule.runOnIdle { nav.navigateTab(ShutterUpDestinations.CALENDAR) }
        composeRule.onNodeWithText("calendar-screen").assertIsDisplayed()

        composeRule.runOnIdle { nav.navigateTab(ShutterUpDestinations.FEED) }
        composeRule.onNodeWithText("feed-screen").assertIsDisplayed()

        composeRule.runOnIdle { nav.navigateTab(ShutterUpDestinations.CALENDAR) }
        composeRule.onNodeWithText("calendar-screen").assertIsDisplayed()
        assertEquals(ShutterUpDestinations.CALENDAR, nav.currentDestination?.route)
    }

    private fun setUpTabs(): NavHostController {
        lateinit var nav: NavHostController
        composeRule.setContent {
            nav = rememberNavController()
            NavHost(navController = nav, startDestination = ShutterUpDestinations.HOME) {
                composable(ShutterUpDestinations.HOME) { Text("home-screen") }
                composable(ShutterUpDestinations.CALENDAR) { Text("calendar-screen") }
                composable(ShutterUpDestinations.FEED) { Text("feed-screen") }
                composable(ShutterUpDestinations.SETTINGS) { Text("settings-screen") }
            }
        }
        return nav
    }
}
