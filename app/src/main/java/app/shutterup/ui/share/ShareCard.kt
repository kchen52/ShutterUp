package app.shutterup.ui.share

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.shutterup.domain.share.ShareCardContent
import app.shutterup.ui.components.Kicker
import app.shutterup.ui.theme.FrauncesHeadline
import app.shutterup.ui.theme.LocalThemeTint
import app.shutterup.ui.theme.ProvideThemeTint
import app.shutterup.ui.theme.ShutterUpTheme
import java.time.LocalDate

/**
 * Composed share card (DESIGN §4.11). Real type scale and Fraunces —
 * not a Canvas approximation. The note is not a field on [content]
 * and must never appear here.
 */
@Composable
fun ShareCard(
    content: ShareCardContent,
    photo: ImageBitmap,
    modifier: Modifier = Modifier,
) {
    val tint = LocalThemeTint.current
    val surface = if (tint != Color.Unspecified) {
        tint
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val photoShape = RoundedCornerShape(16.dp)
    val hairline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val aspect = photo.width.toFloat() / photo.height.toFloat().coerceAtLeast(1f)
    Column(
        modifier = modifier
            .width(ShareCardMetrics.WidthDp)
            .background(surface)
            .padding(24.dp),
    ) {
        Image(
            bitmap = photo,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(photoShape)
                .border(1.dp, hairline, photoShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(20.dp))
        Kicker(text = content.kicker)
        Spacer(Modifier.height(8.dp))
        Text(
            text = content.title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = WORDMARK,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FrauncesHeadline,
                fontWeight = FontWeight.W500,
            ),
        )
    }
}

/** Compact column at 3× density → 1080 px, a typical messaging / photo-feed width. */
object ShareCardMetrics {
    val WidthDp = 360.dp
    const val DensityScale: Float = 3f
    const val WidthPx: Int = 1080
    const val PhotoLongestPx: Int = 1080
}

private const val WORDMARK = "ShutterUp"

/** SPEC §7.1 title cap. 40 characters; the longest title that can actually ship. */
internal const val TITLE_AT_LIMIT: String = "Find the last remaining scrap of sky now"

internal fun sampleShareContent(
    title: String = "Find the sky in a puddle",
    theme: String = "Reflections",
): ShareCardContent = ShareCardContent.from(
    date = LocalDate.of(2026, 9, 19),
    theme = theme,
    title = title,
)

/**
 * Deterministic stand-in for a real still: dusk sky, a building with
 * lit windows, wet pavement, a puddle reflecting the sky. Drawn in
 * normalised coordinates so portrait, landscape and square all show
 * the same scene. This is the photograph — not a mat around it.
 */
internal fun sampleSharePhoto(width: Int, height: Int): ImageBitmap {
    val image = ImageBitmap(width, height)
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = ComposeCanvas(image),
        size = Size(width.toFloat(), height.toFloat()),
    ) {
        drawPuddleScene()
    }
    return image
}

private fun DrawScope.drawPuddleScene() {
    val w = size.width
    val h = size.height
    val horizon = h * 0.50f
    val skyTop = Color(0xFF4E6F93)
    val skyHorizon = Color(0xFFD7C5A8)
    val bands = 28
    for (i in 0 until bands) {
        val t = i / (bands - 1f)
        val y0 = horizon * i / bands
        val y1 = horizon * (i + 1) / bands
        drawRect(
            color = lerp(skyTop, skyHorizon, t),
            topLeft = Offset(0f, y0),
            size = Size(w, (y1 - y0) + 1f),
        )
    }
    drawOval(
        color = Color(0x66F4EDE4),
        topLeft = Offset(w * 0.42f, h * 0.06f),
        size = Size(w * 0.48f, h * 0.14f),
    )
    drawRect(
        color = Color(0xFF3A3734),
        topLeft = Offset(0f, horizon),
        size = Size(w, h - horizon),
    )
    for (i in 0..10) {
        val t = i / 10f
        val y = horizon + (h - horizon) * (0.08f + t * 0.85f)
        drawLine(
            color = Color(0xFF2E2B29),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = h * 0.006f,
        )
    }
    val buildingLeft = 0f
    val buildingWidth = w * 0.34f
    val buildingTop = h * 0.10f
    drawRect(
        color = Color(0xFF2A2623),
        topLeft = Offset(buildingLeft, buildingTop),
        size = Size(buildingWidth, horizon - buildingTop),
    )
    val cols = 3
    val rows = 5
    val insetX = buildingWidth * 0.16f
    val insetY = (horizon - buildingTop) * 0.10f
    val cellW = (buildingWidth - insetX * 2f) / cols
    val cellH = (horizon - buildingTop - insetY * 2f) / rows
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val lit = (row + col) % 3 != 0
            val window = if (lit) Color(0xFFE2B15A) else Color(0xFF1A1714)
            drawRect(
                color = window,
                topLeft = Offset(
                    buildingLeft + insetX + col * cellW + cellW * 0.18f,
                    buildingTop + insetY + row * cellH + cellH * 0.20f,
                ),
                size = Size(cellW * 0.64f, cellH * 0.58f),
            )
        }
    }
    val poleX = w * 0.72f
    drawRect(
        color = Color(0xFF1F1C1A),
        topLeft = Offset(poleX, h * 0.18f),
        size = Size(w * 0.018f, horizon - h * 0.18f),
    )
    drawCircle(
        color = Color(0xFFE8C56B),
        radius = w * 0.028f,
        center = Offset(poleX + w * 0.009f, h * 0.17f),
    )
    val puddleW = w * 0.46f
    val puddleH = h * 0.16f
    val puddleTopLeft = Offset(w * 0.38f, h * 0.70f)
    drawOval(
        color = Color(0xFF6D8AA8),
        topLeft = puddleTopLeft,
        size = Size(puddleW, puddleH),
    )
    drawOval(
        color = Color(0xFFC9B79A),
        topLeft = Offset(puddleTopLeft.x + puddleW * 0.18f, puddleTopLeft.y + puddleH * 0.12f),
        size = Size(puddleW * 0.46f, puddleH * 0.38f),
    )
    drawOval(
        color = Color(0x55FFFFFF),
        topLeft = puddleTopLeft,
        size = Size(puddleW, puddleH),
        style = Stroke(width = h * 0.008f),
    )
}

@Preview(name = "Portrait light", showBackground = true, widthDp = 360)
@Composable
private fun ShareCardPreviewPortraitLight() {
    ShutterUpTheme(darkTheme = false) {
        ProvideThemeTint(theme = "Reflections", darkTheme = false) {
            ShareCard(content = sampleShareContent(), photo = sampleSharePhoto(300, 400))
        }
    }
}

@Preview(
    name = "Portrait dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ShareCardPreviewPortraitDark() {
    ShutterUpTheme(darkTheme = true) {
        ProvideThemeTint(theme = "Reflections", darkTheme = true) {
            ShareCard(content = sampleShareContent(), photo = sampleSharePhoto(300, 400))
        }
    }
}

@Preview(name = "Landscape light", showBackground = true, widthDp = 360)
@Composable
private fun ShareCardPreviewLandscapeLight() {
    ShutterUpTheme(darkTheme = false) {
        ProvideThemeTint(theme = "Reflections", darkTheme = false) {
            ShareCard(content = sampleShareContent(), photo = sampleSharePhoto(400, 300))
        }
    }
}

@Preview(
    name = "Landscape dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ShareCardPreviewLandscapeDark() {
    ShutterUpTheme(darkTheme = true) {
        ProvideThemeTint(theme = "Reflections", darkTheme = true) {
            ShareCard(content = sampleShareContent(), photo = sampleSharePhoto(400, 300))
        }
    }
}

@Preview(name = "Square light", showBackground = true, widthDp = 360)
@Composable
private fun ShareCardPreviewSquareLight() {
    ShutterUpTheme(darkTheme = false) {
        ProvideThemeTint(theme = "Reflections", darkTheme = false) {
            ShareCard(content = sampleShareContent(), photo = sampleSharePhoto(400, 400))
        }
    }
}

@Preview(
    name = "Square dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ShareCardPreviewSquareDark() {
    ShutterUpTheme(darkTheme = true) {
        ProvideThemeTint(theme = "Reflections", darkTheme = true) {
            ShareCard(content = sampleShareContent(), photo = sampleSharePhoto(400, 400))
        }
    }
}

@Preview(name = "Title limit light", showBackground = true, widthDp = 360)
@Composable
private fun ShareCardPreviewTitleLimitLight() {
    ShutterUpTheme(darkTheme = false) {
        ProvideThemeTint(theme = "Reflections", darkTheme = false) {
            ShareCard(
                content = sampleShareContent(title = TITLE_AT_LIMIT),
                photo = sampleSharePhoto(300, 400),
            )
        }
    }
}

@Preview(
    name = "Title limit dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ShareCardPreviewTitleLimitDark() {
    ShutterUpTheme(darkTheme = true) {
        ProvideThemeTint(theme = "Reflections", darkTheme = true) {
            ShareCard(
                content = sampleShareContent(title = TITLE_AT_LIMIT),
                photo = sampleSharePhoto(300, 400),
            )
        }
    }
}

@Preview(name = "Long title light", showBackground = true, widthDp = 360)
@Composable
private fun ShareCardPreviewLongTitleLight() {
    ShutterUpTheme(darkTheme = false) {
        ProvideThemeTint(theme = "Reflections", darkTheme = false) {
            ShareCard(
                content = sampleShareContent(
                    title = "Find the last remaining scrap of sky in a puddle after the rain has already gone and the street is almost dry",
                ),
                photo = sampleSharePhoto(300, 400),
            )
        }
    }
}

@Preview(
    name = "Long title dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ShareCardPreviewLongTitleDark() {
    ShutterUpTheme(darkTheme = true) {
        ProvideThemeTint(theme = "Reflections", darkTheme = true) {
            ShareCard(
                content = sampleShareContent(
                    title = "Find the last remaining scrap of sky in a puddle after the rain has already gone and the street is almost dry",
                ),
                photo = sampleSharePhoto(300, 400),
            )
        }
    }
}
