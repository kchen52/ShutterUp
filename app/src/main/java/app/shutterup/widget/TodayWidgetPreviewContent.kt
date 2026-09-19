package app.shutterup.widget

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shutterup.ui.detail.samplePrompt
import app.shutterup.ui.theme.ShutterUpTheme
import app.shutterup.ui.theme.themeTint
import java.time.LocalDate

enum class TodayWidgetPreviewSize { Small, Medium }

/**
 * Compose stand-in for Glance layouts so Roborazzi can capture DESIGN.md §8
 * (`GlancePreview` is not used; screenshot the content function directly).
 */
@Composable
fun TodayWidgetPreviewContent(
    state: TodayWidgetState,
    size: TodayWidgetPreviewSize,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false,
) {
    val medium = size == TodayWidgetPreviewSize.Medium
    val scheme = MaterialTheme.colorScheme
    val tint = themeTint(state.theme, scheme, darkTheme)
    val boxMod = if (medium) {
        modifier.size(width = 250.dp, height = 140.dp)
    } else {
        modifier.size(110.dp)
    }
    Surface(
        modifier = boxMod,
        shape = RoundedCornerShape(20.dp),
        color = tint,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (medium) 12.dp else 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (medium) state.kicker else state.shortKicker,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = scheme.onSurfaceVariant,
                    maxLines = if (medium) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (state.completed) {
                    Box(
                        modifier = Modifier
                            .background(scheme.primaryContainer, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "✓",
                            color = scheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
            if (!state.completed) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.title,
                    fontFamily = FontFamily.Serif,
                    fontSize = if (medium) 20.sp else 18.sp,
                    lineHeight = if (medium) 24.sp else 22.sp,
                    color = scheme.onSurface,
                    maxLines = if (medium) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (medium && !state.paused) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.oneLiner,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 14.sp,
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Box(
                            modifier = Modifier
                                .background(scheme.primary, RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "Shoot",
                                color = scheme.onPrimary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

fun sampleTodayWidgetState(): TodayWidgetState = TodayWidgetState.from(
    date = LocalDate.of(2026, 9, 19),
    prompt = samplePrompt(),
    streakDays = 14,
    paused = false,
    thumbPath = null,
)

@Preview(name = "Medium light", widthDp = 250, heightDp = 140)
@Composable
private fun WidgetMediumLightPreview() {
    ShutterUpTheme(darkTheme = false) {
        TodayWidgetPreviewContent(
            state = sampleTodayWidgetState(),
            size = TodayWidgetPreviewSize.Medium,
            darkTheme = false,
        )
    }
}

@Preview(name = "Medium dark", widthDp = 250, heightDp = 140, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WidgetMediumDarkPreview() {
    ShutterUpTheme(darkTheme = true) {
        TodayWidgetPreviewContent(
            state = sampleTodayWidgetState(),
            size = TodayWidgetPreviewSize.Medium,
            darkTheme = true,
        )
    }
}

@Preview(name = "Small light", widthDp = 110, heightDp = 110)
@Composable
private fun WidgetSmallLightPreview() {
    ShutterUpTheme(darkTheme = false) {
        TodayWidgetPreviewContent(
            state = sampleTodayWidgetState(),
            size = TodayWidgetPreviewSize.Small,
            darkTheme = false,
        )
    }
}
