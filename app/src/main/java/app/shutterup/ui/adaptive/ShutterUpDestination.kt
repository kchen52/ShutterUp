package app.shutterup.ui.adaptive

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Destinations for [ShutterUpAdaptiveScaffold].
 *
 * Four primary tabs (DESIGN.md §3): Today · Calendar · Feed · Badges.
 * Settings is not a compact tab; it sits at the bottom of the expanded rail
 * and is opened from Today's gear on compact width.
 */
enum class ShutterUpDestination(
    /** Visible label (DESIGN.md §3: Today, not "Home"). */
    val label: String,
    val outlinedIcon: ImageVector,
    val selectedIcon: ImageVector,
    /** Compact bottom bar shows only primary tabs. */
    val primaryTab: Boolean,
) {
    Home(
        label = "Today",
        outlinedIcon = Icons.Outlined.Home,
        selectedIcon = Icons.Rounded.Home,
        primaryTab = true,
    ),
    Calendar(
        label = "Calendar",
        outlinedIcon = Icons.Outlined.CalendarMonth,
        selectedIcon = Icons.Rounded.CalendarMonth,
        primaryTab = true,
    ),
    Feed(
        label = "Feed",
        outlinedIcon = Icons.Outlined.GridView,
        selectedIcon = Icons.Rounded.GridView,
        primaryTab = true,
    ),
    Badges(
        label = "Badges",
        outlinedIcon = Icons.Outlined.MilitaryTech,
        selectedIcon = Icons.Rounded.MilitaryTech,
        primaryTab = true,
    ),
    Settings(
        label = "Settings",
        outlinedIcon = Icons.Outlined.Settings,
        selectedIcon = Icons.Rounded.Settings,
        primaryTab = false,
    );

    fun icon(selected: Boolean): ImageVector = if (selected) selectedIcon else outlinedIcon
}

/**
 * Click handlers for each [ShutterUpDestination]. The scaffold never holds a
 * `NavController`; the host maps these to `navController.navigate(...)`.
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
