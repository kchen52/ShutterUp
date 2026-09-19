package app.shutterup.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.shutterup.domain.series.SeriesDot
import app.shutterup.ui.theme.ShutterUpTheme

@Composable
fun SeriesDots(
    dots: List<SeriesDot>,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val outline = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = modifier.semantics {
            contentDescription = seriesDotsDescription(dots)
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dots.forEach { dot ->
            when (dot) {
                SeriesDot.COMPLETED -> Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(primary),
                )
                SeriesDot.CURRENT -> Box(
                    modifier = Modifier
                        .size(8.dp)
                        .border(1.5.dp, primary, CircleShape),
                )
                SeriesDot.EMPTY -> Box(
                    modifier = Modifier
                        .size(6.dp)
                        .border(1.dp, outline, CircleShape),
                )
            }
        }
    }
}

internal fun seriesDotsDescription(dots: List<SeriesDot>): String {
    val current = dots.indexOf(SeriesDot.CURRENT) + 1
    val completed = dots.count { it == SeriesDot.COMPLETED }
    return if (current > 0) {
        "Series day $current of ${dots.size}, $completed complete"
    } else {
        "Series, $completed of ${dots.size} complete"
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun SeriesDotsPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            SeriesDots(
                dots = listOf(
                    SeriesDot.COMPLETED,
                    SeriesDot.COMPLETED,
                    SeriesDot.CURRENT,
                    SeriesDot.EMPTY,
                    SeriesDot.EMPTY,
                    SeriesDot.EMPTY,
                    SeriesDot.EMPTY,
                ),
            )
        }
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SeriesDotsPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface {
            SeriesDots(
                dots = listOf(
                    SeriesDot.COMPLETED,
                    SeriesDot.EMPTY,
                    SeriesDot.CURRENT,
                    SeriesDot.EMPTY,
                    SeriesDot.EMPTY,
                    SeriesDot.EMPTY,
                    SeriesDot.EMPTY,
                ),
            )
        }
    }
}
