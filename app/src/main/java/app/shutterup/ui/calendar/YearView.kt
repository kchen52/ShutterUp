package app.shutterup.ui.calendar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.shutterup.domain.calendar.YearCell
import app.shutterup.domain.calendar.YearCellMark
import app.shutterup.domain.calendar.YearGrid
import app.shutterup.domain.calendar.YearMonthBand
import app.shutterup.ui.components.Kicker
import app.shutterup.ui.theme.ShutterUpTheme
import app.shutterup.ui.theme.themeAccent
import app.shutterup.ui.theme.themeTint
import java.time.format.TextStyle
import java.util.Locale

private val YearCellShape = RoundedCornerShape(3.dp)

/**
 * Printed year: twelve month bands, one small cell per day. No photographs,
 * no numbers inside the cells. Month kickers live in the margin.
 */
@Composable
fun YearGridView(
    grid: YearGrid,
    onOpenDay: (java.time.LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        grid.months.forEach { band ->
            YearMonthRow(
                band = band,
                onOpenDay = onOpenDay,
                interactive = interactive,
                darkTheme = darkTheme,
            )
        }
    }
}

@Composable
private fun YearMonthRow(
    band: YearMonthBand,
    onOpenDay: (java.time.LocalDate) -> Unit,
    interactive: Boolean,
    darkTheme: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Kicker(
            text = band.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
            modifier = Modifier.padding(end = 8.dp),
        )
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                band.cells.forEach { cell ->
                    YearDayCell(
                        cell = cell,
                        onClick = { onOpenDay(cell.date) },
                        interactive = interactive,
                        darkTheme = darkTheme,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun YearDayCell(
    cell: YearCell,
    onClick: () -> Unit,
    interactive: Boolean,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val description = yearCellDescription(cell)
    val scheme = MaterialTheme.colorScheme
    val hairline = scheme.outlineVariant.copy(alpha = 0.4f)
    Box(
        modifier = modifier
            .semantics { contentDescription = description }
            .clickable(enabled = interactive, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(YearCellShape)
                .then(
                    when (cell.mark) {
                        YearCellMark.COMPLETED -> {
                            val theme = cell.theme
                            val fill = if (theme.isNullOrBlank()) {
                                scheme.surfaceContainerHigh
                            } else {
                                lerp(
                                    themeTint(theme, scheme, darkTheme),
                                    themeAccent(theme, scheme),
                                    if (darkTheme) 0.32f else 0.28f,
                                )
                            }
                            Modifier.background(fill)
                        }
                        YearCellMark.PENDING_TODAY -> Modifier
                            .background(scheme.surface)
                            .border(1.5.dp, scheme.primary, YearCellShape)
                        YearCellMark.SKIPPED,
                        YearCellMark.MISSED,
                        YearCellMark.ABSENT,
                        -> Modifier.border(1.dp, hairline, YearCellShape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (cell.mark) {
                YearCellMark.SKIPPED -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(1.5.dp)
                            .background(scheme.outlineVariant),
                    )
                }
                YearCellMark.MISSED -> {
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(scheme.outlineVariant),
                    )
                }
                else -> Unit
            }
        }
    }
}

@Preview(name = "Year sparse light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun YearGridPreviewSparseLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface { YearGridView(grid = sampleYearGrid(full = false), onOpenDay = {}) }
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
private fun YearGridPreviewSparseDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface { YearGridView(grid = sampleYearGrid(full = false), onOpenDay = {}, darkTheme = true) }
    }
}

@Preview(name = "Year full light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun YearGridPreviewFullLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface { YearGridView(grid = sampleYearGrid(full = true), onOpenDay = {}) }
    }
}

@Preview(
    name = "Year expanded",
    showBackground = true,
    widthDp = 840,
    heightDp = 800,
)
@Composable
private fun YearGridPreviewExpanded() {
    ShutterUpTheme(darkTheme = false) {
        Surface { YearGridView(grid = sampleYearGrid(full = true), onOpenDay = {}) }
    }
}

@Preview(name = "Year font 200%", showBackground = true, widthDp = 360, heightDp = 1200, fontScale = 2f)
@Composable
private fun YearGridPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        Surface { YearGridView(grid = sampleYearGrid(full = false), onOpenDay = {}) }
    }
}
