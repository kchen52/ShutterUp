package app.shutterup.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import app.shutterup.ui.adaptive.ShutterUpAdaptiveScaffold
import app.shutterup.ui.adaptive.ShutterUpDestination
import app.shutterup.ui.adaptive.ShutterUpTabCallbacks
import app.shutterup.ui.badges.BadgesRoute
import app.shutterup.ui.calendar.CalendarRoute
import app.shutterup.ui.completion.CompletionRoute
import app.shutterup.ui.day.DayRoute
import app.shutterup.ui.detail.PromptDetailRoute
import app.shutterup.ui.feed.FeedRoute
import app.shutterup.ui.home.HomeRoute
import app.shutterup.ui.settings.SettingsRoute
import app.shutterup.ui.themes.ThemesRoute

object ShutterUpDestinations {
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val FEED = "feed"
    const val THEMES = "themes"
    const val BADGES = "badges"
    const val SETTINGS = "settings"
    const val DAY = "day/{dateIso}"
    const val DETAIL = "detail/{dateIso}?autoLaunchCamera={autoLaunchCamera}&reroll={reroll}"
    const val COMPLETION = "completion/{dateIso}?newBadges={newBadges}&freezeEarned={freezeEarned}&previousStreak={previousStreak}"

    fun day(dateIso: String): String = "day/$dateIso"

    fun detail(
        dateIso: String,
        autoLaunchCamera: Boolean = false,
        reroll: Boolean = false,
    ): String = "detail/$dateIso?autoLaunchCamera=$autoLaunchCamera&reroll=$reroll"

    fun completion(
        dateIso: String,
        newBadges: String = "",
        freezeEarned: Boolean = false,
        previousStreak: Int = 0,
    ): String =
        "completion/$dateIso?newBadges=$newBadges&freezeEarned=$freezeEarned&previousStreak=$previousStreak"
}

/**
 * App navigation: Home, Calendar, Day, Feed, Themes, Badges, Settings,
 * Prompt Detail, Completion. Adaptive chrome wraps tab destinations.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun ShutterUpNavGraph(
    navController: NavHostController,
    todayIso: String,
    modifier: Modifier = Modifier,
) {
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val selected = tabForRoute(route)
    val hideChrome = route.startsWith("detail") ||
        route.startsWith("completion") ||
        route.startsWith("day")
    ShutterUpAdaptiveScaffold(
        selected = selected,
        callbacks = ShutterUpTabCallbacks(
            onHome = { navController.navigateTab(ShutterUpDestinations.HOME) },
            onCalendar = { navController.navigateTab(ShutterUpDestinations.CALENDAR) },
            onFeed = { navController.navigateTab(ShutterUpDestinations.FEED) },
            onBadges = { navController.navigateTab(ShutterUpDestinations.BADGES) },
            onSettings = { navController.navigateTab(ShutterUpDestinations.SETTINGS) },
        ),
        layoutType = if (hideChrome) NavigationSuiteType.None else null,
    ) {
        NavHost(
            navController = navController,
            startDestination = ShutterUpDestinations.HOME,
            modifier = modifier,
        ) {
            composable(ShutterUpDestinations.HOME) {
                HomeRoute(
                    onOpenDetail = { dateIso, auto ->
                        navController.navigate(ShutterUpDestinations.detail(dateIso, auto))
                    },
                    onOpenDay = { dateIso ->
                        navController.navigate(ShutterUpDestinations.day(dateIso))
                    },
                    onOpenCalendar = {
                        navController.navigateTab(ShutterUpDestinations.CALENDAR)
                    },
                    onOpenSettings = {
                        navController.navigateTab(ShutterUpDestinations.SETTINGS)
                    },
                )
            }
            composable(ShutterUpDestinations.CALENDAR) {
                CalendarRoute(
                    onOpenDay = { dateIso ->
                        navController.navigate(ShutterUpDestinations.day(dateIso))
                    },
                )
            }
            composable(ShutterUpDestinations.FEED) {
                FeedRoute(
                    onOpenDay = { dateIso ->
                        navController.navigate(ShutterUpDestinations.day(dateIso))
                    },
                    onOpenThemes = {
                        navController.navigate(ShutterUpDestinations.THEMES)
                    },
                )
            }
            composable(ShutterUpDestinations.THEMES) {
                ThemesRoute(
                    onOpenDay = { dateIso ->
                        navController.navigate(ShutterUpDestinations.day(dateIso))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(ShutterUpDestinations.BADGES) {
                BadgesRoute()
            }
            composable(ShutterUpDestinations.SETTINGS) {
                SettingsRoute()
            }
            composable(
                route = ShutterUpDestinations.DAY,
                arguments = listOf(
                    navArgument("dateIso") { type = NavType.StringType },
                ),
            ) {
                DayRoute(
                    onBack = { navController.popBackStack() },
                    onOpenDetail = { dateIso, auto ->
                        navController.navigate(ShutterUpDestinations.detail(dateIso, auto))
                    },
                    onOpenCompletion = { dateIso ->
                        navController.navigate(ShutterUpDestinations.completion(dateIso))
                    },
                )
            }
            composable(
                route = ShutterUpDestinations.DETAIL,
                arguments = listOf(
                    navArgument("dateIso") { type = NavType.StringType },
                    navArgument("autoLaunchCamera") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("reroll") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) {
                PromptDetailRoute(
                    onBack = { navController.popBackStack() },
                    onOpenCompletion = { nav ->
                        navController.navigate(
                            ShutterUpDestinations.completion(
                                dateIso = nav.dateIso,
                                newBadges = nav.newBadges,
                                freezeEarned = nav.freezeEarned,
                                previousStreak = nav.previousStreak,
                            ),
                        )
                    },
                )
            }
            composable(
                route = ShutterUpDestinations.COMPLETION,
                arguments = listOf(
                    navArgument("dateIso") { type = NavType.StringType },
                    navArgument("newBadges") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("freezeEarned") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("previousStreak") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                ),
            ) { entry ->
                val dateIso = entry.arguments?.getString("dateIso").orEmpty()
                CompletionRoute(
                    onStay = {
                        navController.popBackStack(ShutterUpDestinations.HOME, inclusive = false)
                    },
                    onRetake = {
                        navController.navigate(ShutterUpDestinations.detail(dateIso)) {
                            popUpTo(ShutterUpDestinations.HOME)
                        }
                    },
                )
            }
        }
    }
}

private fun tabForRoute(route: String): ShutterUpDestination = when {
    route.startsWith(ShutterUpDestinations.CALENDAR) -> ShutterUpDestination.Calendar
    route.startsWith(ShutterUpDestinations.FEED) || route.startsWith(ShutterUpDestinations.THEMES) ->
        ShutterUpDestination.Feed
    route.startsWith(ShutterUpDestinations.BADGES) -> ShutterUpDestination.Badges
    route.startsWith(ShutterUpDestinations.SETTINGS) -> ShutterUpDestination.Settings
    else -> ShutterUpDestination.Home
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(ShutterUpDestinations.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Deep link `shutterup://day/<ISO-date>` into Prompt Detail (SPEC §8.1, §15.2). */
fun NavHostController.navigateDayUri(uri: Uri?) {
    if (uri == null) return
    if (uri.scheme != "shutterup" || uri.host != "day") return
    val date = uri.pathSegments.firstOrNull() ?: return
    val auto = uri.getBooleanQueryParameter("autoLaunchCamera", false)
    val reroll = uri.getBooleanQueryParameter("reroll", false)
    navigate(ShutterUpDestinations.detail(date, auto, reroll))
}
