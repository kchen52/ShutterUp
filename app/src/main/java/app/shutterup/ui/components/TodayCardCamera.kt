package app.shutterup.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.shutterup.ui.icons.CameraOutlineIcon
import app.shutterup.ui.theme.ShutterUpTheme
import app.shutterup.ui.theme.complementaryAccent

/** Keeps the tilted outline inside the card's 28 dp corner. */
val TodayCardCameraPaddingEnd = 8.dp
val TodayCardCameraPaddingBottom = 12.dp
val TodayCardCameraSize = 108.dp
const val TodayCardCameraTilt = -10f

/**
 * Playful camera outline for the Today card corner.
 *
 * Decorative only — complementary to the system primary, drawn as a child of
 * the card so nothing renders past the rounded edge.
 */
@Composable
fun TodayCardCamera(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val tint = complementaryAccent(MaterialTheme.colorScheme, dark).copy(
        alpha = if (dark) 0.86f else 0.80f,
    )
    Icon(
        imageVector = CameraOutlineIcon,
        contentDescription = null,
        tint = tint,
        modifier = modifier,
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun TodayCardCameraPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            TodayCardCamera(
                modifier = Modifier
                    .padding(16.dp)
                    .rotate(TodayCardCameraTilt)
                    .size(TodayCardCameraSize),
            )
        }
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TodayCardCameraPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface {
            TodayCardCamera(
                modifier = Modifier
                    .padding(16.dp)
                    .rotate(TodayCardCameraTilt)
                    .size(TodayCardCameraSize),
            )
        }
    }
}
