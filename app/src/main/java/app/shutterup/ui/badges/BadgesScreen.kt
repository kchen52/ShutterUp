package app.shutterup.ui.badges

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shutterup.domain.model.Achievement
import app.shutterup.domain.model.StreakState
import app.shutterup.ui.theme.ShutterUpTheme
import java.time.Instant
import java.time.LocalDate

/**
 * Sectioned badge grid. Detail is an in-place dialog (no [androidx.navigation.NavController]).
 */
@Composable
fun BadgesRoute(
    viewModel: BadgesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BadgesScreen(
        state = state,
        onSelectBadge = viewModel::selectBadge,
        onDismissDetail = viewModel::dismissDetail,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(
    state: BadgesUiState,
    onSelectBadge: (String) -> Unit = {},
    onDismissDetail: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Badges", style = MaterialTheme.typography.headlineMedium) },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        val cacheEntries = remember(state.sections) {
            state.sections.flatMap { section ->
                section.badges.map { it.id to it.unlocked }
            }
        }
        WarmBadgeEmblemCache(cacheEntries)
        // Keep collapsing-bar insets on the grid's contentPadding so the
        // viewport size stays stable; Modifier.padding would relayout every frame.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val minCell = if (maxWidth >= 600.dp) 96.dp else 108.dp
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = minCell),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(
                    key = "streak-stats",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "streak-stats",
                ) {
                    StreakStatRow(
                        current = state.currentStreak,
                        longest = state.longestStreak,
                    )
                }
                state.sections.forEach { section ->
                    item(
                        key = "header-${section.title}",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = "section-header",
                    ) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(
                        items = section.badges,
                        key = { it.id },
                        contentType = { "badge" },
                    ) { badge ->
                        BadgeGridCell(
                            badge = badge,
                            onClick = { onSelectBadge(badge.id) },
                        )
                    }
                }
            }
        }
    }
    state.selected?.let { badge ->
        BadgeDetailDialog(badge = badge, onDismiss = onDismissDetail)
    }
}

@Composable
private fun StreakStatRow(
    current: Int,
    longest: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatCell(
            number = current.toString(),
            caption = "days",
            modifier = Modifier.weight(1f),
        )
        StatCell(
            number = longest.toString(),
            caption = "longest",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCell(
    number: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = number, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BadgeGridCell(
    badge: BadgeCellUi,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp),
        ) {
            BadgeEmblem(badgeId = badge.id, unlocked = badge.unlocked, size = 96.dp)
            Text(
                text = badge.caption,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun BadgeDetailDialog(
    badge: BadgeCellUi,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        icon = {
            BadgeEmblem(badgeId = badge.id, unlocked = badge.unlocked, size = 160.dp)
        },
        title = {
            Text(text = badge.name, style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = badge.description, style = MaterialTheme.typography.bodyMedium)
                badge.unlockedOnLabel?.let { date ->
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

internal fun sampleBadgesState(): BadgesUiState {
    val first = Achievement(
        id = BadgeIds.FIRST_LIGHT,
        unlockedAt = Instant.parse("2026-09-01T08:00:00Z"),
        unlockedOnDate = LocalDate.of(2026, 9, 1),
    )
    val week = Achievement(
        id = BadgeIds.STREAK_7,
        unlockedAt = Instant.parse("2026-09-07T08:00:00Z"),
        unlockedOnDate = LocalDate.of(2026, 9, 7),
    )
    return badgesUiState(
        achievements = listOf(first, week),
        streak = StreakState(current = 14, longest = 14, freezes = 1, lastProcessedDate = LocalDate.of(2026, 9, 19)),
        selectedId = null,
    )
}

@Preview(name = "compact-light", showBackground = true, widthDp = 400, heightDp = 1400)
@Composable
private fun BadgesPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        BadgesScreen(state = sampleBadgesState())
    }
}

@Preview(
    name = "compact-dark",
    showBackground = true,
    widthDp = 400,
    heightDp = 1400,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun BadgesPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        BadgesScreen(state = sampleBadgesState())
    }
}

@Preview(name = "expanded-light", showBackground = true, widthDp = 840, heightDp = 900)
@Composable
private fun BadgesPreviewExpanded() {
    ShutterUpTheme(darkTheme = false) {
        BadgesScreen(state = sampleBadgesState())
    }
}

@Preview(name = "font-scale-2", showBackground = true, widthDp = 400, heightDp = 1800, fontScale = 2f)
@Composable
private fun BadgesPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        BadgesScreen(state = sampleBadgesState())
    }
}
