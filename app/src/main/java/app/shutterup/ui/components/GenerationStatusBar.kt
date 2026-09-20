package app.shutterup.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.shutterup.domain.ai.GenerationProgress
import app.shutterup.ui.theme.ShutterUpTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** User-visible generation status (DESIGN.md §4.1, SPEC §7.6). */
object PromptGenerationCopy {
    const val SERIES = "Generating prompts for a seven-day series"

    fun forDate(date: LocalDate): String {
        val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return "Generating prompts for $weekday"
    }

    fun message(progress: GenerationProgress): String =
        if (progress.series) SERIES else forDate(progress.date)
}

/**
 * Slim, non-blocking strip shown while Nano or the library is writing prompts.
 */
@Composable
fun GenerationStatusBar(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = message
                    liveRegion = LiveRegionMode.Polite
                },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 400)
@Composable
private fun GenerationStatusBarPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        GenerationStatusBar(message = PromptGenerationCopy.SERIES)
    }
}

@Preview(name = "Dark", showBackground = true, widthDp = 400, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GenerationStatusBarPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        GenerationStatusBar(message = PromptGenerationCopy.forDate(LocalDate.of(2026, 9, 21)))
    }
}
