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
    // Soft-box body, pulled to the viewport so the corner can hang off the card.
    strokePath {
        moveTo(18f, 32f)
        lineTo(78f, 32f)
        arcTo(10f, 10f, 0f, false, true, 88f, 42f)
        lineTo(88f, 78f)
        arcTo(10f, 10f, 0f, false, true, 78f, 88f)
        lineTo(18f, 88f)
        arcTo(10f, 10f, 0f, false, true, 8f, 78f)
        lineTo(8f, 42f)
        arcTo(10f, 10f, 0f, false, true, 18f, 32f)
        close()
    }
    // Viewfinder hump — the bit that peeks over the card edge.
    strokePath {
        moveTo(58f, 12f)
        lineTo(74f, 12f)
        arcTo(6f, 6f, 0f, false, true, 80f, 18f)
        lineTo(80f, 32f)
        lineTo(52f, 32f)
        lineTo(52f, 18f)
        arcTo(6f, 6f, 0f, false, true, 58f, 12f)
        close()
    }
    // Lens — a curious "eye".
    strokePath {
        moveTo(60f, 60f)
        arcTo(18f, 18f, 0f, true, true, 24f, 60f)
        arcTo(18f, 18f, 0f, true, true, 60f, 60f)
        close()
    }
    strokePath {
        moveTo(51f, 60f)
        arcTo(9f, 9f, 0f, true, true, 33f, 60f)
        arcTo(9f, 9f, 0f, true, true, 51f, 60f)
        close()
    }
    // Catchlight, like a wink of sky in the glass.
    strokePath {
        moveTo(34f, 52f)
        arcTo(6.5f, 6.5f, 0f, false, true, 42f, 49f)
    }
    // Shutter button.
    strokePath {
        moveTo(81f, 42f)
        arcTo(4f, 4f, 0f, true, true, 73f, 42f)
        arcTo(4f, 4f, 0f, true, true, 81f, 42f)
        close()
    }
    // Flash window.
    strokePath {
        moveTo(16f, 42f)
        lineTo(28f, 42f)
        arcTo(3f, 3f, 0f, false, true, 31f, 45f)
        lineTo(31f, 51f)
        arcTo(3f, 3f, 0f, false, true, 28f, 54f)
        lineTo(16f, 54f)
        arcTo(3f, 3f, 0f, false, true, 13f, 51f)
        lineTo(13f, 45f)
        arcTo(3f, 3f, 0f, false, true, 16f, 42f)
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
