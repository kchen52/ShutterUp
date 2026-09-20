package app.shutterup.ui.completion

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.ui.badges.BadgeEmblem
import app.shutterup.ui.badges.badgeDescription
import app.shutterup.ui.badges.badgeDisplayName
import app.shutterup.ui.components.ApertureCheckMark
import app.shutterup.ui.components.Kicker
import app.shutterup.ui.components.LibraryTag
import app.shutterup.ui.detail.samplePrompt
import app.shutterup.ui.icons.SnowflakeIcon
import app.shutterup.ui.theme.ProvideThemeTint
import app.shutterup.ui.theme.ShutterUpTheme
import coil3.compose.AsyncImage
import java.io.File

/**
 * Completion: photo, note, streak, badge sheet, Done / Retake (DESIGN §4.3).
 */
@Composable
fun CompletionRoute(
    onStay: () -> Unit,
    onRetake: () -> Unit,
    viewModel: CompletionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CompletionScreen(
        state = state,
        onNoteChange = viewModel::updateNote,
        onDone = {
            viewModel.updateNote(state.note)
            onStay()
        },
        onRetake = onRetake,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletionScreen(
    state: CompletionUiState,
    onNoteChange: (String) -> Unit = {},
    onDone: () -> Unit = {},
    onRetake: () -> Unit = {},
    showBadgeSheet: Boolean = true,
    apertureProgress: Float? = null,
) {
    val prompt = state.prompt
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    var note by remember(state.note) { mutableStateOf(state.note) }
    var badgeIndex by remember { mutableIntStateOf(0) }
    val displayedStreak = state.streak.current
    val photoScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label = "photo-arrive",
    )
    val maxPhotoHeight = (LocalConfiguration.current.screenHeightDp * 0.6f).dp
    val photoShape = RoundedCornerShape(16.dp)
    val hairline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    ProvideThemeTint(theme = prompt?.theme.orEmpty(), darkTheme = dark) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val thumb = state.entries.lastOrNull()?.thumbPath
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxPhotoHeight)
                    .scale(photoScale)
                    .clip(photoShape)
                    .border(1.dp, hairline, photoShape),
                contentAlignment = Alignment.Center,
            ) {
                if (thumb != null && File(thumb).exists()) {
                    AsyncImage(
                        model = File(thumb),
                        contentDescription = "Today's photo",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Photo", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            ApertureCheckMark(
                size = 28.dp,
                color = MaterialTheme.colorScheme.secondary,
                progress = apertureProgress,
                playOnce = true,
                contentDescription = "Completed",
            )
            val kicker = buildString {
                val label = state.seriesTitle ?: prompt?.theme
                append(label?.uppercase().orEmpty())
                if (state.streak.current > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("DAY ${state.streak.current}")
                }
            }
            if (kicker.isNotEmpty()) Kicker(text = kicker)
            if (prompt?.source == PromptSourceRef.LIBRARY) {
                LibraryTag()
            }
            if (state.firstEver) {
                Text("First light.", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Day one. Everything else is repetition.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    text = prompt?.title.orEmpty(),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = {
                    note = it
                    onNoteChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add a note…") },
                minLines = 2,
                maxLines = 2,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnimatedContent(
                    targetState = displayedStreak,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "streak-roll",
                ) { value ->
                    Text(
                        text = "$value days",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                Icon(
                    imageVector = SnowflakeIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = state.streak.freezes.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (state.freezeEarned) {
                Text(
                    text = "You earned a freeze. It'll cover one missed day.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = {
                    onNoteChange(note)
                    onDone()
                }) { Text("Done") }
                TextButton(onClick = onRetake) { Text("Retake") }
            }
        }
    }
    val badges = state.newBadgeIds
    if (showBadgeSheet && badgeIndex < badges.size) {
        val id = badges[badgeIndex]
        ModalBottomSheet(
            onDismissRequest = { badgeIndex++ },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BadgeEmblem(badgeId = id, unlocked = true, size = 96.dp)
                Text(badgeDisplayName(id), style = MaterialTheme.typography.headlineSmall)
                Text(badgeDescription(id), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { badgeIndex++ }) { Text("Nice") }
            }
        }
    }
}

@Preview(name = "Compact light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun CompletionPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            CompletionScreen(
                state = sampleCompletionState(firstEver = true),
                showBadgeSheet = false,
                apertureProgress = 1f,
            )
        }
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
private fun CompletionPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface {
            CompletionScreen(
                state = sampleCompletionState(firstEver = false),
                showBadgeSheet = false,
                apertureProgress = 1f,
            )
        }
    }
}

@Preview(name = "Series light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun CompletionPreviewSeriesLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            CompletionScreen(
                state = sampleCompletionState(firstEver = false, seriesTitle = "A Week of Hands"),
                showBadgeSheet = false,
            )
        }
    }
}

internal fun sampleCompletionState(
    firstEver: Boolean,
    seriesTitle: String? = null,
): CompletionUiState = CompletionUiState(
    date = samplePrompt().date,
    prompt = samplePrompt(),
    streak = app.shutterup.domain.model.StreakState(
        current = if (firstEver) 1 else 15,
        longest = if (firstEver) 1 else 15,
        freezes = if (firstEver) 0 else 2,
        lastProcessedDate = samplePrompt().date,
    ),
    newBadgeIds = if (firstEver) listOf("first_light") else listOf("streak_7"),
    freezeEarned = !firstEver,
    previousStreak = if (firstEver) 0 else 14,
    firstEver = firstEver,
    seriesTitle = seriesTitle,
)
