package app.shutterup.ui.home

import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.domain.model.StreakState
import app.shutterup.ui.calendar.spokenDate
import app.shutterup.ui.components.Kicker
import app.shutterup.ui.components.LibraryTag
import app.shutterup.ui.components.NotificationPermissionCard
import app.shutterup.ui.components.ShootButton
import app.shutterup.ui.components.StreakStatus
import app.shutterup.ui.detail.samplePrompt
import app.shutterup.ui.settings.SettingsCopy
import app.shutterup.ui.theme.LocalThemeTint
import app.shutterup.ui.theme.ProvideThemeTint
import app.shutterup.ui.theme.ShutterUpTheme
import coil3.compose.AsyncImage
import java.io.File
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Today card, streak row, month ring, recent strip. Shoot opens Prompt Detail.
 *
 * @param listPane expanded left pane: compact Today minus the card's detail text
 *   and without Shoot (Shoot lives in the Prompt Detail pane).
 */
@Composable
fun HomeRoute(
    onOpenDetail: (dateIso: String, autoLaunchCamera: Boolean) -> Unit,
    onOpenDay: (dateIso: String) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    listPane: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    HomeScreen(
        state = state,
        listPane = listPane,
        onShoot = { onOpenDetail(state.today.toString(), true) },
        onDetails = { onOpenDetail(state.today.toString(), false) },
        onAddNote = { onOpenDay(state.today.toString()) },
        onRetake = { onOpenDetail(state.today.toString(), true) },
        onOpenDay = onOpenDay,
        onOpenCalendar = onOpenCalendar,
        onOpenSettings = onOpenSettings,
        onResume = viewModel::resume,
        onOpenNotificationSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                },
            )
        },
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onShoot: () -> Unit = {},
    onDetails: () -> Unit = {},
    onAddNote: () -> Unit = {},
    onRetake: () -> Unit = {},
    onOpenDay: (String) -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onResume: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    listPane: Boolean = false,
) {
    val dark = isSystemInDarkTheme()
    val prompt = state.prompt
    ProvideThemeTint(theme = prompt?.theme.orEmpty(), darkTheme = dark) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            val cardMin = maxHeight * 0.55f
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "ShutterUp",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
                TodayCard(
                    state = state,
                    minHeight = cardMin,
                    listPane = listPane,
                    onShoot = onShoot,
                    onDetails = onDetails,
                    onAddNote = onAddNote,
                    onRetake = onRetake,
                    onResume = onResume,
                )
                StreakStatus(
                    streakDays = state.streak.current,
                    freezes = state.streak.freezes,
                    monthCompleted = state.monthCompleted,
                    monthEligible = state.monthEligible,
                )
                if (state.aiDownloadPercent != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = SettingsCopy.AI_PREPARING,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (state.notificationsDenied) {
                    NotificationPermissionCard(onOpenSettings = onOpenNotificationSettings)
                }
                if (state.preparingPrompts) {
                    Text(
                        text = "Preparing prompts…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RecentStrip(entries = state.recent, onOpenDay = onOpenDay, onOpenCalendar = onOpenCalendar)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TodayCard(
    state: HomeUiState,
    minHeight: androidx.compose.ui.unit.Dp,
    listPane: Boolean,
    onShoot: () -> Unit,
    onDetails: () -> Unit,
    onAddNote: () -> Unit,
    onRetake: () -> Unit,
    onResume: () -> Unit,
) {
    val prompt = state.prompt
    if (state.paused) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Kicker(text = "Resume when you are ready")
                Text(text = "Paused", style = MaterialTheme.typography.displaySmall)
                Button(onClick = onResume, shape = RoundedCornerShape(50)) {
                    Text("Resume")
                }
            }
        }
        return
    }
    if (prompt == null) {
        val copy = when {
            state.preparingPrompts || state.aiDownloadPercent != null -> "Preparing on-device AI"
            else -> SettingsCopy.AI_UNAVAILABLE
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Box(Modifier.fillMaxWidth().padding(20.dp)) {
                Text(copy, style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }
    if (prompt.status == DayStatus.COMPLETED || prompt.status == DayStatus.COMPLETED_NO_PHOTO) {
        CompletedTodayCard(
            prompt = prompt,
            photoPath = state.recent.firstOrNull { it.date == state.today }?.thumbPath
                ?: state.recent.firstOrNull { it.date == state.today }?.mediaUri,
            minHeight = minHeight,
            onAddNote = onAddNote,
            onRetake = onRetake,
        )
        return
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = LocalThemeTint.current),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TodayKickerRow(prompt)
            val skipped = prompt.status == DayStatus.SKIPPED
            Text(
                text = prompt.title,
                style = MaterialTheme.typography.displaySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = if (skipped) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (skipped) TextDecoration.LineThrough else TextDecoration.None,
            )
            if (skipped) {
                Text(
                    text = "Skipped — see you tomorrow.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                if (!listPane) {
                    Text(
                        text = prompt.oneLiner,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!listPane) {
                        ShootButton(onClick = onShoot)
                        Spacer(Modifier.weight(1f))
                    }
                    TextButton(onClick = onDetails) { Text("Details →") }
                }
            }
        }
    }
}

@Composable
private fun TodayKickerRow(prompt: DayPrompt) {
    val weekday = prompt.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Kicker(text = "$weekday · ${prompt.theme}")
        if (prompt.source == PromptSourceRef.LIBRARY) {
            LibraryTag()
        }
    }
}

@Composable
private fun CompletedTodayCard(
    prompt: DayPrompt,
    photoPath: String?,
    minHeight: androidx.compose.ui.unit.Dp,
    onAddNote: () -> Unit,
    onRetake: () -> Unit,
) {
    val weekday = prompt.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(28.dp)),
    ) {
        val file = photoPath?.let(::File)
        if (file != null && file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = "Today's photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "$weekday · ${prompt.theme}".uppercase(Locale.ENGLISH),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = prompt.title,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.displaySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row {
                TextButton(onClick = onAddNote) { Text("Add a note") }
                TextButton(onClick = onRetake) { Text("Retake") }
            }
        }
    }
}

@Composable
private fun RecentStrip(
    entries: List<Entry>,
    onOpenDay: (String) -> Unit,
    onOpenCalendar: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Kicker(text = "Recent")
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenCalendar) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Calendar")
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries, key = { "${it.date}-${it.id}" }) { entry ->
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp),
                        )
                        .clickable { onOpenDay(entry.date.toString()) },
                ) {
                    val file = File(entry.thumbPath)
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = "${spokenDate(entry.date)}, completed",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        )
                    }
                }
            }
        }
    }
}

/** Deterministic Home state for previews and Roborazzi. */
fun sampleHomeState(
    prompt: DayPrompt = samplePrompt(),
    paused: Boolean = false,
    notificationsDenied: Boolean = false,
    aiDownloadPercent: Int? = null,
): HomeUiState = HomeUiState(
    today = LocalDate.of(2026, 9, 19),
    prompt = prompt,
    streak = StreakState(current = 14, longest = 22, freezes = 2, lastProcessedDate = prompt.date),
    monthCompleted = 18,
    monthEligible = 19,
    recent = emptyList(),
    paused = paused,
    notificationsDenied = notificationsDenied,
    aiDownloadPercent = aiDownloadPercent,
)

@Preview(name = "Compact light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomePreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface { HomeScreen(state = sampleHomeState()) }
    }
}

@Preview(
    name = "Compact dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomePreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface { HomeScreen(state = sampleHomeState()) }
    }
}
