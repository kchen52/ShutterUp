package app.shutterup.ui.completion

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.ui.badges.BadgeEmblem
import app.shutterup.ui.badges.badgeDescription
import app.shutterup.ui.badges.badgeDisplayName
import app.shutterup.ui.components.Kicker
import app.shutterup.ui.components.LibraryTag
import app.shutterup.ui.detail.samplePrompt
import app.shutterup.ui.theme.ProvideThemeTint
import app.shutterup.ui.theme.ShutterUpTheme
import coil3.compose.AsyncImage
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

/**
 * Completion: photo, note, streak roll, badge sheet, SHARE / STAY / Retake.
 */
@Composable
fun CompletionRoute(
    onStay: () -> Unit,
    onRetake: () -> Unit,
    viewModel: CompletionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    CompletionScreen(
        state = state,
        onNoteChange = viewModel::updateNote,
        onStay = {
            viewModel.updateNote(state.note)
            onStay()
        },
        onShare = {
            val uri = state.entries.lastOrNull()?.mediaUri?.let(Uri::parse)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "SHARE"))
        },
        onRetake = onRetake,
        showConfetti = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletionScreen(
    state: CompletionUiState,
    onNoteChange: (String) -> Unit = {},
    onStay: () -> Unit = {},
    onShare: () -> Unit = {},
    onRetake: () -> Unit = {},
    showConfetti: Boolean = false,
    showBadgeSheet: Boolean = true,
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
    ProvideThemeTint(theme = prompt?.theme.orEmpty(), darkTheme = dark) {
        Box(Modifier.fillMaxSize()) {
            if (showConfetti) {
                ConfettiCanvas(modifier = Modifier.fillMaxSize())
            }
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
                        .fillMaxWidth(0.85f)
                        .heightIn(max = 280.dp)
                        .scale(if (showConfetti) photoScale else 1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                ) {
                    if (thumb != null && File(thumb).exists()) {
                        AsyncImage(
                            model = File(thumb),
                            contentDescription = "Today's photo",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop,
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
                val kicker = buildString {
                    append(prompt?.theme?.uppercase().orEmpty())
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
                Row(verticalAlignment = Alignment.Bottom) {
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
                    if (state.streak.freezes > 0) {
                        Text(
                            text = "  ${state.streak.freezes}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (state.freezeEarned) {
                    Text(
                        text = "You earned a freeze. It'll cover one missed day.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (state.newBadgeIds.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        state.newBadgeIds.take(4).forEach { id ->
                            BadgeEmblem(badgeId = id, unlocked = true, size = 56.dp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Button(onClick = {
                        onNoteChange(note)
                        onStay()
                    }) { Text("STAY") }
                    TextButton(onClick = onShare) { Text("SHARE") }
                    TextButton(onClick = onRetake) { Text("Retake") }
                }
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

@Composable
fun ConfettiCanvas(modifier: Modifier = Modifier) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        for (i in 0 until 24) {
            val angle = i * 0.37f
            val x = w * (0.5f + 0.4f * cos(angle * 3))
            val y = h * (0.15f + (i % 7) * 0.08f)
            drawCircle(
                color = colors[i % colors.size].copy(alpha = 0.55f),
                radius = 6f + (i % 4) * 2f,
                center = Offset(x, y),
            )
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
                showConfetti = true,
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
                showConfetti = true,
            )
        }
    }
}

internal fun sampleCompletionState(firstEver: Boolean): CompletionUiState = CompletionUiState(
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
)
