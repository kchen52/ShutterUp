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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shutterup.domain.model.DayStatus
import app.shutterup.ui.components.MonthRing
import app.shutterup.ui.icons.SnowflakeIcon
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
 * Month grid with day-status dots/rings. Tap opens `day/{dateIso}`.
 */
@Composable
fun CalendarRoute(
    onOpenDay: (String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CalendarScreen(
        state = state,
        onMonthChange = viewModel::showMonth,
        onOpenDay = { date -> onOpenDay(date.toString()) },
    )
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
    LaunchedEffect(pagerState.settledPage) {
        onMonthChange(PAGER_START.plusMonths(pagerState.settledPage.toLong()))
    }
    Scaffold(
        topBar = {
            TopAppBar(
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
                colors = TopAppBarDefaults.topAppBarColors(),
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
                MonthRing(completed = state.monthCompleted, eligible = state.monthEligible)
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
        DayOfWeek.entries.let { days ->
            days.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
            .aspectRatio(1f)
            .semantics { contentDescription = description }
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .todayRing(cell),
        contentAlignment = Alignment.Center,
    ) {
        when {
            cell.status == DayStatus.COMPLETED && !cell.thumbPath.isNullOrBlank() &&
                File(cell.thumbPath).exists() -> {
                AsyncImage(
                    model = File(cell.thumbPath),
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
            }
            cell.status == DayStatus.COMPLETED || cell.status == DayStatus.COMPLETED_NO_PHOTO -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp),
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
        if (cell.status != DayStatus.COMPLETED && cell.status != DayStatus.COMPLETED_NO_PHOTO) {
            Text(
                text = cell.date.dayOfMonth.toString(),
                color = numberColor,
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
