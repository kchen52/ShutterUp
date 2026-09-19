package app.shutterup.ui.icons

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import kotlin.math.cos
import kotlin.math.sin

val ApertureIcon: ImageVector = ImageVector.Builder(
    name = "ApertureIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    strokePath {
        moveTo(21f, 12f)
        arcToRelative(9f, 9f, 0f, true, true, -18f, 0f)
        arcToRelative(9f, 9f, 0f, true, true, 18f, 0f)
        close()
    }
    for (i in 0 until 6) {
        val startDeg = i * 60f - 90f
        val innerDeg = startDeg + 28f
        val endDeg = startDeg + 60f
        strokePath {
            moveTo(polarX(9f, startDeg), polarY(9f, startDeg))
            lineTo(polarX(3.4f, innerDeg), polarY(3.4f, innerDeg))
            lineTo(polarX(9f, endDeg), polarY(9f, endDeg))
        }
    }
}.build()

val SnowflakeIcon: ImageVector = ImageVector.Builder(
    name = "SnowflakeIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    for (i in 0 until 6) {
        val deg = i * 60f - 90f
        strokePath {
            moveTo(polarX(1.4f, deg), polarY(1.4f, deg))
            lineTo(polarX(9f, deg), polarY(9f, deg))
        }
        val branchDegA = deg + 60f
        val branchDegB = deg - 60f
        val hubX = polarX(5.1f, deg)
        val hubY = polarY(5.1f, deg)
        strokePath {
            moveTo(
                hubX + 3.1f * cosDeg(branchDegA),
                hubY + 3.1f * sinDeg(branchDegA),
            )
            lineTo(hubX, hubY)
            lineTo(
                hubX + 3.1f * cosDeg(branchDegB),
                hubY + 3.1f * sinDeg(branchDegB),
            )
        }
    }
}.build()

private fun ImageVector.Builder.strokePath(
    builder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
) {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = builder,
    )
}

private fun polarX(radius: Float, degrees: Float): Float = 12f + radius * cosDeg(degrees)

private fun polarY(radius: Float, degrees: Float): Float = 12f + radius * sinDeg(degrees)

private fun cosDeg(degrees: Float): Float = cos(Math.toRadians(degrees.toDouble())).toFloat()

private fun sinDeg(degrees: Float): Float = sin(Math.toRadians(degrees.toDouble())).toFloat()

@Preview(name = "Light", showBackground = true)
@Composable
private fun IconsPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        IconPreviewRow()
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IconsPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        IconPreviewRow()
    }
}

@Preview(name = "FontScale 2x", showBackground = true, fontScale = 2f)
@Composable
private fun IconsPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        IconPreviewRow()
    }
}

@Composable
private fun IconPreviewRow() {
    Surface {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = ApertureIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Icon(
                imageVector = SnowflakeIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
