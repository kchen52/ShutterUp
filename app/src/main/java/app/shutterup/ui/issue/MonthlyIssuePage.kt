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
) {
    val dark = isSystemInDarkTheme()
    val tint = themeTint(page.theme, MaterialTheme.colorScheme, dark)
    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(20.dp)) {
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
                modifier = Modifier.padding(top = 24.dp),
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
) {
    if (thumbs.isEmpty()) {
        Spacer(modifier = modifier.fillMaxWidth())
        return
    }
    val rows = (thumbs.size + SHEET_COLUMNS - 1) / SHEET_COLUMNS
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (col in 0 until SHEET_COLUMNS) {
                    val index = row * SHEET_COLUMNS + col
                    if (index < thumbs.size) {
                        ContactCell(
                            thumb = thumbs[index],
                            onOpenDay = onOpenDay,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
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
        BoxWithConstraints {
            MonthlyIssuePage(page = sampleIssuePage(), onOpenDay = {})
        }
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
        MonthlyIssuePage(page = sampleIssuePage(), onOpenDay = {})
    }
}

@Preview(name = "expanded-light", showBackground = true, widthDp = 840, heightDp = 1100)
@Composable
private fun MonthlyIssuePagePreviewExpanded() {
    ShutterUpTheme(darkTheme = false) {
        Box(Modifier.padding(24.dp)) {
            MonthlyIssuePage(
                page = sampleIssuePage(),
                onOpenDay = {},
                modifier = Modifier.fillMaxWidth(0.72f),
            )
        }
    }
}

@Preview(name = "font-scale-2", showBackground = true, widthDp = 400, heightDp = 1400, fontScale = 2f)
@Composable
private fun MonthlyIssuePagePreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        MonthlyIssuePage(page = sampleIssuePage(), onOpenDay = {})
    }
}

@Preview(name = "sparse-light", showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun MonthlyIssuePagePreviewSparse() {
    ShutterUpTheme(darkTheme = false) {
        MonthlyIssuePage(page = sampleIssuePage(sparse = true), onOpenDay = {})
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
