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

internal fun sampleShareContent(
    title: String = "Find the sky in a puddle",
    theme: String = "Reflections",
): ShareCardContent = ShareCardContent.from(
    date = LocalDate.of(2026, 9, 19),
    theme = theme,
    title = title,
)

internal fun sampleSharePhoto(width: Int, height: Int): ImageBitmap {
    val image = ImageBitmap(width, height)
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = ComposeCanvas(image),
        size = Size(width.toFloat(), height.toFloat()),
    ) {
        drawRect(Color(0xFF8A9BA8))
        drawRect(
            color = Color(0xFF5C6B75),
            topLeft = Offset(size.width * 0.18f, size.height * 0.18f),
            size = Size(size.width * 0.64f, size.height * 0.64f),
        )
    }
    return image
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
