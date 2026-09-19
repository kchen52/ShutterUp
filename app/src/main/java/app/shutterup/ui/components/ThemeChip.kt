package app.shutterup.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.shutterup.ui.theme.LocalThemeTint
import app.shutterup.ui.theme.ShutterUpTheme
import app.shutterup.ui.theme.themeTint

@Composable
fun ThemeChip(
    label: String,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val resolvedTint = tint.takeOrElse { LocalThemeTint.current }
    val container = if (selected) resolvedTint else MaterialTheme.colorScheme.surface
    val border = if (selected) {
        null
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }
    val chipModifier = modifier.semantics { this.selected = selected }
    val content: @Composable () -> Unit = {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = chipModifier.heightIn(min = 48.dp),
            shape = CircleShape,
            color = container,
            border = border,
        ) {
            content()
        }
    } else {
        Surface(
            modifier = chipModifier,
            shape = CircleShape,
            color = container,
            border = border,
        ) {
            content()
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun ThemeChipPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        ThemeChipPreviewRow(darkTheme = false)
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThemeChipPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        ThemeChipPreviewRow(darkTheme = true)
    }
}

@Preview(name = "FontScale 2x", showBackground = true, fontScale = 2f)
@Composable
private fun ThemeChipPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        ThemeChipPreviewRow(darkTheme = false)
    }
}

@Composable
private fun ThemeChipPreviewRow(darkTheme: Boolean) {
    val tint = themeTint("reflections", MaterialTheme.colorScheme, darkTheme)
    Surface {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemeChip(label = "Reflections", selected = true, tint = tint, onClick = {})
            ThemeChip(label = "Quiet hours", selected = false, tint = tint, onClick = {})
            ThemeChip(label = "Still", selected = false, tint = tint, onClick = null)
        }
    }
}
