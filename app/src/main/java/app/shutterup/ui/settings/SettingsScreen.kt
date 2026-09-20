package app.shutterup.ui.settings

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import app.shutterup.data.backup.ProgressBackupFormat
import app.shutterup.domain.geo.CityCatalog
import app.shutterup.ui.components.Kicker
import app.shutterup.ui.icons.SnowflakeIcon
import app.shutterup.ui.theme.ShutterUpTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Settings route. No [androidx.navigation.NavController]: privacy is a callback
 * so worker A can push a destination or show a sheet.
 */
@Composable
fun SettingsRoute(
    onOpenPrivacy: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ProgressBackupFormat.MIME),
    ) { uri -> viewModel.exportTo(uri) }
    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> viewModel.importFrom(uri) }
    SettingsScreen(
        state = state,
        onNotifyTime = viewModel::setNotifyTime,
        onPreciseTiming = viewModel::setPreciseTiming,
        onPaused = viewModel::setPaused,
        onSeriesEnabled = viewModel::setSeriesEnabled,
        onCoarseCity = viewModel::setCoarseCityId,
        onDebugUseFakeAi = viewModel::setDebugUseFakeAi,
        onForceRollover = viewModel::forceDayRollover,
        onSeedHistory = viewModel::seedSixtyDays,
        onResetAll = viewModel::resetAllData,
        onBackup = { createBackup.launch(viewModel.suggestedBackupName()) },
        onRestore = {
            openBackup.launch(
                arrayOf(
                    ProgressBackupFormat.MIME,
                    "application/x-zip-compressed",
                    "application/octet-stream",
                ),
            )
        },
        onSnackbarShown = viewModel::consumeSnackbar,
        onOpenPrivacy = onOpenPrivacy,
        onOpenExactAlarmSettings = {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        },
        onOpenBatterySettings = {
            val pkg = context.packageName
            val appBattery = Intent("android.settings.APP_BATTERY_SETTINGS").apply {
                data = Uri.parse("package:$pkg")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val launched = runCatching { context.startActivity(appBattery) }.isSuccess
            if (!launched) {
                context.startActivity(
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }
        },
    )
}

private enum class SettingsCategory {
    Daily,
    Prompts,
    Photos,
    About,
    Debug,
}

/**
 * Settings list (DESIGN.md §4.8). Compact: one column. Expanded: category list
 * plus the selected group (SPEC §5).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    modifier: Modifier = Modifier,
    onNotifyTime: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    onPreciseTiming: (Boolean) -> Unit = {},
    onPaused: (Boolean) -> Unit = {},
    onSeriesEnabled: (Boolean) -> Unit = {},
    onCoarseCity: (String?) -> Unit = {},
    onDebugUseFakeAi: (Boolean) -> Unit = {},
    onForceRollover: () -> Unit = {},
    onSeedHistory: () -> Unit = {},
    onResetAll: () -> Unit = {},
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
    onSnackbarShown: () -> Unit = {},
    onOpenPrivacy: (() -> Unit)? = null,
    onOpenExactAlarmSettings: () -> Unit = {},
    onOpenBatterySettings: () -> Unit = {},
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var category by remember { mutableStateOf(SettingsCategory.Daily) }
    val widthDp = currentWindowAdaptiveInfo().windowSizeClass.minWidthDp
    val twoPane = widthDp >= WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(state.snackbar) {
        val message = state.snackbar ?: return@LaunchedEffect
        snackbarHost.showSnackbar(message)
        onSnackbarShown()
    }
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                scrollBehavior = scroll,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        if (twoPane) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                CategoryList(
                    selected = category,
                    showDebug = state.showDebug,
                    onSelect = { category = it },
                    modifier = Modifier
                        .width(280.dp)
                        .verticalScroll(rememberScrollState()),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    SettingsGroup(
                        category = category,
                        state = state,
                        onNotifyTime = onNotifyTime,
                        onPreciseTiming = onPreciseTiming,
                        onPaused = onPaused,
                        onSeriesEnabled = onSeriesEnabled,
                        onCoarseCity = onCoarseCity,
                        onDebugUseFakeAi = onDebugUseFakeAi,
                        onForceRollover = onForceRollover,
                        onSeedHistory = onSeedHistory,
                        onResetAll = onResetAll,
                        onBackup = onBackup,
                        onRestore = onRestore,
                        onOpenPrivacy = onOpenPrivacy,
                        onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                        onOpenBatterySettings = onOpenBatterySettings,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            ) {
                val categories = buildList {
                    add(SettingsCategory.Daily)
                    add(SettingsCategory.Prompts)
                    add(SettingsCategory.Photos)
                    add(SettingsCategory.About)
                    if (state.showDebug) add(SettingsCategory.Debug)
                }
                categories.forEach { item ->
                    SettingsGroup(
                        category = item,
                        state = state,
                        onNotifyTime = onNotifyTime,
                        onPreciseTiming = onPreciseTiming,
                        onPaused = onPaused,
                        onSeriesEnabled = onSeriesEnabled,
                        onCoarseCity = onCoarseCity,
                        onDebugUseFakeAi = onDebugUseFakeAi,
                        onForceRollover = onForceRollover,
                        onSeedHistory = onSeedHistory,
                        onResetAll = onResetAll,
                        onBackup = onBackup,
                        onRestore = onRestore,
                        onOpenPrivacy = onOpenPrivacy,
                        onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                        onOpenBatterySettings = onOpenBatterySettings,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryList(
    selected: SettingsCategory,
    showDebug: Boolean,
    onSelect: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = buildList {
        add(SettingsCategory.Daily)
        add(SettingsCategory.Prompts)
        add(SettingsCategory.Photos)
        add(SettingsCategory.About)
        if (showDebug) add(SettingsCategory.Debug)
    }
    Column(modifier = modifier) {
        categories.forEach { item ->
            val label = categoryLabel(item)
            ListItem(
                headlineContent = {
                    Text(
                        text = label,
                        color = if (item == selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable { onSelect(item) },
            )
        }
    }
}

private fun categoryLabel(category: SettingsCategory): String = when (category) {
    SettingsCategory.Daily -> SettingsCopy.SECTION_DAILY
    SettingsCategory.Prompts -> SettingsCopy.SECTION_PROMPTS
    SettingsCategory.Photos -> SettingsCopy.SECTION_PHOTOS
    SettingsCategory.About -> SettingsCopy.SECTION_ABOUT
    SettingsCategory.Debug -> SettingsCopy.SECTION_DEBUG
}

@Composable
private fun SettingsGroup(
    category: SettingsCategory,
    state: SettingsUiState,
    onNotifyTime: (Int, Int) -> Unit,
    onPreciseTiming: (Boolean) -> Unit,
    onPaused: (Boolean) -> Unit,
    onSeriesEnabled: (Boolean) -> Unit,
    onCoarseCity: (String?) -> Unit,
    onDebugUseFakeAi: (Boolean) -> Unit,
    onForceRollover: () -> Unit,
    onSeedHistory: () -> Unit,
    onResetAll: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onOpenPrivacy: (() -> Unit)?,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
) {
    when (category) {
        SettingsCategory.Daily -> DailyPromptSection(
            state = state,
            onNotifyTime = onNotifyTime,
            onPreciseTiming = onPreciseTiming,
            onPaused = onPaused,
            onOpenExactAlarmSettings = onOpenExactAlarmSettings,
            onOpenBatterySettings = onOpenBatterySettings,
        )
        SettingsCategory.Prompts -> PromptsSection(
            state = state,
            onSeriesEnabled = onSeriesEnabled,
            onCoarseCity = onCoarseCity,
        )
        SettingsCategory.Photos -> PhotosSection(
            state = state,
            onBackup = onBackup,
            onRestore = onRestore,
        )
        SettingsCategory.About -> AboutSection(state = state, onOpenPrivacy = onOpenPrivacy)
        SettingsCategory.Debug -> DebugSection(
            state = state,
            onDebugUseFakeAi = onDebugUseFakeAi,
            onForceRollover = onForceRollover,
            onSeedHistory = onSeedHistory,
            onResetAll = onResetAll,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyPromptSection(
    state: SettingsUiState,
    onNotifyTime: (Int, Int) -> Unit,
    onPreciseTiming: (Boolean) -> Unit,
    onPaused: (Boolean) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val timeLabel = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(state.notifyTime)
    SectionHeader(SettingsCopy.SECTION_DAILY)
    ListItem(
        headlineContent = { Text(SettingsCopy.NOTIFICATION_TIME) },
        supportingContent = { Text(timeLabel) },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable { showPicker = true },
    )
    SwitchRow(
        title = SettingsCopy.PRECISE_TIMING,
        checked = state.preciseTiming,
        onCheckedChange = onPreciseTiming,
        supporting = SettingsCopy.PRECISE_SUPPORTING,
    )
    if (state.preciseTiming) {
        ListItem(
            headlineContent = { Text(state.preciseTimingStatus) },
            modifier = Modifier
                .heightIn(min = 48.dp)
                .then(
                    if (!state.exactAlarmAllowed) {
                        Modifier.clickable(onClick = onOpenExactAlarmSettings)
                    } else {
                        Modifier
                    },
                ),
            supportingContent = if (!state.exactAlarmAllowed) {
                { Text(SettingsCopy.OPEN_SETTINGS) }
            } else {
                null
            },
        )
    }
    SwitchRow(
        title = SettingsCopy.PAUSE,
        checked = state.paused,
        onCheckedChange = onPaused,
        supporting = SettingsCopy.PAUSE_SUPPORTING,
    )
    val freezeLabel = if (state.freezeCount == 1) {
        "1 freeze held"
    } else {
        "${state.freezeCount} freezes held"
    }
    ListItem(
        headlineContent = { Text(SettingsCopy.FREEZES) },
        supportingContent = { Text(freezeLabel) },
        modifier = Modifier.semantics { contentDescription = freezeLabel },
        leadingContent = {
            Icon(
                imageVector = SnowflakeIcon,
                contentDescription = freezeLabel,
                tint = MaterialTheme.colorScheme.tertiary,
            )
        },
    )
    if (state.showBatteryHint) {
        ListItem(
            headlineContent = { Text(SettingsCopy.BATTERY_HINT) },
            supportingContent = { Text(SettingsCopy.BATTERY_ACTION) },
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(onClick = onOpenBatterySettings),
        )
    }
    if (showPicker) {
        val pickerState = rememberTimePickerState(
            initialHour = state.notifyTime.hour,
            initialMinute = state.notifyTime.minute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onNotifyTime(pickerState.hour, pickerState.minute)
                        showPicker = false
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPicker = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Cancel")
                }
            },
            text = { TimePicker(state = pickerState) },
        )
    }
}

@Composable
private fun PromptsSection(
    state: SettingsUiState,
    onSeriesEnabled: (Boolean) -> Unit,
    onCoarseCity: (String?) -> Unit,
) {
    var showCities by remember { mutableStateOf(false) }
    SectionHeader(SettingsCopy.SECTION_PROMPTS)
    SwitchRow(
        title = SettingsCopy.SERIES,
        checked = state.seriesEnabled,
        onCheckedChange = onSeriesEnabled,
        supporting = if (state.seriesEnabled) {
            SettingsCopy.SERIES_SUPPORTING_ON
        } else {
            SettingsCopy.SERIES_SUPPORTING
        },
    )
    ListItem(
        headlineContent = { Text(SettingsCopy.WHERE_YOU_ARE) },
        supportingContent = {
            Column {
                Text(state.coarseCityName ?: SettingsCopy.WHERE_YOU_ARE_UNSET)
                Text(SettingsCopy.WHERE_YOU_ARE_SUPPORTING)
            }
        },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable { showCities = true },
    )
    ListItem(
        headlineContent = { Text(SettingsCopy.LIBRARY_STATUS) },
        supportingContent = { Text(SettingsCopy.LIBRARY_SUPPORTING) },
    )
    if (showCities) {
        CityPickerDialog(
            selectedId = state.coarseCityId,
            onSelect = { id ->
                onCoarseCity(id)
                showCities = false
            },
            onDismiss = { showCities = false },
        )
    }
}

@Composable
private fun CityPickerDialog(
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Close")
            }
        },
        title = { Text(SettingsCopy.WHERE_YOU_ARE) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                val unsetSelected = selectedId == null
                ListItem(
                    headlineContent = {
                        Text(
                            text = SettingsCopy.WHERE_YOU_ARE_UNSET,
                            color = if (unsetSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable { onSelect(null) },
                )
                CityCatalog.cities.forEach { city ->
                    val selected = city.id == selectedId
                    ListItem(
                        headlineContent = {
                            Text(
                                text = city.name,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clickable { onSelect(city.id) },
                    )
                }
            }
        },
    )
}

@Composable
private fun PhotosSection(
    state: SettingsUiState,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    var confirmRestore by remember { mutableStateOf(false) }
    SectionHeader(SettingsCopy.SECTION_PHOTOS)
    ListItem(
        headlineContent = { Text(SettingsCopy.SAVE_LOCATION_LABEL) },
        supportingContent = { Text(state.saveLocation) },
    )
    ListItem(
        headlineContent = { Text(SettingsCopy.STORAGE_USED) },
        supportingContent = { Text(state.storageUsed) },
    )
    ListItem(
        headlineContent = { Text(SettingsCopy.BACKUP) },
        supportingContent = { Text(SettingsCopy.BACKUP_SUPPORTING) },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onBackup),
    )
    ListItem(
        headlineContent = { Text(SettingsCopy.RESTORE) },
        supportingContent = { Text(SettingsCopy.RESTORE_SUPPORTING) },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable { confirmRestore = true },
    )
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRestore = false
                        onRestore()
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(SettingsCopy.RESTORE_CONFIRM)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmRestore = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Cancel")
                }
            },
            title = { Text(SettingsCopy.RESTORE_TITLE) },
            text = { Text(SettingsCopy.RESTORE_BODY) },
        )
    }
}

@Composable
private fun AboutSection(
    state: SettingsUiState,
    onOpenPrivacy: (() -> Unit)?,
) {
    var showLicences by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    SectionHeader(SettingsCopy.SECTION_ABOUT)
    ListItem(
        headlineContent = { Text(SettingsCopy.VERSION) },
        supportingContent = { Text(state.versionName.ifBlank { "0.1.0" }) },
    )
    ListItem(
        headlineContent = { Text(state.aboutLine) },
    )
    ListItem(
        headlineContent = { Text(SettingsCopy.PRIVACY) },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable {
                if (onOpenPrivacy != null) onOpenPrivacy() else showPrivacy = true
            },
    )
    ListItem(
        headlineContent = { Text(SettingsCopy.LICENCES) },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable { showLicences = true },
    )
    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            confirmButton = {
                TextButton(
                    onClick = { showPrivacy = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Close")
                }
            },
            title = { Text(SettingsCopy.PRIVACY) },
            text = { Text(state.privacyBody) },
        )
    }
    if (showLicences) {
        AlertDialog(
            onDismissRequest = { showLicences = false },
            confirmButton = {
                TextButton(
                    onClick = { showLicences = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Close")
                }
            },
            title = { Text(SettingsCopy.LICENCES) },
            text = { Text(SettingsCopy.LICENCES_BODY) },
        )
    }
}

@Composable
private fun DebugSection(
    state: SettingsUiState,
    onDebugUseFakeAi: (Boolean) -> Unit,
    onForceRollover: () -> Unit,
    onSeedHistory: () -> Unit,
    onResetAll: () -> Unit,
) {
    SectionHeader(SettingsCopy.SECTION_DEBUG)
    SwitchRow(
        title = SettingsCopy.DEBUG_FAKE_AI,
        checked = state.debugUseFakeAi,
        onCheckedChange = onDebugUseFakeAi,
    )
    ListItem(
        headlineContent = { Text(SettingsCopy.DEBUG_FORCE_ROLLOVER) },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onForceRollover),
    )
    ListItem(
        headlineContent = { Text(SettingsCopy.DEBUG_SEED) },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onSeedHistory),
    )
    ListItem(
        headlineContent = { Text(SettingsCopy.DEBUG_RESET) },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onResetAll),
    )
}

@Composable
private fun SectionHeader(title: String) {
    Kicker(
        text = title,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supporting: String? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = null)
        },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    )
}

internal fun sampleSettingsState(
    coarseCityId: String? = null,
    coarseCityName: String? = null,
    seriesEnabled: Boolean = false,
): SettingsUiState {
    return SettingsUiState(
        notifyTime = LocalTime.of(9, 0),
        preciseTiming = false,
        exactAlarmAllowed = true,
        paused = false,
        themeFocus = "",
        seriesEnabled = seriesEnabled,
        freezeCount = 2,
        versionName = "0.1.0",
        showDebug = true,
        debugUseFakeAi = false,
        showBatteryHint = false,
        storageUsed = "12 MB",
        coarseCityId = coarseCityId,
        coarseCityName = coarseCityName,
    )
}

@Preview(name = "Compact light", widthDp = 400, heightDp = 900)
@Composable
private fun SettingsPreviewCompactLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface { SettingsScreen(state = sampleSettingsState()) }
    }
}

@Preview(name = "Compact dark", widthDp = 400, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsPreviewCompactDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface { SettingsScreen(state = sampleSettingsState()) }
    }
}

@Preview(name = "Expanded light", widthDp = 1000, heightDp = 800)
@Composable
private fun SettingsPreviewExpandedLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface { SettingsScreen(state = sampleSettingsState()) }
    }
}

@Preview(name = "FontScale 2x", widthDp = 400, heightDp = 1200, fontScale = 2f)
@Composable
private fun SettingsPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        Surface { SettingsScreen(state = sampleSettingsState()) }
    }
}

@Preview(name = "City set", widthDp = 400, heightDp = 900)
@Composable
private fun SettingsPreviewCitySet() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            SettingsScreen(state = sampleSettingsState(coarseCityId = "sydney", coarseCityName = "Sydney"))
        }
    }
}
