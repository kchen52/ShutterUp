package app.shutterup.ui.feed

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shutterup.ui.components.Kicker
import app.shutterup.ui.components.ThemeChip
import app.shutterup.ui.theme.ProvideThemeTint
import app.shutterup.ui.theme.ShutterUpTheme
import app.shutterup.ui.theme.themeTint
import coil3.compose.AsyncImage
import java.time.LocalDate
import java.util.Locale

/**
 * Chronological completed-days feed. Navigation is callback-only so the
 * nav graph can wire routes without this screen holding a [androidx.navigation.NavController].
 *
 * @param onOpenDay ISO-8601 local date of the tapped completed day.
 */
@Composable
fun FeedRoute(
    onOpenDay: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FeedScreen(
        state = state,
        onOpenDay = onOpenDay,
        onSelectTheme = viewModel::selectTheme,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    state: FeedUiState,
    onOpenDay: (String) -> Unit,
    onSelectTheme: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Feed", style = MaterialTheme.typography.headlineMedium) },
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
            val columns = if (expanded) 2 else 1
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    FeedFilterRow(
                        themes = state.themes,
                        selectedTheme = state.selectedTheme,
                        onSelectTheme = onSelectTheme,
                    )
                }
                if (state.items.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        FeedEmptyState()
                    }
                } else {
                    items(state.items, key = { it.dateIso }) { card ->
                        HistoryCard(
                            card = card,
                            onClick = { onOpenDay(card.dateIso) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedFilterRow(
    themes: List<String>,
    selectedTheme: String?,
    onSelectTheme: (String?) -> Unit,
) {
    val dark = isSystemInDarkTheme()
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            ProvideThemeTint(theme = "All", darkTheme = dark) {
                ThemeChip(
                    label = "ALL",
                    selected = selectedTheme == null,
                    onClick = { onSelectTheme(null) },
                )
            }
        }
        items(themes) { theme ->
            ThemeChip(
                label = theme.uppercase(Locale.ENGLISH),
                selected = selectedTheme == theme,
                tint = themeTint(theme, MaterialTheme.colorScheme, dark),
                onClick = { onSelectTheme(theme) },
            )
        }
    }
}

@Composable
fun HistoryCard(
    card: FeedCardUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val tint = themeTint(card.theme, MaterialTheme.colorScheme, dark)
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = card.spokenDescription
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(card.aspectRatio)
                    .clip(RoundedCornerShape(16.dp))
                    .background(tint),
                contentAlignment = Alignment.Center,
            ) {
                if (card.thumbPath != null) {
                    AsyncImage(
                        model = card.thumbPath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Kicker(
                text = card.kicker,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp, end = 4.dp),
            )
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
fun FeedEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Your first photo goes here.",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Come back after today's prompt.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

internal fun sampleFeedState(): FeedUiState = FeedUiState(
    items = listOf(
        FeedCardUi(
            dateIso = "2026-09-19",
            title = "Find the sky in a puddle",
            theme = "Reflections",
            kicker = feedKicker(LocalDate.of(2026, 9, 19), "Reflections"),
            spokenDescription = "19 September, completed",
            thumbPath = null,
            aspectRatio = 3f / 4f,
        ),
        FeedCardUi(
            dateIso = "2026-09-12",
            title = "Kitchen still life after dark",
            theme = "Quiet hours",
            kicker = feedKicker(LocalDate.of(2026, 9, 12), "Quiet hours"),
            spokenDescription = "12 September, completed",
            thumbPath = null,
            aspectRatio = 4f / 5f,
        ),
    ),
    themes = listOf("Reflections", "Quiet hours"),
    selectedTheme = null,
)

@Preview(name = "compact-light", showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun FeedPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        FeedScreen(state = sampleFeedState(), onOpenDay = {})
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
private fun FeedPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        FeedScreen(state = sampleFeedState(), onOpenDay = {})
    }
}

@Preview(name = "expanded-light", showBackground = true, widthDp = 840, heightDp = 900)
@Composable
private fun FeedPreviewExpanded() {
    ShutterUpTheme(darkTheme = false) {
        FeedScreen(state = sampleFeedState(), onOpenDay = {})
    }
}

@Preview(name = "font-scale-2", showBackground = true, widthDp = 400, heightDp = 900, fontScale = 2f)
@Composable
private fun FeedPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        FeedScreen(state = sampleFeedState(), onOpenDay = {})
    }
}
