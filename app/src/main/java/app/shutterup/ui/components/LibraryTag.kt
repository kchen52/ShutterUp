package app.shutterup.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.shutterup.ui.theme.ShutterUpTheme

@Composable
fun LibraryTag(
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outline
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, outline),
    ) {
        Text(
            text = "From the library",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun LibraryTagPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface { LibraryTag(modifier = Modifier.padding(16.dp)) }
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LibraryTagPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface { LibraryTag(modifier = Modifier.padding(16.dp)) }
    }
}

@Preview(name = "FontScale 2x", showBackground = true, fontScale = 2f)
@Composable
private fun LibraryTagPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        Surface { LibraryTag(modifier = Modifier.padding(16.dp)) }
    }
}
