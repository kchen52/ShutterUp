package app.shutterup.ui.themes

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shutterup.ui.components.Kicker
import app.shutterup.ui.components.ThemeChip
import app.shutterup.ui.feed.FeedCardUi
import app.shutterup.ui.feed.FeedEmptyState
import app.shutterup.ui.feed.HistoryCard
import app.shutterup.ui.feed.feedKicker
import app.shutterup.ui.theme.ShutterUpTheme
import app.shutterup.ui.theme.themeTint
import java.time.LocalDate

/**
 * Theme-focus browser. Compact: list with counts, then a filtered feed.
 * Expanded: list left, filtered grid right (SPEC §5).
 *
 * @param onOpenDay ISO-8601 local date of a tapped completed day.
 * @param onBack returns to Feed (Themes is not a top-level destination).
 */
@Composable
fun ThemesRoute(
    onOpenDay: (String) -> Unit,
    onBack: () -> Unit = {},
    viewModel: ThemesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ThemesScreen(
        state = state,
        onOpenDay = onOpenDay,
        onBack = onBack,
        onSelectTheme = viewModel::selectTheme,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesScreen(
    state: ThemesUiState,
    onOpenDay: (String) -> Unit,
    onBack: () -> Unit = {},
    onSelectTheme: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Themes", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val expanded = maxWidth >= 600.dp
            if (expanded) {
                ThemesExpanded(
                    state = state,
                    onOpenDay = onOpenDay,
                    onSelectTheme = onSelectTheme,
                )
            } else {
                ThemesCompact(
                    state = state,
                    onOpenDay = onOpenDay,
                    onSelectTheme = onSelectTheme,
                )
            }
        }
    }
}

@Composable
private fun ThemesCompact(
    state: ThemesUiState,
    onOpenDay: (String) -> Unit,
    onSelectTheme: (String?) -> Unit,
) {
    if (state.selectedTheme == null) {
        ThemeList(
            state = state,
            onSelectTheme = onSelectTheme,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            ThemeChipRow(
                themes = state.themes,
                selectedTheme = state.selectedTheme,
                onSelectTheme = onSelectTheme,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            FilteredGrid(
                items = state.items,
                columns = 1,
                onOpenDay = onOpenDay,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ThemesExpanded(
    state: ThemesUiState,
    onOpenDay: (String) -> Unit,
    onSelectTheme: (String?) -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        ThemeList(
            state = state,
            onSelectTheme = onSelectTheme,
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight(),
        )
        FilteredGrid(
            items = state.items,
            columns = 2,
            onOpenDay = onOpenDay,
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun ThemeList(
    state: ThemesUiState,
    onSelectTheme: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.themeFocus?.let { focus ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Kicker(text = "Theme focus")
                    Text(text = focus, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        if (state.themes.isEmpty()) {
            item { FeedEmptyState() }
        } else {
            items(state.themes, key = { it.theme }) { row ->
                ThemeCountRow(
                    row = row,
                    selected = state.selectedTheme == row.theme,
                    onClick = {
                        onSelectTheme(if (state.selectedTheme == row.theme) null else row.theme)
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemeCountRow(
    row: ThemeCountUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val tint = themeTint(row.theme, MaterialTheme.colorScheme, dark)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${row.theme}, ${row.count}" },
        color = if (selected) tint else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = row.theme, style = MaterialTheme.typography.titleLarge)
            Text(
                text = row.count.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeChipRow(
    themes: List<ThemeCountUi>,
    selectedTheme: String?,
    onSelectTheme: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ThemeChip(
                label = "All",
                selected = selectedTheme == null,
                onClick = { onSelectTheme(null) },
            )
        }
        items(themes) { row ->
            val dark = isSystemInDarkTheme()
            ThemeChip(
                label = "${row.theme} ${row.count}",
                selected = selectedTheme == row.theme,
                tint = themeTint(row.theme, MaterialTheme.colorScheme, dark),
                onClick = { onSelectTheme(row.theme) },
            )
        }
    }
}

@Composable
private fun FilteredGrid(
    items: List<FeedCardUi>,
    columns: Int,
    onOpenDay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        FeedEmptyState(modifier = modifier)
        return
    }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalItemSpacing = 16.dp,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(items, key = { it.dateIso }) { card ->
            HistoryCard(card = card, onClick = { onOpenDay(card.dateIso) })
        }
    }
}

internal fun sampleThemesState(): ThemesUiState = ThemesUiState(
    themeFocus = "black & white",
    themes = listOf(
        ThemeCountUi("Reflections", 2),
        ThemeCountUi("Quiet hours", 1),
    ),
    selectedTheme = "Reflections",
    items = listOf(
        FeedCardUi(
            dateIso = "2026-09-19",
            title = "Find the sky in a puddle",
            theme = "Reflections",
            kicker = feedKicker(LocalDate.of(2026, 9, 19), "Reflections"),
            thumbPath = null,
            aspectRatio = 3f / 4f,
        ),
        FeedCardUi(
            dateIso = "2026-09-05",
            title = "Glass on glass",
            theme = "Reflections",
            kicker = feedKicker(LocalDate.of(2026, 9, 5), "Reflections"),
            thumbPath = null,
            aspectRatio = 1f,
        ),
    ),
)

@Preview(name = "compact-light", showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun ThemesPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        ThemesScreen(state = sampleThemesState(), onOpenDay = {})
    }
}

@Preview(
    name = "compact-dark",
    showBackground = true,
    widthDp = 400,
    heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ThemesPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        ThemesScreen(state = sampleThemesState(), onOpenDay = {})
    }
}

@Preview(name = "expanded-light", showBackground = true, widthDp = 840, heightDp = 900)
@Composable
private fun ThemesPreviewExpanded() {
    ShutterUpTheme(darkTheme = false) {
        ThemesScreen(state = sampleThemesState(), onOpenDay = {})
    }
}

@Preview(name = "font-scale-2", showBackground = true, widthDp = 400, heightDp = 900, fontScale = 2f)
@Composable
private fun ThemesPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        ThemesScreen(state = sampleThemesState(), onOpenDay = {})
    }
}
