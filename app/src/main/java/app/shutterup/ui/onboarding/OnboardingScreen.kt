package app.shutterup.ui.onboarding

import android.Manifest
import android.content.res.Configuration
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shutterup.domain.ai.Availability
import app.shutterup.ui.settings.SettingsCopy
import app.shutterup.ui.theme.FrauncesHeadline
import app.shutterup.ui.theme.ShutterUpTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) {
        if (state.finished) onFinished()
    }
    OnboardingScreen(
        state = state,
        onPageChange = viewModel::setPage,
        onNext = viewModel::nextPage,
        onBack = viewModel::previousPage,
        onNotifyTime = viewModel::setNotifyTime,
        onThemeFocus = viewModel::setThemeFocus,
        onFinish = viewModel::finish,
    )
}

/**
 * Four full-screen pages, max width 480 dp, swipe or Next (DESIGN.md §4.9).
 */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    modifier: Modifier = Modifier,
    onPageChange: (Int) -> Unit = {},
    onNext: () -> Unit = {},
    onBack: () -> Unit = {},
    onNotifyTime: (Int, Int) -> Unit = { _, _ -> },
    onThemeFocus: (String) -> Unit = {},
    onFinish: () -> Unit = {},
    onRequestNotifications: () -> Unit = {},
) {
    val pagerState = rememberPagerState(
        initialPage = state.page,
        pageCount = { OnboardingUiState.PAGE_COUNT },
    )
    LaunchedEffect(state.page) {
        if (pagerState.currentPage != state.page) {
            pagerState.scrollToPage(state.page)
        }
    }
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != state.page) {
            onPageChange(pagerState.currentPage)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onNext() }
    val requestNotifications: () -> Unit = {
        onRequestNotifications()
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    Box(modifier = modifier.fillMaxSize()) {
        WelcomeGradient(visible = state.page == 0)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
            ) { page ->
                OnboardingPageContent(
                    page = page,
                    state = state,
                    onNotifyTime = onNotifyTime,
                    onThemeFocus = onThemeFocus,
                    onAllowNotifications = requestNotifications,
                )
            }
            Row(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.page > 0) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(OnboardingCopy.BACK)
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
                if (state.page == OnboardingUiState.LAST_PAGE) {
                    Button(
                        onClick = onFinish,
                        enabled = !state.saving,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(OnboardingCopy.GET_FIRST_PROMPT)
                    }
                } else {
                    Button(
                        onClick = {
                            if (state.page == 1) requestNotifications() else onNext()
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(OnboardingCopy.NEXT)
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(
    page: Int,
    state: OnboardingUiState,
    modifier: Modifier = Modifier,
    onNotifyTime: (Int, Int) -> Unit = { _, _ -> },
    onThemeFocus: (String) -> Unit = {},
    onAllowNotifications: () -> Unit = {},
) {
    when (page) {
        0 -> WelcomePage(modifier)
        1 -> NotificationsPage(onAllowNotifications, modifier)
        2 -> PreferencesPage(state, onNotifyTime, onThemeFocus, modifier)
        else -> AiStatusPage(state, modifier)
    }
}

@Composable
private fun WelcomePage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = OnboardingCopy.WORDMARK,
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = FrauncesHeadline),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = OnboardingCopy.WELCOME_SENTENCE,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun NotificationsPage(
    onAllowNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = OnboardingCopy.NOTIFICATIONS_HEADLINE,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = OnboardingCopy.NOTIFICATIONS_BODY,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        MockNotificationCard()
        Button(
            onClick = onAllowNotifications,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(OnboardingCopy.ALLOW_NOTIFICATIONS)
        }
    }
}

@Composable
private fun MockNotificationCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = OnboardingCopy.MOCK_NOTIFICATION_APP,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = OnboardingCopy.MOCK_NOTIFICATION_TITLE,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = OnboardingCopy.MOCK_NOTIFICATION_TEXT,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferencesPage(
    state: OnboardingUiState,
    onNotifyTime: (Int, Int) -> Unit,
    onThemeFocus: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickerState = rememberTimePickerState(
        initialHour = state.notifyHour,
        initialMinute = state.notifyMinute,
        is24Hour = false,
    )
    LaunchedEffect(pickerState.hour, pickerState.minute) {
        onNotifyTime(pickerState.hour, pickerState.minute)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = OnboardingCopy.PREFS_HEADLINE,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = OnboardingCopy.NOTIFY_TIME_LABEL,
            style = MaterialTheme.typography.titleMedium,
        )
        TimePicker(state = pickerState)
        if (state.themeFocus.isEmpty()) {
            Text(
                text = SettingsCopy.THEME_FOCUS_LABEL,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = state.themeFocus,
            onValueChange = onThemeFocus,
            modifier = Modifier.fillMaxWidth(),
            label = if (state.themeFocus.isNotEmpty()) {
                { Text(SettingsCopy.THEME_FOCUS_LABEL) }
            } else {
                null
            },
            placeholder = { Text(OnboardingCopy.THEME_FOCUS_PLACEHOLDER) },
            singleLine = true,
        )
    }
}

@Composable
private fun AiStatusPage(
    state: OnboardingUiState,
    modifier: Modifier = Modifier,
) {
    val availability = state.availability
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = OnboardingCopy.AI_HEADLINE,
            style = MaterialTheme.typography.headlineMedium,
        )
        when (availability) {
            Availability.UNAVAILABLE -> {
                Text(
                    text = OnboardingCopy.AI_UNAVAILABLE,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Availability.AVAILABLE -> {
                Text(
                    text = OnboardingCopy.AI_READY,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Availability.DOWNLOADABLE, Availability.DOWNLOADING, null -> {
                Text(
                    text = if (state.aiDownloadPercent != null) {
                        "${OnboardingCopy.AI_PREPARING} · ${state.aiDownloadPercent} %"
                    } else {
                        OnboardingCopy.AI_PREPARING
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (state.aiDownloadPercent != null) {
                    LinearProgressIndicator(
                        progress = { (state.aiDownloadPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun WelcomeGradient(visible: Boolean) {
    if (!visible) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
        return
    }
    val inspect = LocalInspectionMode.current
    val context = LocalContext.current
    val motion = remember(inspect) {
        if (inspect) {
            false
        } else {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) != 0f
        }
    }
    val hue = if (motion) {
        val transition = rememberInfiniteTransition(label = "welcome-hue")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 20_000, easing = LinearEasing),
            ),
            label = "hue",
        )
        animated
    } else {
        32f
    }
    val scheme = MaterialTheme.colorScheme
    val a = hslSoft(hue, scheme.surface)
    val b = hslSoft(hue + 40f, scheme.surface)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(a, b))),
    )
}

private fun hslSoft(hue: Float, surface: Color): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val rad = Math.toRadians(h.toDouble())
    val mixR = (0.55 + 0.45 * cos(rad)).toFloat()
    val mixG = (0.55 + 0.45 * cos(rad + 2.1)).toFloat()
    val mixB = (0.55 + 0.45 * sin(rad)).toFloat()
    return Color(
        red = surface.red * 0.88f + mixR * 0.12f,
        green = surface.green * 0.88f + mixG * 0.12f,
        blue = surface.blue * 0.88f + mixB * 0.12f,
        alpha = 1f,
    )
}

@Preview(name = "Welcome light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OnboardingWelcomeLightPreview() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            OnboardingPageContent(page = 0, state = OnboardingUiState())
        }
    }
}

@Preview(name = "Notifications dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingNotificationsDarkPreview() {
    ShutterUpTheme(darkTheme = true) {
        Surface {
            OnboardingPageContent(page = 1, state = OnboardingUiState(page = 1))
        }
    }
}

@Preview(name = "Prefs light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OnboardingPrefsLightPreview() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            OnboardingPageContent(page = 2, state = OnboardingUiState(page = 2))
        }
    }
}

@Preview(name = "AI light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OnboardingAiLightPreview() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            OnboardingPageContent(
                page = 3,
                state = OnboardingUiState(page = 3, availability = Availability.DOWNLOADING),
            )
        }
    }
}
