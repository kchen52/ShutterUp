package app.shutterup.ui.adaptive

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.window.core.layout.WindowSizeClass
import app.shutterup.ui.theme.ShutterUpTheme

/**
 * Adaptive app chrome: bottom bar on compact width, [NavigationRail] on
 * medium and expanded width. Never a drawer (DESIGN.md §3).
 *
 * Compact bar: Today · Calendar · Feed · Badges.
 * Rail: the same four, plus Settings pinned at the bottom.
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
    when (resolvedType) {
        NavigationSuiteType.None -> Box(modifier = modifier.fillMaxSize()) { content() }
        NavigationSuiteType.NavigationBar -> {
            Scaffold(
                modifier = modifier,
                bottomBar = {
                    NavigationBar {
                        ShutterUpDestination.entries.filter { it.primaryTab }.forEach { destination ->
                            val isSelected = destination == selected
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { callbacks.onSelect(destination) },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon(isSelected),
                                        contentDescription = destination.label,
                                    )
                                },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                },
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    content()
                }
            }
        }
        else -> {
            Row(modifier = modifier.fillMaxSize()) {
                NavigationRail(modifier = Modifier.fillMaxHeight()) {
                    ShutterUpDestination.entries.filter { it.primaryTab }.forEach { destination ->
                        val isSelected = destination == selected
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { callbacks.onSelect(destination) },
                            icon = {
                                Icon(
                                    imageVector = destination.icon(isSelected),
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    val settingsSelected = selected == ShutterUpDestination.Settings
                    NavigationRailItem(
                        selected = settingsSelected,
                        onClick = callbacks.onSettings,
                        icon = {
                            Icon(
                                imageVector = ShutterUpDestination.Settings.icon(settingsSelected),
                                contentDescription = ShutterUpDestination.Settings.label,
                            )
                        },
                        label = { Text(ShutterUpDestination.Settings.label) },
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Compact width → [NavigationSuiteType.NavigationBar]; medium and expanded →
 * [NavigationSuiteType.NavigationRail]. Drawer is never the default.
 */
fun shutterUpNavigationSuiteType(adaptiveInfo: WindowAdaptiveInfo): NavigationSuiteType =
    shutterUpNavigationSuiteType(adaptiveInfo.windowSizeClass.minWidthDp)

/** Width-class mapping used by [ShutterUpAdaptiveScaffold] and unit tests. */
fun shutterUpNavigationSuiteType(minWidthDp: Int): NavigationSuiteType = when {
    minWidthDp < WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND ->
        NavigationSuiteType.NavigationBar
    else -> NavigationSuiteType.NavigationRail
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
            layoutType = NavigationSuiteType.NavigationRail,
        )
    }
}

@Preview(name = "Expanded dark", widthDp = 1000, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ScaffoldPreviewExpandedDark() {
    ShutterUpTheme(darkTheme = true) {
        SampleScaffold(
            selected = ShutterUpDestination.Settings,
            layoutType = NavigationSuiteType.NavigationRail,
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
