package app.shutterup.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.shutterup.ui.theme.LocalThemeTint
import app.shutterup.ui.theme.ShutterUpTheme
import app.shutterup.ui.theme.themeTint

@Composable
fun ConstraintCard(
    text: String,
    tint: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    val resolvedTint = tint.takeOrElse { LocalThemeTint.current }
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = resolvedTint),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Kicker(text = "CONSTRAINT")
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun ConstraintCardPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        ConstraintCardPreviewBody(darkTheme = false)
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ConstraintCardPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        ConstraintCardPreviewBody(darkTheme = true)
    }
}

@Preview(name = "FontScale 2x", showBackground = true, fontScale = 2f)
@Composable
private fun ConstraintCardPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        ConstraintCardPreviewBody(darkTheme = false)
    }
}

@Composable
private fun ConstraintCardPreviewBody(darkTheme: Boolean) {
    Surface {
        ConstraintCard(
            text = "Don't rotate the photo afterwards.",
            tint = themeTint("reflections", MaterialTheme.colorScheme, darkTheme),
            modifier = Modifier.padding(16.dp),
        )
    }
}
