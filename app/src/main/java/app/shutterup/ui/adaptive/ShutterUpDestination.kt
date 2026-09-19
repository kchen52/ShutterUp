package app.shutterup.ui.adaptive

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level destinations shown in [ShutterUpAdaptiveScaffold].
 *
 * Route strings live in the nav graph (worker A). This model is
 * `currentRoute`-agnostic: the host passes [selected] plus [ShutterUpTabCallbacks].
 */
enum class ShutterUpDestination(
    /** Visible label (DESIGN.md §3: Today, not "Home"). */
    val label: String,
    val icon: ImageVector,
) {
    Home(label = "Today", icon = Icons.Outlined.Home),
    Calendar(label = "Calendar", icon = Icons.Outlined.CalendarMonth),
    Feed(label = "Feed", icon = Icons.Outlined.GridView),
    Badges(label = "Badges", icon = Icons.Outlined.MilitaryTech),
    Settings(label = "Settings", icon = Icons.Outlined.Settings),
}

/**
 * Click handlers for each [ShutterUpDestination]. The scaffold never holds a
 * `NavController`; worker A maps these to `navController.navigate(...)`.
 */
data class ShutterUpTabCallbacks(
    val onHome: () -> Unit,
    val onCalendar: () -> Unit,
    val onFeed: () -> Unit,
    val onBadges: () -> Unit,
    val onSettings: () -> Unit,
) {
    /** Dispatches [destination] to the matching callback. */
    fun onSelect(destination: ShutterUpDestination) {
        when (destination) {
            ShutterUpDestination.Home -> onHome()
            ShutterUpDestination.Calendar -> onCalendar()
            ShutterUpDestination.Feed -> onFeed()
            ShutterUpDestination.Badges -> onBadges()
            ShutterUpDestination.Settings -> onSettings()
        }
    }
}
