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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.shutterup.R
import app.shutterup.ui.theme.ShutterUpTheme
import app.shutterup.ui.theme.complementaryAccent

/**
 * Massive bottom-start sticker: the icon hangs slightly past the card's
 * left and bottom edges, and the card clips anything off the surface.
 * Callers overlay this with `matchParentSize` so the 320 dp graphic does
 * not stretch the wrap-content card.
 */
val TodayCardCameraSpillX = 40.dp
val TodayCardCameraSpillY = 80.dp
val TodayCardCameraSize = 320.dp
const val TodayCardCameraTilt = 16f

/**
 * Material Symbols photo_camera outline (weight 200) for the Today card corner.
 *
 * Decorative only — complementary to the system primary. Callers clip this
 * to the card shape so only the overlapping part is visible.
 */
@Composable
fun TodayCardCamera(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val tint = complementaryAccent(MaterialTheme.colorScheme, dark).copy(
        alpha = if (dark) 0.48f else 0.40f,
    )
    Icon(
        painter = painterResource(R.drawable.ic_photo_camera_outline),
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
