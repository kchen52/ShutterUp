package app.shutterup.ui.detail

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shutterup.capture.TakePrivatePicture
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.ui.components.ConstraintCard
import app.shutterup.ui.components.Kicker
import app.shutterup.ui.components.LibraryTag
import app.shutterup.ui.components.SeriesDots
import app.shutterup.ui.components.ShootButton
import app.shutterup.ui.settings.SettingsCopy
import app.shutterup.ui.theme.ProvideThemeTint
import app.shutterup.ui.theme.ShutterUpTheme
import java.util.Locale

/**
 * Prompt Detail: theme, constraint, library tag, shoot (SPEC §4.2 / DESIGN §4.2).
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
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.onGalleryPicked(uri) }
    LaunchedEffect(state.launch) {
        val launch = state.launch ?: return@LaunchedEffect
        stillLauncher.launch(launch.uri)
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
        onReroll = viewModel::reroll,
        onSkip = viewModel::onSkipClicked,
        onSkipConfirm = viewModel::confirmSkip,
        onSkipDismiss = viewModel::dismissSkip,
        onChooseGallery = viewModel::chooseFromGallery,
        onRetryStorage = viewModel::retryStorage,
        onDismissStorage = viewModel::dismissStorageDialog,
        onSnackbarShown = viewModel::consumeSnackbar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptDetailScreen(
    state: PromptDetailUiState,
    onBack: () -> Unit = {},
    onShoot: () -> Unit = {},
    onReroll: () -> Unit = {},
    onSkip: () -> Unit = {},
    onSkipConfirm: () -> Unit = {},
    onSkipDismiss: () -> Unit = {},
    onChooseGallery: () -> Unit = {},
    onRetryStorage: () -> Unit = {},
    onDismissStorage: () -> Unit = {},
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
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                )
            },
            bottomBar = {
                Column {
                    BottomAppBar {
                        ShootButton(onClick = onShoot)
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = onReroll,
                            enabled = prompt != null && !prompt.rerollUsed && state.isToday,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(if (prompt?.rerollUsed == true) "Rerolled" else "Reroll")
                        }
                        TextButton(
                            onClick = onSkip,
                            enabled = state.isToday,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text("Skip")
                        }
                    }
                    if (state.offerGallery) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = PromptDetailViewModel.COPY_GALLERY_CARD,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Button(onClick = onChooseGallery) {
                                    Text(PromptDetailViewModel.COPY_CHOOSE_GALLERY)
                                }
                            }
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
                    Text(SettingsCopy.AI_PREPARING, style = MaterialTheme.typography.bodyLarge)
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
                val series = state.seriesProgress
                if (series != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Kicker(
                            text = series.detailKicker(prompt.theme),
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (prompt.source == PromptSourceRef.LIBRARY) {
                            LibraryTag()
                        }
                    }
                    SeriesDots(dots = series.dots)
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Kicker(text = prompt.theme.uppercase(Locale.ENGLISH))
                        if (prompt.source == PromptSourceRef.LIBRARY) {
                            LibraryTag()
                        }
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
    if (state.showStorageDialog) {
        AlertDialog(
            onDismissRequest = onDismissStorage,
            text = { Text(PromptDetailViewModel.COPY_STORAGE_BODY) },
            confirmButton = {
                TextButton(onClick = onRetryStorage) {
                    Text(PromptDetailViewModel.COPY_TRY_AGAIN)
                }
            },
        )
    }
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
                    offerGallery = true,
                ),
            )
        }
    }
}

@Preview(name = "Series font scale 2x", showBackground = true, widthDp = 360, heightDp = 1600, fontScale = 2f)
@Composable
private fun PromptDetailPreviewSeriesFontScale() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            PromptDetailScreen(
                state = PromptDetailUiState(
                    date = samplePrompt().date,
                    prompt = samplePrompt(),
                    remainingLabel = "9 hours left today",
                    isToday = true,
                    seriesProgress = app.shutterup.ui.home.sampleSeriesProgress(),
                ),
            )
        }
    }
}

@Preview(name = "Series light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PromptDetailPreviewSeriesLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            PromptDetailScreen(
                state = PromptDetailUiState(
                    date = samplePrompt().date,
                    prompt = samplePrompt(),
                    remainingLabel = "9 hours left today",
                    isToday = true,
                    seriesProgress = app.shutterup.ui.home.sampleSeriesProgress(),
                ),
            )
        }
    }
}

@Preview(
    name = "Series dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PromptDetailPreviewSeriesDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface {
            PromptDetailScreen(
                state = PromptDetailUiState(
                    date = samplePrompt().date,
                    prompt = samplePrompt(),
                    remainingLabel = "9 hours left today",
                    isToday = true,
                    seriesProgress = app.shutterup.ui.home.sampleSeriesProgress(),
                ),
            )
        }
    }
}
