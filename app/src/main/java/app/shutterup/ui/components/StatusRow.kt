package app.shutterup.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.shutterup.ui.icons.SnowflakeIcon
import app.shutterup.ui.theme.FrauncesHeadline
import app.shutterup.ui.theme.ShutterUpTheme

@Composable
fun MonthRing(
    completed: Int,
    eligible: Int,
    modifier: Modifier = Modifier,
) {
    val progress = if (eligible <= 0) {
        0f
    } else {
        (completed.toFloat() / eligible.toFloat()).coerceIn(0f, 1f)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(28.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
        )
        Text(
            text = "$completed/$eligible",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun StreakStatus(
    streakDays: Int,
    freezes: Int,
    monthCompleted: Int,
    monthEligible: Int,
    modifier: Modifier = Modifier,
) {
    val description = "$streakDays days, $freezes freezes, $monthCompleted of $monthEligible days this month"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = description
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { },
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = streakDays.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FrauncesHeadline,
                ),
            )
            Text(
                text = "days",
                modifier = Modifier.padding(bottom = 2.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        if (freezes > 0) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = SnowflakeIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = freezes.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        MonthRing(
            completed = monthCompleted,
            eligible = monthEligible,
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { },
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun StatusRowPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        StatusRowPreviewBody()
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatusRowPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        StatusRowPreviewBody()
    }
}

@Preview(name = "FontScale 2x", showBackground = true, fontScale = 2f)
@Composable
private fun StatusRowPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        StatusRowPreviewBody()
    }
}

@Composable
private fun StatusRowPreviewBody() {
    Surface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StreakStatus(streakDays = 14, freezes = 2, monthCompleted = 18, monthEligible = 19)
            StreakStatus(streakDays = 3, freezes = 0, monthCompleted = 1, monthEligible = 19)
        }
    }
}
