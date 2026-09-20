package app.shutterup.ui.issue

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.shutterup.ui.components.Kicker
import app.shutterup.ui.theme.ShutterUpTheme
import app.shutterup.ui.theme.themeTint
import coil3.compose.AsyncImage

private const val SHEET_COLUMNS = 6

@Composable
fun MonthlyIssuePage(
    page: IssuePageUi,
    onOpenDay: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    onOpenIssue: (() -> Unit)? = null,
    fillSheet: Boolean = false,
) {
    val dark = isSystemInDarkTheme()
    val tint = themeTint(page.theme, MaterialTheme.colorScheme, dark)
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .then(if (fillSheet) Modifier.fillMaxSize() else Modifier)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Kicker(
                    text = page.kicker,
                    modifier = Modifier.weight(1f),
                )
                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                text = page.headline,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            ContactSheet(
                thumbs = page.thumbs,
                onOpenDay = onOpenDay,
                fill = fillSheet,
                modifier = if (fillSheet) {
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                } else {
                    Modifier.padding(top = 24.dp)
                },
            )
            Text(
                text = page.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
            if (page.themesKicker.isNotBlank()) {
                Kicker(
                    text = page.themesKicker,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
    val pageModifier = modifier
        .fillMaxWidth()
        .then(if (fillSheet) Modifier.fillMaxHeight() else Modifier)
        .semantics { contentDescription = page.kicker }
    if (onOpenIssue != null) {
        Surface(
            onClick = onOpenIssue,
            modifier = pageModifier,
            shape = RoundedCornerShape(28.dp),
            color = tint,
            content = content,
        )
    } else {
        Surface(
            modifier = pageModifier,
            shape = RoundedCornerShape(28.dp),
            color = tint,
            content = content,
        )
    }
}

@Composable
fun ContactSheet(
    thumbs: List<IssueThumbUi>,
    onOpenDay: (String) -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
) {
    if (thumbs.isEmpty()) {
        Spacer(modifier = modifier.fillMaxWidth())
        return
    }
    if (fill) {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            val columns = chooseSheetColumns(thumbs.size, maxWidth, maxHeight)
            val cell = sheetCellSize(columns, thumbs.size, maxWidth, maxHeight)
            SheetGrid(
                thumbs = thumbs,
                columns = columns,
                cell = cell,
                onOpenDay = onOpenDay,
            )
        }
        return
    }
    SheetGrid(
        thumbs = thumbs,
        columns = SHEET_COLUMNS,
        cell = null,
        onOpenDay = onOpenDay,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun SheetGrid(
    thumbs: List<IssueThumbUi>,
    columns: Int,
    cell: Dp?,
    onOpenDay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = (thumbs.size + columns - 1) / columns
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    val cellModifier = if (cell != null) {
                        Modifier.size(cell)
                    } else {
                        Modifier.weight(1f)
                    }
                    if (index < thumbs.size) {
                        ContactCell(
                            thumb = thumbs[index],
                            onOpenDay = onOpenDay,
                            modifier = cellModifier,
                        )
                    } else {
                        Spacer(modifier = cellModifier)
                    }
                }
            }
        }
    }
}

internal fun chooseSheetColumns(count: Int, width: Dp, height: Dp): Int {
    if (count <= 6) return SHEET_COLUMNS
    val gutter = 4.dp
    for (cols in 3..SHEET_COLUMNS) {
        val cell = (width - gutter * (cols - 1)) / cols
        if (cell <= 0.dp) continue
        val rows = (count + cols - 1) / cols
        val needed = cell * rows + gutter * (rows - 1)
        if (needed <= height) return cols
    }
    return SHEET_COLUMNS
}

internal fun sheetCellSize(columns: Int, count: Int, width: Dp, height: Dp): Dp {
    val gutter = 4.dp
    val fromWidth = (width - gutter * (columns - 1)) / columns
    val rows = (count + columns - 1) / columns
    val needed = fromWidth * rows + gutter * (rows - 1)
    if (needed <= height || rows <= 0) return fromWidth
    val fromHeight = (height - gutter * (rows - 1)) / rows
    return minOf(fromWidth, fromHeight)
}

@Composable
private fun ContactCell(
    thumb: IssueThumbUi,
    onOpenDay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, outline, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onOpenDay(thumb.dateIso) }
            .semantics { contentDescription = thumb.spokenDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (thumb.thumbPath != null) {
            AsyncImage(
                model = thumb.thumbPath,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Preview(name = "compact-light", showBackground = true, widthDp = 400, heightDp = 1100)
@Composable
private fun MonthlyIssuePagePreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        MonthlyIssuePage(
            page = sampleIssuePage(),
            onOpenDay = {},
            fillSheet = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(
    name = "compact-dark",
    showBackground = true,
    widthDp = 400,
    heightDp = 1100,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MonthlyIssuePagePreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        MonthlyIssuePage(
            page = sampleIssuePage(),
            onOpenDay = {},
            fillSheet = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "expanded-light", showBackground = true, widthDp = 840, heightDp = 1100)
@Composable
private fun MonthlyIssuePagePreviewExpanded() {
    ShutterUpTheme(darkTheme = false) {
        Box(Modifier.fillMaxSize().padding(24.dp)) {
            MonthlyIssuePage(
                page = sampleIssuePage(),
                onOpenDay = {},
                fillSheet = true,
                modifier = Modifier.fillMaxWidth(0.72f).fillMaxHeight(),
            )
        }
    }
}

@Preview(name = "font-scale-2", showBackground = true, widthDp = 400, heightDp = 1400, fontScale = 2f)
@Composable
private fun MonthlyIssuePagePreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        MonthlyIssuePage(
            page = sampleIssuePage(),
            onOpenDay = {},
            fillSheet = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "sparse-light", showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun MonthlyIssuePagePreviewSparse() {
    ShutterUpTheme(darkTheme = false) {
        MonthlyIssuePage(
            page = sampleIssuePage(sparse = true),
            onOpenDay = {},
            fillSheet = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "feed-card-light", showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun MonthlyIssueCardPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        MonthlyIssuePage(
            page = sampleIssuePage(),
            onOpenDay = {},
            onDismiss = {},
            onOpenIssue = {},
        )
    }
}
