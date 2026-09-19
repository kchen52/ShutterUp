package app.shutterup.ui.adaptive

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/** True when the inner / expanded width class is active (DESIGN.md §10). */
fun isExpandedWidth(minWidthDp: Int): Boolean =
    minWidthDp >= WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND

/**
 * List-detail split. [listFraction] is the list pane's share of the content
 * width (0.40 Today, 0.45 Calendar).
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ShutterUpListDetail(
    listFraction: Float,
    modifier: Modifier = Modifier,
    list: @Composable () -> Unit,
    detail: @Composable () -> Unit,
) {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val widthDp = LocalConfiguration.current.screenWidthDp
    val listWidth = (widthDp * listFraction).dp
    val directive = calculatePaneScaffoldDirective(adaptiveInfo).copy(
        maxHorizontalPartitions = 2,
        defaultPanePreferredWidth = listWidth,
    )
    val navigator = rememberListDetailPaneScaffoldNavigator<Unit>(
        scaffoldDirective = directive,
    )
    ListDetailPaneScaffold(
        modifier = modifier.fillMaxSize(),
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane { list() }
        },
        detailPane = {
            AnimatedPane { detail() }
        },
    )
}
