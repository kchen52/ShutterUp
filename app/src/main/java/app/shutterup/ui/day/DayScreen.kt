package app.shutterup.ui.day

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.MediaKind
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.ui.adaptive.isExpandedWidth
import app.shutterup.ui.badges.BadgeEmblem
import app.shutterup.ui.components.ConstraintCard
import app.shutterup.ui.components.Kicker
import app.shutterup.ui.components.LibraryTag
import app.shutterup.ui.components.ShootButton
import app.shutterup.ui.detail.samplePrompt
import app.shutterup.domain.take.DiptychCrop
import app.shutterup.domain.take.TakeInterval
import app.shutterup.ui.theme.ProvideThemeTint
import app.shutterup.ui.theme.ShutterUpTheme
import coil3.compose.AsyncImage
import java.io.File
import java.time.Instant
import java.time.LocalDate

/**
 * Single-day view. Shoot / Retake open Prompt Detail; delete keeps the day complete.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun DayRoute(
    onBack: () -> Unit,
    onOpenDetail: (dateIso: String, autoLaunchCamera: Boolean) -> Unit,
    onOpenCompletion: (dateIso: String) -> Unit,
    onOpenDay: (dateIso: String) -> Unit = {},
    viewModel: DayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val expanded = isExpandedWidth(currentWindowAdaptiveInfo().windowSizeClass.minWidthDp)
    DayScreen(
        state = state,
        expanded = expanded,
        onBack = onBack,
        onShoot = { onOpenDetail(state.date.toString(), true) },
        onRetake = { onOpenDetail(state.date.toString(), true) },
        onOpenCompletion = { onOpenCompletion(state.date.toString()) },
        onOpenDay = onOpenDay,
        onNoteChange = viewModel::updateNote,
        onDelete = viewModel::onDeleteClicked,
        onShootAgain = viewModel::onShootAgainClicked,
        onConfirmSecondTake = viewModel::confirmSecondTake,
        onDismissSecondTake = viewModel::dismissSecondTake,
        onDeleteShutterUp = { viewModel.confirmDelete(alsoGallery = false) },
        onDeleteGallery = { viewModel.confirmDelete(alsoGallery = true) },
        onDeleteDismiss = viewModel::dismissDelete,
        onSnackbarShown = viewModel::consumeSnackbar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen(
    state: DayUiState,
    expanded: Boolean = false,
    onBack: () -> Unit = {},
    onShoot: () -> Unit = {},
    onRetake: () -> Unit = {},
    onOpenCompletion: () -> Unit = {},
    onOpenDay: (String) -> Unit = {},
    onNoteChange: (String) -> Unit = {},
    onDelete: () -> Unit = {},
    onShootAgain: () -> Unit = {},
    onConfirmSecondTake: () -> Unit = {},
    onDismissSecondTake: () -> Unit = {},
    onDeleteShutterUp: () -> Unit = {},
    onDeleteGallery: () -> Unit = {},
    onDeleteDismiss: () -> Unit = {},
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
            snackbarHost = { SnackbarHost(snackbarHost) },
        ) { padding ->
            if (prompt == null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Nothing saved for this day.", style = MaterialTheme.typography.bodyLarge)
                }
                return@Scaffold
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                var viewerEntry by remember { mutableStateOf<Entry?>(null) }
                if (state.diptych != null) {
                    TakeDiptych(
                        ui = state.diptych,
                        expanded = expanded,
                        onOpenPhoto = { viewerEntry = it },
                        onOpenDay = onOpenDay,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                } else {
                    PhotoHeader(
                        entries = state.entries,
                        originalMissing = state.originalMissing ||
                            prompt.status == DayStatus.COMPLETED_NO_PHOTO,
                        onOpenPhoto = { viewerEntry = it },
                    )
                }
                viewerEntry?.thumbPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        Dialog(
                            onDismissRequest = { viewerEntry = null },
                            properties = DialogProperties(usePlatformDefaultWidth = false),
                        ) {
                            PinchZoomViewer(file = file, onDismiss = { viewerEntry = null })
                        }
                    }
                }
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Kicker(text = "${statusLabel(prompt.status)} · ${prompt.theme}")
                        if (prompt.source == PromptSourceRef.LIBRARY) {
                            LibraryTag()
                        }
                    }
                    if (state.badgeIds.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.badgeIds.forEach { id ->
                                BadgeEmblem(badgeId = id, unlocked = true, size = 32.dp)
                            }
                        }
                    }
                    Text(
                        text = prompt.title,
                        style = MaterialTheme.typography.displaySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
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
                    state.freezeLine?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.laterTakesLine?.let { line ->
                        Text(
                            text = line,
                            modifier = Modifier.clickable(
                                enabled = state.laterTakeDateIso != null,
                            ) {
                                state.laterTakeDateIso?.let(onOpenDay)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = onNoteChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Add a note…") },
                        minLines = 2,
                        maxLines = 4,
                    )
                    val canShoot = state.isToday && prompt.status == DayStatus.PENDING
                    val canRetake = state.isToday &&
                        (prompt.status == DayStatus.COMPLETED || prompt.status == DayStatus.COMPLETED_NO_PHOTO)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.entries.isNotEmpty()) {
                            TextButton(onClick = onDelete) { Text("Delete") }
                        }
                        if (state.canSecondTake) {
                            TextButton(onClick = onShootAgain) { Text("Shoot this again") }
                        }
                        Spacer(Modifier.weight(1f))
                        if (canRetake) {
                            TextButton(onClick = onRetake) { Text("Retake") }
                            TextButton(onClick = onOpenCompletion) { Text("Done") }
                        }
                    }
                    if (canShoot) {
                        ShootButton(onClick = onShoot)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text("Delete this photo?") },
            text = { Text("The day stays complete.") },
            confirmButton = {
                TextButton(onClick = onDeleteShutterUp) { Text("Delete from ShutterUp") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onDeleteGallery) { Text("Also delete from Gallery") }
                    TextButton(onClick = onDeleteDismiss) { Text("Cancel") }
                }
            },
        )
    }
    if (state.showSecondTakeDialog && state.secondTakeConfirmBody != null) {
        AlertDialog(
            onDismissRequest = onDismissSecondTake,
            title = { Text("Shoot this again?") },
            text = { Text(state.secondTakeConfirmBody) },
            confirmButton = {
                TextButton(onClick = onConfirmSecondTake) { Text("Shoot this again") }
            },
            dismissButton = {
                TextButton(onClick = onDismissSecondTake) { Text("Not now") }
            },
        )
    }
}

@Composable
private fun PhotoHeader(
    entries: List<Entry>,
    originalMissing: Boolean,
    onOpenPhoto: (Entry) -> Unit,
) {
    val first = entries.firstOrNull()
    val file = first?.thumbPath?.let(::File)
    val hasFile = file != null && file.exists()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
    ) {
        if (first != null && hasFile && !originalMissing) {
            AsyncImage(
                model = file,
                contentDescription = "Day photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .clickable { onOpenPhoto(first) }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom > 1.02f) onOpenPhoto(first)
                        }
                    },
                contentScale = ContentScale.Fit,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.BrokenImage,
                    contentDescription = "Original missing",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    modifier = Modifier.padding(top = 12.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = "Original missing",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun PinchZoomViewer(file: File, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.9f))
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale <= 1f && zoom < 1f) onDismiss()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = file,
            contentDescription = "Day photo",
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale },
            contentScale = ContentScale.Fit,
        )
    }
}

private fun statusLabel(status: DayStatus): String = when (status) {
    DayStatus.PENDING -> "PENDING"
    DayStatus.COMPLETED, DayStatus.COMPLETED_NO_PHOTO -> "COMPLETED"
    DayStatus.SKIPPED -> "SKIPPED"
    DayStatus.MISSED -> "MISSED"
    DayStatus.PAUSED -> "PAUSED"
}

/** Deterministic Day state for previews and Roborazzi. */
fun sampleDayState(
    status: DayStatus = DayStatus.COMPLETED,
    originalMissing: Boolean = false,
    frozen: Boolean = false,
): DayUiState {
    val prompt = samplePrompt().copy(status = status, frozen = frozen)
    val entry = Entry(
        id = 1,
        date = prompt.date,
        mediaUri = "file:///tmp/missing.jpg",
        thumbPath = "/tmp/missing.jpg",
        capturedAt = Instant.parse("2026-09-19T10:00:00Z"),
        width = 1200,
        height = 1600,
        note = null,
        importedFromGallery = false,
        createdAt = Instant.parse("2026-09-19T10:00:00Z"),
        mediaKind = MediaKind.PHOTO,
    )
    return DayUiState(
        date = prompt.date,
        prompt = prompt,
        entries = if (status == DayStatus.COMPLETED || status == DayStatus.COMPLETED_NO_PHOTO) {
            listOf(entry)
        } else {
            emptyList()
        },
        badgeIds = listOf("first_light"),
        isToday = true,
        originalMissing = originalMissing,
        freezeLine = if (frozen) "A freeze kept your streak on 17 September." else null,
    )
}

fun sampleDiptychState(
    threeTakes: Boolean = false,
    mixedAspect: Boolean = false,
): DayUiState {
    val firstDate = LocalDate.of(2026, 6, 28)
    val secondDate = LocalDate.of(2026, 9, 19)
    val thirdDate = LocalDate.of(2026, 12, 11)
    val viewed = if (threeTakes) thirdDate else secondDate
    val previous = if (threeTakes) secondDate else firstDate
    val prompt = samplePrompt().copy(
        date = viewed,
        status = DayStatus.COMPLETED,
        repeatsDate = firstDate,
    )
    val firstEntry = sampleTakeEntry(
        date = previous,
        width = 1200,
        height = 1600,
    )
    val secondEntry = sampleTakeEntry(
        date = viewed,
        width = if (mixedAspect) 1600 else 1200,
        height = if (mixedAspect) 1200 else 1600,
    )
    val crop = if (mixedAspect) DiptychCrop.SQUARE else DiptychCrop.NATIVE
    return DayUiState(
        date = viewed,
        prompt = prompt,
        entries = listOf(secondEntry),
        badgeIds = emptyList(),
        isToday = false,
        originalMissing = false,
        canSecondTake = true,
        laterTakesLine = null,
        diptych = DiptychUi(
            first = DiptychFrame(
                date = previous,
                kicker = TakeInterval.dateKicker(previous),
                entry = firstEntry,
                originalMissing = false,
            ),
            second = DiptychFrame(
                date = viewed,
                kicker = TakeInterval.phrase(previous, viewed),
                entry = secondEntry,
                originalMissing = false,
            ),
            crop = crop,
            rest = if (threeTakes) listOf(firstDate) else emptyList(),
        ),
    )
}

private fun sampleTakeEntry(
    date: LocalDate,
    width: Int,
    height: Int,
): Entry = Entry(
    id = date.toEpochDay(),
    date = date,
    mediaUri = "file:///tmp/missing-$date.jpg",
    thumbPath = "/tmp/missing-$date.jpg",
    capturedAt = Instant.parse("2026-09-19T10:00:00Z"),
    width = width,
    height = height,
    note = null,
    importedFromGallery = false,
    createdAt = Instant.parse("2026-09-19T10:00:00Z"),
    mediaKind = MediaKind.PHOTO,
)

@Preview(name = "Compact light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun DayPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface { DayScreen(state = sampleDayState()) }
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
private fun DayPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface { DayScreen(state = sampleDayState(status = DayStatus.MISSED, frozen = true)) }
    }
}

@Preview(name = "Diptych compact light", showBackground = true, widthDp = 360, heightDp = 1100)
@Composable
private fun DayDiptychPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface { DayScreen(state = sampleDiptychState()) }
    }
}

@Preview(
    name = "Diptych compact dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 1100,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DayDiptychPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface { DayScreen(state = sampleDiptychState()) }
    }
}

@Preview(name = "Diptych expanded", showBackground = true, widthDp = 840, heightDp = 900)
@Composable
private fun DayDiptychPreviewExpanded() {
    ShutterUpTheme(darkTheme = false) {
        Surface { DayScreen(state = sampleDiptychState(), expanded = true) }
    }
}

@Preview(
    name = "Diptych font scale 2x",
    showBackground = true,
    widthDp = 360,
    heightDp = 1400,
    fontScale = 2f,
)
@Composable
private fun DayDiptychPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        Surface { DayScreen(state = sampleDiptychState()) }
    }
}

@Preview(name = "Diptych mixed aspect", showBackground = true, widthDp = 360, heightDp = 1100)
@Composable
private fun DayDiptychPreviewMixedAspect() {
    ShutterUpTheme(darkTheme = false) {
        Surface { DayScreen(state = sampleDiptychState(mixedAspect = true)) }
    }
}

@Preview(name = "Diptych three takes", showBackground = true, widthDp = 360, heightDp = 1200)
@Composable
private fun DayDiptychPreviewThreeTakes() {
    ShutterUpTheme(darkTheme = false) {
        Surface { DayScreen(state = sampleDiptychState(threeTakes = true)) }
    }
}
