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

/** Inset that lets the outline sit on the corner and still spill off it. */
val TodayCardCameraInsetEnd = 14.dp
val TodayCardCameraInsetBottom = 10.dp
val TodayCardCameraSpillX = 2.dp
val TodayCardCameraSpillY = 4.dp
val TodayCardCameraSize = 104.dp
const val TodayCardCameraTilt = -15f

/**
 * Playful camera outline for the Today card corner.
 *
 * Decorative only — complementary to the system primary, tilted a few degrees
 * so it feels like a sticker that didn't quite land square.
 */
@Composable
fun TodayCardCamera(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val tint = complementaryAccent(MaterialTheme.colorScheme, dark).copy(
        alpha = if (dark) 0.82f else 0.74f,
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
