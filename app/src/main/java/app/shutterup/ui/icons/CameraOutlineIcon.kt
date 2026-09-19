package app.shutterup.ui.icons

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.shutterup.ui.theme.ShutterUpTheme

/**
 * Front-on compact camera: wide body, viewfinder bump, big protruding lens.
 *
 * Drawn at a 96 dp viewport with a 2.4 dp stroke so the silhouette stays
 * readable when shown large in the Today card corner (DESIGN.md §2.5).
 */
val CameraOutlineIcon: ImageVector = ImageVector.Builder(
    name = "CameraOutlineIcon",
    defaultWidth = 96.dp,
    defaultHeight = 96.dp,
    viewportWidth = 96f,
    viewportHeight = 96f,
).apply {
    // One silhouette: wide body + viewfinder so it reads as a single object.
    strokePath {
        moveTo(16f, 78f)
        lineTo(80f, 78f)
        arcTo(10f, 10f, 0f, false, false, 90f, 68f)
        lineTo(90f, 46f)
        arcTo(10f, 10f, 0f, false, false, 80f, 36f)
        lineTo(78f, 36f)
        lineTo(78f, 22f)
        arcTo(8f, 8f, 0f, false, false, 70f, 14f)
        lineTo(58f, 14f)
        arcTo(8f, 8f, 0f, false, false, 50f, 22f)
        lineTo(50f, 36f)
        lineTo(16f, 36f)
        arcTo(10f, 10f, 0f, false, false, 6f, 46f)
        lineTo(6f, 68f)
        arcTo(10f, 10f, 0f, false, false, 16f, 78f)
        close()
    }
    // Lens barrel sitting proud of the body — the "this is a camera" cue.
    strokePath {
        moveTo(64f, 60f)
        arcTo(22f, 22f, 0f, true, true, 20f, 60f)
        arcTo(22f, 22f, 0f, true, true, 64f, 60f)
        close()
    }
    strokePath {
        moveTo(55f, 60f)
        arcTo(13f, 13f, 0f, true, true, 29f, 60f)
        arcTo(13f, 13f, 0f, true, true, 55f, 60f)
        close()
    }
    strokePath {
        moveTo(48f, 60f)
        arcTo(6f, 6f, 0f, true, true, 36f, 60f)
        arcTo(6f, 6f, 0f, true, true, 48f, 60f)
        close()
    }
    // Flash window on the face, left of the lens.
    strokePath {
        moveTo(16f, 44f)
        lineTo(26f, 44f)
        arcTo(3f, 3f, 0f, false, true, 29f, 47f)
        lineTo(29f, 51f)
        arcTo(3f, 3f, 0f, false, true, 26f, 54f)
        lineTo(16f, 54f)
        arcTo(3f, 3f, 0f, false, true, 13f, 51f)
        lineTo(13f, 47f)
        arcTo(3f, 3f, 0f, false, true, 16f, 44f)
        close()
    }
    // Shutter release on the viewfinder deck.
    strokePath {
        moveTo(74f, 28f)
        arcTo(4.5f, 4.5f, 0f, true, true, 65f, 28f)
        arcTo(4.5f, 4.5f, 0f, true, true, 74f, 28f)
        close()
    }
}.build()

private fun ImageVector.Builder.strokePath(
    builder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
) {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.4f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = builder,
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun CameraOutlinePreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface {
            Icon(
                imageVector = CameraOutlineIcon,
                contentDescription = null,
                modifier = Modifier
                    .padding(16.dp)
                    .size(96.dp),
            )
        }
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CameraOutlinePreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface {
            Icon(
                imageVector = CameraOutlineIcon,
                contentDescription = null,
                modifier = Modifier
                    .padding(16.dp)
                    .size(96.dp),
            )
        }
    }
}
