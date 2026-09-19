package app.shutterup.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.shutterup.ui.theme.ShutterUpTheme

@Composable
fun NotificationPermissionCard(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Turn on notifications to get your daily prompt",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.Start)
                    .heightIn(min = 48.dp),
            ) {
                Text(text = "Open settings")
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun NotificationPermissionCardPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            NotificationPermissionCard(
                onOpenSettings = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NotificationPermissionCardPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface {
            NotificationPermissionCard(
                onOpenSettings = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "FontScale 2x", showBackground = true, fontScale = 2f)
@Composable
private fun NotificationPermissionCardPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            NotificationPermissionCard(
                onOpenSettings = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
