package app.shutterup.ui.calendar

import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.shutterup.domain.calendar.yearGrid
import app.shutterup.domain.model.DayStatus
import app.shutterup.ui.adaptive.ShutterUpListDetail
import app.shutterup.ui.adaptive.isExpandedWidth
import app.shutterup.ui.components.ApertureCheckMark
import app.shutterup.ui.day.DayRoute
import app.shutterup.ui.icons.SnowflakeIcon
import app.shutterup.ui.navigation.ShutterUpDestinations
import app.shutterup.ui.theme.ShutterUpTheme
import coil3.compose.AsyncImage
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val PAGER_START = YearMonth.of(2020, 1)
private const val PAGER_MONTHS = 240
private const val PAGER_YEARS = 20
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private const val ZOOM_DURATION_MS = 400

/**
 * Month grid with day-status dots/rings. Compact: tap opens `day/{dateIso}`.
 * Expanded: grid | Day in [ShutterUpListDetail].
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun CalendarRoute(
    onOpenDay: (String) -> Unit,
    onOpenDetail: (dateIso: String, autoLaunchCamera: Boolean) -> Unit = { _, _ -> },
    onOpenCompletion: (String) -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val expanded = isExpandedWidth(currentWindowAdaptiveInfo().windowSizeClass.minWidthDp)
    if (expanded) {
        var selectedIso by remember(state.today) { mutableStateOf(state.today.toString()) }
        ShutterUpListDetail(
            listFraction = 0.45f,
            list = {
                CalendarScreen(
                    state = state,
                    onMonthChange = viewModel::showMonth,
                    onOpenDay = { date -> selectedIso = date.toString() },
                )
            },
            detail = {
                key(selectedIso) {
                    val paneNav = rememberNavController()
                    NavHost(
                        navController = paneNav,
                        startDestination = ShutterUpDestinations.day(selectedIso),
                    ) {
                        composable(
                            route = ShutterUpDestinations.DAY,
                            arguments = listOf(
                                navArgument("dateIso") { type = NavType.StringType },
                            ),
                        ) {
                            DayRoute(
                                onBack = { },
                                onOpenDetail = onOpenDetail,
                                onOpenCompletion = onOpenCompletion,
                            )
                        }
                    }
                }
            },
        )
    } else {
        CalendarScreen(
            state = state,
            onMonthChange = viewModel::showMonth,
            onOpenDay = { date -> onOpenDay(date.toString()) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onMonthChange: (YearMonth) -> Unit = {},
    onOpenDay: (LocalDate) -> Unit = {},
    startInYearView: Boolean = false,
) {
    val monthTitle = remember(state.month) {
        state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
    }
    val yearTitle = remember(state.month.year) { state.month.year.toString() }
    val startPage = remember {
        monthsBetween(PAGER_START, YearMonth.from(state.today)).coerceIn(0, PAGER_MONTHS - 1)
    }
    val yearStartPage = remember {
        (state.today.year - PAGER_START.year).coerceIn(0, PAGER_YEARS - 1)
    }
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { PAGER_MONTHS })
    val yearPagerState = rememberPagerState(initialPage = yearStartPage, pageCount = { PAGER_YEARS })
    val scope = rememberCoroutineScope()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val yearProgress = remember { mutableFloatStateOf(if (startInYearView) 1f else 0f) }
    var pinching by remember { mutableStateOf(false) }
    val animScale = rememberAnimatorDurationScale()
    val zooming = pinching || yearProgress.floatValue in 0.01f..0.99f
    val inYear = yearProgress.floatValue >= 0.5f
    val title = if (inYear) yearTitle else monthTitle
    val monthInteractive = !pinching && yearProgress.floatValue < 0.5f
    val yearInteractive = !pinching && yearProgress.floatValue > 0.5f

    LaunchedEffect(pagerState.settledPage) {
        if (yearProgress.floatValue < 0.5f) {
            onMonthChange(PAGER_START.plusMonths(pagerState.settledPage.toLong()))
        }
    }
    LaunchedEffect(yearPagerState.settledPage) {
        if (yearProgress.floatValue >= 0.5f) {
            val year = PAGER_START.year + yearPagerState.settledPage
            if (year != state.month.year) {
                onMonthChange(YearMonth.of(year, state.month.monthValue))
            }
        }
    }
    LaunchedEffect(state.month.year) {
        val target = (state.month.year - PAGER_START.year).coerceIn(0, PAGER_YEARS - 1)
        if (yearPagerState.currentPage != target) {
            yearPagerState.scrollToPage(target)
        }
    }

    fun animateYearProgress(target: Float) {
        scope.launch {
            val duration = (ZOOM_DURATION_MS * animScale).roundToInt()
            if (duration <= 0) {
                yearProgress.floatValue = target
            } else {
                animate(
                    initialValue = yearProgress.floatValue,
                    targetValue = target,
                    animationSpec = tween(durationMillis = duration, easing = EmphasizedDecelerate),
                ) { value, _ -> yearProgress.floatValue = value }
            }
        }
    }

    suspend fun scrollPager(year: Boolean, delta: Int) {
        val pager = if (year) yearPagerState else pagerState
        val page = (pager.currentPage + delta).coerceIn(0, pager.pageCount - 1)
        if (animScale <= 0f) pager.scrollToPage(page) else pager.animateScrollToPage(page)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    AnimatedContent(targetState = title, label = "calendarTitle") { text ->
                        Text(text = text, style = MaterialTheme.typography.headlineMedium)
                    }
                },
                actions = {
                    YearViewToggle(
                        inYear = inYear,
                        onClick = { animateYearProgress(if (inYear) 0f else 1f) },
                    )
                    IconButton(
                        onClick = { scope.launch { scrollPager(inYear, -1) } },
                    ) {
                        Icon(
                            Icons.Filled.ChevronLeft,
                            contentDescription = if (inYear) "Previous year" else "Previous month",
                        )
                    }
                    IconButton(
                        onClick = { scope.launch { scrollPager(inYear, 1) } },
                    ) {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = if (inYear) "Next year" else "Next month",
                        )
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .pointerInput(animScale) {
                    detectCalendarPinch(
                        onPinchDelta = { zoom ->
                            pinching = true
                            yearProgress.floatValue =
                                yearProgressAfterPinch(yearProgress.floatValue, zoom)
                        },
                        onPinchEnd = {
                            pinching = false
                            animateYearProgress(snapYearProgress(yearProgress.floatValue))
                        },
                    )
                },
        ) {
            if (!state.hasHistory) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Your first photo goes here.",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Come back after today's prompt.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                return@Column
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val p = yearProgress.floatValue
                if (p < 1f || zooming) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = 1f - p
                                val scale = 1f - 0.08f * p
                                scaleX = scale
                                scaleY = scale
                            }
                            .then(
                                if (inYear) Modifier.clearAndSetSemantics { } else Modifier,
                            ),
                    ) {
                        WeekdayHeader()
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = monthInteractive,
                            modifier = Modifier.fillMaxWidth(),
                        ) { page ->
                            val pageMonth = PAGER_START.plusMonths(page.toLong())
                            val cells = if (pageMonth == state.month) {
                                state.cells
                            } else {
                                emptyList()
                            }
                            MonthGrid(
                                cells = cells.ifEmpty {
                                    monthCells(pageMonth, emptyMap(), emptyMap(), state.today)
                                },
                                onOpenDay = onOpenDay,
                            )
                        }
                    }
                }
                if (p > 0f || zooming) {
                    HorizontalPager(
                        state = yearPagerState,
                        userScrollEnabled = yearInteractive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = p
                                val scale = 0.92f + 0.08f * p
                                scaleX = scale
                                scaleY = scale
                            }
                            .then(
                                if (!inYear) Modifier.clearAndSetSemantics { } else Modifier,
                            ),
                    ) { page ->
                        val pageYear = PAGER_START.year + page
                        val grid = if (pageYear == state.yearGrid.year) {
                            state.yearGrid
                        } else {
                            yearGrid(pageYear, emptyList(), state.today)
                        }
                        YearGridView(
                            grid = grid,
                            onOpenDay = onOpenDay,
                            interactive = yearInteractive,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            val summaryCompleted = if (inYear) state.yearGrid.progress.completed else state.monthCompleted
            val summaryEligible = if (inYear) state.yearGrid.progress.eligible else state.monthEligible
            val summaryLabel = if (inYear) {
                "$summaryCompleted of $summaryEligible days this year"
            } else {
                "$summaryCompleted of $summaryEligible days this month"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val progress = if (summaryEligible <= 0) {
                    0f
                } else {
                    (summaryCompleted.toFloat() / summaryEligible.toFloat()).coerceIn(0f, 1f)
                }
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                    strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                )
                Text(
                    text = summaryLabel,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Longest streak ${state.longestStreak}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun YearViewToggle(
    inYear: Boolean,
    onClick: () -> Unit,
) {
    val label = if (inYear) "MONTH" else "YEAR"
    val description = if (inYear) "Show month view" else "Show year view"
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics {
                role = Role.Button
                contentDescription = description
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun rememberAnimatorDurationScale(): Float {
    val inspect = LocalInspectionMode.current
    val context = LocalContext.current
    return remember(inspect) {
        if (inspect) {
            1f
        } else {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    cells: List<CalendarCell>,
    onOpenDay: (java.time.LocalDate) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        userScrollEnabled = false,
    ) {
        items(cells, key = { it.date }) { cell ->
            CalendarDayCell(cell = cell, onClick = { onOpenDay(cell.date) })
        }
    }
}

@Composable
private fun CalendarDayCell(
    cell: CalendarCell,
    onClick: () -> Unit,
) {
    val description = calendarCellDescription(cell)
    val thumbPath = cell.thumbPath
    val completedWithPhoto = cell.status == DayStatus.COMPLETED &&
        !thumbPath.isNullOrBlank() && File(thumbPath).exists()
    val completedNoPhoto = (cell.status == DayStatus.COMPLETED ||
        cell.status == DayStatus.COMPLETED_NO_PHOTO) && !completedWithPhoto
    val numberColor = when {
        !cell.inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        cell.isFuture -> MaterialTheme.colorScheme.onSurfaceVariant
        cell.status == DayStatus.PAUSED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        cell.status == DayStatus.MISSED -> MaterialTheme.colorScheme.outline
        cell.isToday && cell.status == DayStatus.PENDING -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth()
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .todayRing(cell),
            contentAlignment = Alignment.Center,
        ) {
            when {
                completedWithPhoto -> {
                    AsyncImage(
                        model = File(requireNotNull(thumbPath)),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp),
                            ),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                    )
                }
                completedNoPhoto -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Text(
                            text = cell.date.dayOfMonth.toString(),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 6.dp, top = 4.dp),
                        )
                        ApertureCheckMark(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 5.dp),
                            size = 18.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            progress = 1f,
                        )
                    }
                }
                cell.status == DayStatus.SKIPPED -> {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(width = 10.dp, height = 1.5.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
                cell.status == DayStatus.MISSED -> {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
            if (!completedNoPhoto) {
                Text(
                    text = cell.date.dayOfMonth.toString(),
                    color = if (completedWithPhoto) {
                        Color.White
                    } else {
                        numberColor
                    },
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.alpha(if (cell.status == DayStatus.PAUSED) 0.6f else 1f),
                )
            }
            if (cell.frozen && (cell.status == DayStatus.SKIPPED || cell.status == DayStatus.MISSED)) {
                Icon(
                    imageVector = SnowflakeIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun Modifier.todayRing(cell: CalendarCell): Modifier {
    return if (cell.isToday && cell.status == DayStatus.PENDING) {
        border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
    } else {
        this
    }
}

private fun monthsBetween(start: YearMonth, end: YearMonth): Int =
    (end.year - start.year) * 12 + (end.monthValue - start.monthValue)

@Preview(name = "Compact light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun CalendarPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface { CalendarScreen(state = sampleCalendarState()) }
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
private fun CalendarPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface { CalendarScreen(state = sampleCalendarState()) }
    }
}

@Preview(name = "Year sparse light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun CalendarYearPreviewSparseLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            CalendarScreen(state = sampleYearCalendarState(full = false), startInYearView = true)
        }
    }
}

@Preview(
    name = "Year sparse dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CalendarYearPreviewSparseDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface {
            CalendarScreen(state = sampleYearCalendarState(full = false), startInYearView = true)
        }
    }
}

@Preview(name = "Year full light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun CalendarYearPreviewFullLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            CalendarScreen(state = sampleYearCalendarState(full = true), startInYearView = true)
        }
    }
}

@Preview(name = "Year expanded", showBackground = true, widthDp = 840, heightDp = 800)
@Composable
private fun CalendarYearPreviewExpanded() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            CalendarScreen(state = sampleYearCalendarState(full = true), startInYearView = true)
        }
    }
}

@Preview(name = "Year font 200%", showBackground = true, widthDp = 360, heightDp = 1200, fontScale = 2f)
@Composable
private fun CalendarYearPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            CalendarScreen(state = sampleYearCalendarState(full = false), startInYearView = true)
        }
    }
}
