package app.shutterup.ui.calendar

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private val PAGER_START = YearMonth.of(2020, 1)
private const val PAGER_MONTHS = 240

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
    onOpenDay: (java.time.LocalDate) -> Unit = {},
) {
    val monthTitle = remember(state.month) {
        state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
    }
    val startPage = remember {
        monthsBetween(PAGER_START, YearMonth.from(state.today)).coerceIn(0, PAGER_MONTHS - 1)
    }
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { PAGER_MONTHS })
    val scope = rememberCoroutineScope()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    LaunchedEffect(pagerState.settledPage) {
        onMonthChange(PAGER_START.plusMonths(pagerState.settledPage.toLong()))
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    AnimatedContent(targetState = monthTitle, label = "monthTitle") { title ->
                        Text(text = title, style = MaterialTheme.typography.headlineMedium)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                    ) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                    }
                    IconButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                    ) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
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
                .padding(horizontal = 16.dp),
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
            WeekdayHeader()
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
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
            Spacer(Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val progress = if (state.monthEligible <= 0) {
                    0f
                } else {
                    (state.monthCompleted.toFloat() / state.monthEligible.toFloat()).coerceIn(0f, 1f)
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
                    text = "${state.monthCompleted} of ${state.monthEligible} days this month",
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
