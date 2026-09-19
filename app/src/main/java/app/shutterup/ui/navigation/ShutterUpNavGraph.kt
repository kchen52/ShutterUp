package app.shutterup.ui.navigation

import android.net.Uri
import app.shutterup.navigation.DeepLinks
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.shutterup.ui.adaptive.ShutterUpAdaptiveScaffold
import app.shutterup.ui.adaptive.ShutterUpDestination
import app.shutterup.ui.adaptive.ShutterUpListDetail
import app.shutterup.ui.adaptive.ShutterUpTabCallbacks
import app.shutterup.ui.adaptive.isExpandedWidth
import app.shutterup.ui.badges.BadgesRoute
import app.shutterup.ui.calendar.CalendarRoute
import app.shutterup.ui.completion.CompletionRoute
import app.shutterup.ui.day.DayRoute
import app.shutterup.ui.detail.PromptDetailRoute
import app.shutterup.ui.feed.FeedRoute
import app.shutterup.ui.home.HomeRoute
import app.shutterup.ui.settings.SettingsRoute

object ShutterUpDestinations {
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val FEED = "feed"
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
 * App navigation: Today, Calendar, Feed, Badges as tabs; Settings as a
 * non-tab route; Day / Prompt Detail / Completion as overlays.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
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
    val expanded = isExpandedWidth(currentWindowAdaptiveInfo().windowSizeClass.minWidthDp)
    ShutterUpAdaptiveScaffold(
        selected = selected,
        callbacks = ShutterUpTabCallbacks(
            onHome = { navController.navigateTab(ShutterUpDestinations.HOME) },
            onCalendar = { navController.navigateTab(ShutterUpDestinations.CALENDAR) },
            onFeed = { navController.navigateTab(ShutterUpDestinations.FEED) },
            onBadges = { navController.navigateTab(ShutterUpDestinations.BADGES) },
            onSettings = { navController.navigateSettings() },
        ),
        layoutType = if (hideChrome) NavigationSuiteType.None else null,
    ) {
        NavHost(
            navController = navController,
            startDestination = ShutterUpDestinations.HOME,
            modifier = modifier,
        ) {
            composable(ShutterUpDestinations.HOME) {
                if (expanded) {
                    ShutterUpListDetail(
                        listFraction = 0.40f,
                        list = {
                            HomeRoute(
                                listPane = true,
                                onOpenDetail = { dateIso, auto ->
                                    navController.navigate(ShutterUpDestinations.detail(dateIso, auto))
                                },
                                onOpenDay = { dateIso ->
                                    navController.navigate(ShutterUpDestinations.day(dateIso))
                                },
                                onOpenCalendar = {
                                    navController.navigateTab(ShutterUpDestinations.CALENDAR)
                                },
                                onOpenSettings = { navController.navigateSettings() },
                            )
                        },
                        detail = {
                            EmbeddedPromptDetail(
                                dateIso = todayIso,
                                parentNav = navController,
                            )
                        },
                    )
                } else {
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
                        onOpenSettings = { navController.navigateSettings() },
                    )
                }
            }
            composable(ShutterUpDestinations.CALENDAR) {
                CalendarRoute(
                    onOpenDay = { dateIso ->
                        navController.navigate(ShutterUpDestinations.day(dateIso))
                    },
                    onOpenDetail = { dateIso, auto ->
                        navController.navigate(ShutterUpDestinations.detail(dateIso, auto))
                    },
                    onOpenCompletion = { dateIso ->
                        navController.navigate(ShutterUpDestinations.completion(dateIso))
                    },
                )
            }
            composable(ShutterUpDestinations.FEED) {
                FeedRoute(
                    onOpenDay = { dateIso ->
                        navController.navigate(ShutterUpDestinations.day(dateIso))
                    },
                )
            }
            composable(ShutterUpDestinations.BADGES) {
                BadgesRoute()
            }
            composable(ShutterUpDestinations.SETTINGS) {
                SettingsRoute()
            }
            addDay(navController)
            addDetail(navController)
            addCompletion(navController, todayIso)
        }
    }
}

private fun NavGraphBuilder.addDay(navController: NavHostController) {
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
}

private fun NavGraphBuilder.addDetail(navController: NavHostController) {
    composable(
        route = ShutterUpDestinations.DETAIL,
        arguments = detailArguments(),
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
}

private fun NavGraphBuilder.addCompletion(navController: NavHostController, todayIso: String) {
    composable(
        route = ShutterUpDestinations.COMPLETION,
        arguments = completionArguments(),
    ) { entry ->
        val dateIso = entry.arguments?.getString("dateIso").orEmpty()
        CompletionRoute(
            onStay = { navController.popCompletionToOpener() },
            onRetake = {
                if (dateIso == todayIso) {
                    navController.navigate(ShutterUpDestinations.detail(dateIso)) {
                        popUpTo(ShutterUpDestinations.COMPLETION) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
        )
    }
}

@Composable
private fun EmbeddedPromptDetail(
    dateIso: String,
    parentNav: NavHostController,
) {
    val paneNav = rememberNavController()
    NavHost(
        navController = paneNav,
        startDestination = ShutterUpDestinations.detail(dateIso),
    ) {
        composable(
            route = ShutterUpDestinations.DETAIL,
            arguments = detailArguments(),
        ) {
            PromptDetailRoute(
                onBack = { },
                onOpenCompletion = { nav ->
                    parentNav.navigate(
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
    }
}

private fun detailArguments() = listOf(
    navArgument("dateIso") { type = NavType.StringType },
    navArgument("autoLaunchCamera") {
        type = NavType.BoolType
        defaultValue = false
    },
    navArgument("reroll") {
        type = NavType.BoolType
        defaultValue = false
    },
)

private fun completionArguments() = listOf(
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
)

private fun tabForRoute(route: String): ShutterUpDestination = when {
    route.startsWith(ShutterUpDestinations.CALENDAR) -> ShutterUpDestination.Calendar
    route.startsWith(ShutterUpDestinations.FEED) -> ShutterUpDestination.Feed
    route.startsWith(ShutterUpDestinations.BADGES) -> ShutterUpDestination.Badges
    route.startsWith(ShutterUpDestinations.SETTINGS) -> ShutterUpDestination.Settings
    else -> ShutterUpDestination.Home
}

/**
 * Switch primary tabs. Settings is a non-tab overlay on Today, so it is
 * discarded rather than saved — returning to Today always shows Home.
 */
internal fun NavHostController.navigateTab(route: String) {
    popBackStack(ShutterUpDestinations.SETTINGS, inclusive = true)
    navigate(route) {
        popUpTo(ShutterUpDestinations.HOME) { saveState = true }
        launchSingleTop = true
        // Restoring Home's stack would bring Settings back after a tab switch.
        restoreState = route != ShutterUpDestinations.HOME
    }
}

internal fun NavHostController.navigateSettings() {
    navigate(ShutterUpDestinations.SETTINGS) {
        launchSingleTop = true
    }
}

/** Done / STAY returns to the screen that opened the capture flow (Home or Day). */
private fun NavHostController.popCompletionToOpener() {
    if (popBackStack(ShutterUpDestinations.DAY, inclusive = false)) return
    popBackStack(ShutterUpDestinations.HOME, inclusive = false)
}

/** Deep links `shutterup://day|detail/<ISO-date>` into Prompt Detail (SPEC §8.1). */
fun NavHostController.navigateDayUri(uri: Uri?) {
    if (uri == null) return
    if (!DeepLinks.isPromptLink(uri.scheme, uri.host)) return
    val date = uri.pathSegments.firstOrNull() ?: return
    val auto = uri.getBooleanQueryParameter(DeepLinks.QUERY_AUTO_LAUNCH, false)
    val reroll = uri.getBooleanQueryParameter(DeepLinks.QUERY_REROLL, false)
    navigate(ShutterUpDestinations.detail(date, auto, reroll)) {
        launchSingleTop = true
        popUpTo(ShutterUpDestinations.HOME) { inclusive = false }
    }
}
