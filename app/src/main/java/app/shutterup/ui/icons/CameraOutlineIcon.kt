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
 * Decorative compact-camera outline for the Today card.
 *
 * Drawn at a 96 dp viewport with a 2.2 dp stroke so it stays editorial
 * (DESIGN.md §2.5) when shown large in the card corner.
 */
val CameraOutlineIcon: ImageVector = ImageVector.Builder(
    name = "CameraOutlineIcon",
    defaultWidth = 96.dp,
    defaultHeight = 96.dp,
    viewportWidth = 96f,
    viewportHeight = 96f,
).apply {
    // Soft-box body.
    strokePath {
        moveTo(22f, 36f)
        lineTo(74f, 36f)
        arcTo(10f, 10f, 0f, false, true, 84f, 46f)
        lineTo(84f, 72f)
        arcTo(10f, 10f, 0f, false, true, 74f, 82f)
        lineTo(22f, 82f)
        arcTo(10f, 10f, 0f, false, true, 12f, 72f)
        lineTo(12f, 46f)
        arcTo(10f, 10f, 0f, false, true, 22f, 36f)
        close()
    }
    // Viewfinder hump — the bit that peeks over the card edge.
    strokePath {
        moveTo(56f, 20f)
        lineTo(70f, 20f)
        arcTo(6f, 6f, 0f, false, true, 76f, 26f)
        lineTo(76f, 36f)
        lineTo(50f, 36f)
        lineTo(50f, 26f)
        arcTo(6f, 6f, 0f, false, true, 56f, 20f)
        close()
    }
    // Lens — a curious "eye".
    strokePath {
        moveTo(58f, 59f)
        arcTo(16f, 16f, 0f, true, true, 26f, 59f)
        arcTo(16f, 16f, 0f, true, true, 58f, 59f)
        close()
    }
    strokePath {
        moveTo(50f, 59f)
        arcTo(8f, 8f, 0f, true, true, 34f, 59f)
        arcTo(8f, 8f, 0f, true, true, 50f, 59f)
        close()
    }
    // Catchlight, like a wink of sky in the glass.
    strokePath {
        moveTo(36f, 52.5f)
        arcTo(5.5f, 5.5f, 0f, false, true, 42f, 50f)
    }
    // Shutter button.
    strokePath {
        moveTo(76.5f, 46f)
        arcTo(3.5f, 3.5f, 0f, true, true, 69.5f, 46f)
        arcTo(3.5f, 3.5f, 0f, true, true, 76.5f, 46f)
        close()
    }
    // Flash window.
    strokePath {
        moveTo(20f, 46f)
        lineTo(30f, 46f)
        arcTo(3f, 3f, 0f, false, true, 33f, 49f)
        lineTo(33f, 53f)
        arcTo(3f, 3f, 0f, false, true, 30f, 56f)
        lineTo(20f, 56f)
        arcTo(3f, 3f, 0f, false, true, 17f, 53f)
        lineTo(17f, 49f)
        arcTo(3f, 3f, 0f, false, true, 20f, 46f)
        close()
    }
}.build()

private fun ImageVector.Builder.strokePath(
    builder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
) {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.2f,
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
