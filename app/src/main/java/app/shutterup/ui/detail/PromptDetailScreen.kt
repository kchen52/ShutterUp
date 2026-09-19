package app.shutterup.ui.detail

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.shutterup.capture.CapturePrivateVideo
import app.shutterup.capture.TakePrivatePicture
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.MediaKind
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.ui.components.ConstraintCard
import app.shutterup.ui.components.Kicker
import app.shutterup.ui.components.LibraryTag
import app.shutterup.ui.components.ShootButton
import app.shutterup.ui.components.ThemeChip
import app.shutterup.ui.theme.ProvideThemeTint
import app.shutterup.ui.theme.ShutterUpTheme
import coil3.compose.AsyncImage
import java.io.File

/**
 * Prompt Detail: theme, constraint, library tag, shoot, thumbnail strip, gallery stub.
 */
@Composable
fun PromptDetailRoute(
    onBack: () -> Unit,
    onOpenCompletion: (CompletionNav) -> Unit,
    viewModel: PromptDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stillLauncher = rememberLauncherForActivityResult(TakePrivatePicture()) { ok ->
        viewModel.onCameraReturned(ok)
    }
    val videoLauncher = rememberLauncherForActivityResult(CapturePrivateVideo()) { ok ->
        viewModel.onCameraReturned(ok)
    }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.onGalleryPicked(uri) }
    LaunchedEffect(state.launch) {
        val launch = state.launch ?: return@LaunchedEffect
        if (launch.kind == MediaKind.VIDEO) {
            videoLauncher.launch(launch.uri)
        } else {
            stillLauncher.launch(launch.uri)
        }
        viewModel.consumeLaunch()
    }
    LaunchedEffect(state.pickGallery) {
        if (state.pickGallery) {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            viewModel.consumeGallery()
        }
    }
    LaunchedEffect(state.completion) {
        val nav = state.completion ?: return@LaunchedEffect
        onOpenCompletion(nav)
        viewModel.consumeCompletion()
    }
    PromptDetailScreen(
        state = state,
        onBack = onBack,
        onShoot = viewModel::requestStill,
        onRecord = viewModel::requestVideo,
        onReroll = viewModel::reroll,
        onSkip = viewModel::onSkipClicked,
        onSkipConfirm = viewModel::confirmSkip,
        onSkipDismiss = viewModel::dismissSkip,
        onRetake = viewModel::retakePending,
        onConfirm = viewModel::confirmPending,
        onTrimChange = viewModel::updateTrim,
        onTrimConfirm = viewModel::confirmTrim,
        onViewGallery = viewModel::viewInGallery,
        onSnackbarShown = viewModel::consumeSnackbar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun PromptDetailScreen(
    state: PromptDetailUiState,
    onBack: () -> Unit = {},
    onShoot: () -> Unit = {},
    onRecord: () -> Unit = {},
    onReroll: () -> Unit = {},
    onSkip: () -> Unit = {},
    onSkipConfirm: () -> Unit = {},
    onSkipDismiss: () -> Unit = {},
    onRetake: () -> Unit = {},
    onConfirm: () -> Unit = {},
    onTrimChange: (Long, Long) -> Unit = { _, _ -> },
    onTrimConfirm: () -> Unit = {},
    onViewGallery: () -> Unit = {},
    onSnackbarShown: () -> Unit = {},
) {
    val prompt = state.prompt
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(state.snackbar) {
        val message = state.snackbar ?: return@LaunchedEffect
        snackbarHost.showSnackbar(message)
        onSnackbarShown()
    }
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    ProvideThemeTint(theme = prompt?.theme.orEmpty(), darkTheme = dark) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text("Back") }
                    },
                )
            },
            bottomBar = {
                BottomAppBar {
                    ShootButton(onClick = onShoot)
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        TextButton(
                            onClick = onReroll,
                            enabled = prompt != null && !prompt.rerollUsed && state.isToday,
                        ) {
                            Text(if (prompt?.rerollUsed == true) "Rerolled" else "Reroll")
                        }
                        TextButton(onClick = onSkip, enabled = state.isToday) {
                            Text("Skip")
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHost) },
        ) { padding ->
            if (prompt == null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Your first photo goes here.", style = MaterialTheme.typography.bodyLarge)
                }
                return@Scaffold
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Kicker(text = dateKicker(state.date))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ThemeChip(label = prompt.theme, selected = true, onClick = null)
                    if (prompt.source == PromptSourceRef.LIBRARY) {
                        LibraryTag()
                    }
                }
                if (state.debugSource && prompt.source == PromptSourceRef.ON_DEVICE_AI) {
                    Kicker(text = "${prompt.theme} · FROM NANO")
                }
                Text(text = prompt.title, style = MaterialTheme.typography.displaySmall)
                Text(text = prompt.oneLiner, style = MaterialTheme.typography.bodyLarge)
                Kicker(text = "HOW TO APPROACH IT")
                Text(text = prompt.details, style = MaterialTheme.typography.bodyMedium)
                if (prompt.tips.isNotEmpty()) {
                    Kicker(text = "TIPS")
                    prompt.tips.forEach { tip ->
                        Text(text = "· $tip", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                prompt.constraint?.let { ConstraintCard(text = it) }
                Text(
                    text = state.remainingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "One reroll a day.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                ThumbnailStrip(entries = state.entries, pendingPath = state.pending?.path)
                if (state.pending != null) {
                    ReviewPanel(
                        pending = state.pending,
                        trim = state.trim,
                        trimming = state.trimming,
                        onRetake = onRetake,
                        onConfirm = onConfirm,
                        onTrimChange = onTrimChange,
                        onTrimConfirm = onTrimConfirm,
                    )
                }
                TextButton(onClick = onViewGallery) {
                    Text("VIEW IN GALLERY")
                }
                TextButton(onClick = onRecord, enabled = state.isToday) {
                    Text("Record")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (state.showSkipDialog) {
        val hasFreeze = state.streak.freezes > 0
        val body = if (hasFreeze) {
            "Today will count as skipped. One of your ${state.streak.freezes} freezes will keep your streak."
        } else {
            "Today will count as skipped. Your ${state.streak.current}-day streak ends unless a freeze covers it."
        }
        AlertDialog(
            onDismissRequest = onSkipDismiss,
            title = { Text("Skip today?") },
            text = { Text(body) },
            confirmButton = {
                TextButton(onClick = onSkipConfirm) { Text("Skip") }
            },
            dismissButton = {
                TextButton(onClick = onSkipDismiss) { Text("Keep going") }
            },
        )
    }
}

@Composable
private fun ThumbnailStrip(entries: List<Entry>, pendingPath: String?) {
    val paths = entries.map { it.thumbPath } + listOfNotNull(pendingPath)
    if (paths.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(paths) { path ->
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            ) {
                if (File(path).exists()) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = "Capture thumbnail",
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

@UnstableApi
@Composable
private fun ReviewPanel(
    pending: PendingCapture,
    trim: TrimUi?,
    trimming: Boolean,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
    onTrimChange: (Long, Long) -> Unit,
    onTrimConfirm: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        ) {
            if (pending.kind == MediaKind.VIDEO) {
                VideoPreview(path = pending.path)
            } else {
                AsyncImage(
                    model = File(pending.path),
                    contentDescription = "Review capture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        if (trim != null) {
            val duration = trim.durationMs.coerceAtLeast(1L).toFloat()
            val start = (trim.startMs / duration).coerceIn(0f, 1f)
            val end = (trim.endMs / duration).coerceIn(0f, 1f)
            RangeSlider(
                value = start..end,
                onValueChange = { range ->
                    onTrimChange(
                        (range.start * duration).toLong(),
                        (range.endInclusive * duration).toLong(),
                    )
                },
            )
            TextButton(onClick = onTrimConfirm, enabled = !trimming) {
                Text(if (trimming) "Trimming" else "Trim")
            }
        }
        Row {
            TextButton(onClick = onRetake) { Text("Retake") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onConfirm, enabled = !trimming) { Text("Confirm") }
        }
    }
}

@UnstableApi
@Composable
private fun VideoPreview(path: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(path) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
            prepare()
            playWhenReady = false
        }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
            }
        },
        modifier = Modifier.fillMaxSize(),
        onRelease = { player.release() },
    )
}

@Preview(name = "Compact light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PromptDetailPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            PromptDetailScreen(
                state = PromptDetailUiState(
                    date = samplePrompt().date,
                    prompt = samplePrompt(),
                    remainingLabel = "9 hours left today",
                    isToday = true,
                ),
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
private fun PromptDetailPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface {
            PromptDetailScreen(
                state = PromptDetailUiState(
                    date = samplePrompt().date,
                    prompt = samplePrompt(),
                    remainingLabel = "9 hours left today",
                    isToday = true,
                ),
            )
        }
    }
}

@Preview(name = "Font scale 2x", showBackground = true, widthDp = 360, heightDp = 1200, fontScale = 2f)
@Composable
private fun PromptDetailPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            PromptDetailScreen(
                state = PromptDetailUiState(
                    date = samplePrompt().date,
                    prompt = samplePrompt(PromptSourceRef.ON_DEVICE_AI),
                    remainingLabel = "12 minutes left today",
                    isToday = true,
                    debugSource = true,
                ),
            )
        }
    }
}
