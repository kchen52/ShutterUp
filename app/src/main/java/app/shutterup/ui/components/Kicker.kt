package app.shutterup.ui.components

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import app.shutterup.ui.theme.ShutterUpTheme
import java.util.Locale

@Composable
fun Kicker(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(Locale.ENGLISH),
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun KickerPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface { Kicker("Tuesday · Reflections") }
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun KickerPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface { Kicker("Tuesday · Reflections") }
    }
}

@Preview(name = "FontScale 2x", showBackground = true, fontScale = 2f)
@Composable
private fun KickerPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        Surface { Kicker("Tuesday · Reflections") }
    }
}
