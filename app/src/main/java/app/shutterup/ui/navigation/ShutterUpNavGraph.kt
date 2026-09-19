package app.shutterup.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.shutterup.ui.completion.CompletionRoute
import app.shutterup.ui.detail.PromptDetailRoute

object ShutterUpDestinations {
    const val HOME = "home"
    const val DETAIL = "detail/{dateIso}?autoLaunchCamera={autoLaunchCamera}&reroll={reroll}"
    const val COMPLETION = "completion/{dateIso}?newBadges={newBadges}&freezeEarned={freezeEarned}&previousStreak={previousStreak}"

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
 * App navigation: Home placeholder, Prompt Detail, Completion.
 */
@Composable
fun ShutterUpNavGraph(
    navController: NavHostController,
    todayIso: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = ShutterUpDestinations.HOME,
        modifier = modifier,
    ) {
        composable(ShutterUpDestinations.HOME) {
            HomePlaceholder(
                onOpenToday = {
                    navController.navigate(ShutterUpDestinations.detail(todayIso))
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

/** Deep link `shutterup://day/<ISO-date>` into Prompt Detail. */
fun NavHostController.navigateDayUri(uri: Uri?) {
    if (uri == null) return
    if (uri.scheme != "shutterup" || uri.host != "day") return
    val date = uri.pathSegments.firstOrNull() ?: return
    val auto = uri.getBooleanQueryParameter("autoLaunchCamera", false)
    val reroll = uri.getBooleanQueryParameter("reroll", false)
    navigate(ShutterUpDestinations.detail(date, auto, reroll))
}

@Composable
private fun HomePlaceholder(onOpenToday: () -> Unit) {
    Surface {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Button(onClick = onOpenToday) {
                Text("Today")
            }
        }
    }
}
