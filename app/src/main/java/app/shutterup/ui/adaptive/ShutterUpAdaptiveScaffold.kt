package app.shutterup.ui.adaptive

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.window.core.layout.WindowSizeClass
import app.shutterup.ui.theme.ShutterUpTheme

/**
 * Adaptive app chrome: bottom bar on compact width, navigation rail on medium
 * width, permanent drawer on expanded width (`material3-adaptive-navigation-suite`).
 *
 * Worker A wires this **around** the nav host without this module importing
 * navigation:
 *
 * ```
 * var selected by remember { mutableStateOf(ShutterUpDestination.Home) }
 * ShutterUpAdaptiveScaffold(
 *     selected = selected,
 *     callbacks = ShutterUpTabCallbacks(
 *         onHome = { selected = ShutterUpDestination.Home; nav.navigate(HOME) },
 *         onCalendar = { selected = ShutterUpDestination.Calendar; nav.navigate(CALENDAR) },
 *         onFeed = { selected = ShutterUpDestination.Feed; nav.navigate(FEED) },
 *         onBadges = { selected = ShutterUpDestination.Badges; nav.navigate(BADGES) },
 *         onSettings = { selected = ShutterUpDestination.Settings; nav.navigate(SETTINGS) },
 *     ),
 * ) {
 *     ShutterUpNavGraph(navController, todayIso)
 * }
 * ```
 *
 * Hide the suite on Prompt Detail / Completion by not wrapping those routes, or
 * pass [layoutType] = [NavigationSuiteType.None].
 *
 * @param selected which tab is highlighted (independent of the current back stack).
 * @param callbacks tab clicks; must not capture a NavController inside this file.
 * @param layoutType optional override (screenshot tests / custom hosts). Null
 *   derives the type from [currentWindowAdaptiveInfo].
 * @param content screen body (typically the NavHost).
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ShutterUpAdaptiveScaffold(
    selected: ShutterUpDestination,
    callbacks: ShutterUpTabCallbacks,
    modifier: Modifier = Modifier,
    layoutType: NavigationSuiteType? = null,
    content: @Composable () -> Unit,
) {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val resolvedType = layoutType ?: shutterUpNavigationSuiteType(adaptiveInfo)
    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = resolvedType,
        navigationSuiteItems = {
            ShutterUpDestination.entries.forEach { destination ->
                item(
                    selected = destination == selected,
                    onClick = { callbacks.onSelect(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                        )
                    },
                    label = { Text(destination.label) },
                )
            }
        },
        content = content,
    )
}

/**
 * Compact width → [NavigationSuiteType.NavigationBar], medium →
 * [NavigationSuiteType.NavigationRail], expanded → [NavigationSuiteType.NavigationDrawer].
 */
fun shutterUpNavigationSuiteType(adaptiveInfo: WindowAdaptiveInfo): NavigationSuiteType =
    shutterUpNavigationSuiteType(adaptiveInfo.windowSizeClass.minWidthDp)

/** Width-class mapping used by [ShutterUpAdaptiveScaffold] and unit tests. */
fun shutterUpNavigationSuiteType(minWidthDp: Int): NavigationSuiteType = when {
    minWidthDp < WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND ->
        NavigationSuiteType.NavigationBar
    minWidthDp < WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND ->
        NavigationSuiteType.NavigationRail
    else -> NavigationSuiteType.NavigationDrawer
}

@Preview(name = "Compact light", widthDp = 400, heightDp = 800)
@Composable
private fun ScaffoldPreviewCompactLight() {
    ShutterUpTheme(darkTheme = false) {
        SampleScaffold(ShutterUpDestination.Home)
    }
}

@Preview(name = "Expanded light", widthDp = 1000, heightDp = 800)
@Composable
private fun ScaffoldPreviewExpandedLight() {
    ShutterUpTheme(darkTheme = false) {
        SampleScaffold(
            selected = ShutterUpDestination.Settings,
            layoutType = NavigationSuiteType.NavigationDrawer,
        )
    }
}

@Preview(name = "Expanded dark", widthDp = 1000, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ScaffoldPreviewExpandedDark() {
    ShutterUpTheme(darkTheme = true) {
        SampleScaffold(
            selected = ShutterUpDestination.Settings,
            layoutType = NavigationSuiteType.NavigationDrawer,
        )
    }
}

@Preview(name = "FontScale 2x", widthDp = 400, heightDp = 800, fontScale = 2f)
@Composable
private fun ScaffoldPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        SampleScaffold(ShutterUpDestination.Calendar)
    }
}

@Composable
internal fun SampleScaffold(
    selected: ShutterUpDestination,
    layoutType: NavigationSuiteType? = null,
) {
    ShutterUpAdaptiveScaffold(
        selected = selected,
        callbacks = previewCallbacks(),
        layoutType = layoutType,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(selected.label)
        }
    }
}

internal fun previewCallbacks(): ShutterUpTabCallbacks = ShutterUpTabCallbacks(
    onHome = {},
    onCalendar = {},
    onFeed = {},
    onBadges = {},
    onSettings = {},
)
