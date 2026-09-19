package app.shutterup.ui.components

import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.shutterup.ui.theme.ShutterUpTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** 24-unit viewport shared with ApertureIcon and `ic_aperture_check`. */
internal const val ApertureCheckViewport = 24f

/** 2 dp stroke at 24 dp, matching aperture / snowflake icon language. */
internal const val ApertureCheckStroke = 2f

internal const val ApertureCheckBladeCount = 6
internal const val ApertureCheckOuterRadius = 9f
internal const val ApertureCheckOpenInnerRadius = 3.4f
internal const val ApertureCheckClosedInnerRadius = 1.2f
internal const val ApertureCheckOpenTwistDeg = 28f
internal const val ApertureCheckClosedTwistDeg = 50f
internal const val ApertureCheckCloseEnd = 0.58f
internal const val ApertureCheckRevealStart = 0.46f
internal const val ApertureCheckFadeStart = 0.52f
internal const val ApertureCheckDurationMs = 700

/** M3 emphasized cubic (DESIGN §6). */
internal val ApertureCheckEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

/** Final check in the 24-unit viewport; keep in sync with `ic_aperture_check`. */
internal const val ApertureCheckStartX = 6f
internal const val ApertureCheckStartY = 12.5f
internal const val ApertureCheckMidX = 10.25f
internal const val ApertureCheckMidY = 16.75f
internal const val ApertureCheckEndX = 18.5f
internal const val ApertureCheckEndY = 7.5f

internal data class ApertureCheckFrame(
    val closeAmount: Float,
    val checkReveal: Float,
    val apertureAlpha: Float,
    val innerRadius: Float,
    val innerTwistDeg: Float,
    val irisRotationDeg: Float,
)

/**
 * Six-blade aperture that can close and resolve into a rounded check.
 *
 * Pass [progress] (0 = open iris, 1 = check) for deterministic screenshots
 * and previews. When [progress] is null and [playOnce] is true, the
 * transition runs once and is remembered across recomposition and fold.
 * Static callers never create saveable animation state or read animator
 * settings.
 */
@Composable
fun ApertureCheckMark(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = LocalContentColor.current,
    progress: Float? = null,
    playOnce: Boolean = false,
    contentDescription: String? = null,
) {
    val inspect = LocalInspectionMode.current
    if (apertureCheckAnimates(progress, playOnce, inspect)) {
        ApertureCheckPlayOnce(
            modifier = modifier,
            size = size,
            color = color,
            contentDescription = contentDescription,
        )
    } else {
        ApertureCheckStatic(
            modifier = modifier,
            size = size,
            color = color,
            progress = progress?.coerceIn(0f, 1f) ?: 1f,
            contentDescription = contentDescription,
        )
    }
}

/** True only for live Completion playback; static/preview/explicit progress skip it. */
internal fun apertureCheckAnimates(
    progress: Float?,
    playOnce: Boolean,
    inspect: Boolean,
): Boolean = progress == null && playOnce && !inspect

@Composable
private fun ApertureCheckStatic(
    modifier: Modifier,
    size: Dp,
    color: Color,
    progress: Float,
    contentDescription: String?,
) {
    ApertureCheckCanvas(
        modifier = modifier,
        size = size,
        color = color,
        progress = progress,
        contentDescription = contentDescription,
    )
}

@Composable
private fun ApertureCheckPlayOnce(
    modifier: Modifier,
    size: Dp,
    color: Color,
    contentDescription: String?,
) {
    val motionEnabled = rememberApertureMotionEnabled()
    var played by rememberSaveable { mutableStateOf(false) }
    val animatable = remember { Animatable(if (!motionEnabled || played) 1f else 0f) }
    LaunchedEffect(motionEnabled) {
        if (!motionEnabled || played) {
            animatable.snapTo(1f)
            played = true
            return@LaunchedEffect
        }
        played = true
        animatable.snapTo(0f)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = ApertureCheckDurationMs,
                easing = ApertureCheckEasing,
            ),
        )
    }
    ApertureCheckCanvas(
        modifier = modifier,
        size = size,
        color = color,
        progress = animatable.value,
        contentDescription = contentDescription,
    )
}

@Composable
private fun ApertureCheckCanvas(
    modifier: Modifier,
    size: Dp,
    color: Color,
    progress: Float,
    contentDescription: String?,
) {
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier
    }
    Canvas(
        modifier = modifier
            .size(size)
            .then(semanticsModifier),
    ) {
        drawApertureCheck(
            frame = apertureCheckFrame(progress),
            color = color,
        )
    }
}

@Composable
private fun rememberApertureMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) != 0f
    }
}

internal fun apertureCheckFrame(progress: Float): ApertureCheckFrame {
    val p = progress.coerceIn(0f, 1f)
    val closeAmount = (p / ApertureCheckCloseEnd).coerceIn(0f, 1f)
    val checkReveal = ((p - ApertureCheckRevealStart) / (1f - ApertureCheckRevealStart))
        .coerceIn(0f, 1f)
    val apertureAlpha = 1f - ((p - ApertureCheckFadeStart) / (1f - ApertureCheckFadeStart))
        .coerceIn(0f, 1f)
    return ApertureCheckFrame(
        closeAmount = closeAmount,
        checkReveal = checkReveal,
        apertureAlpha = apertureAlpha,
        innerRadius = lerp(
            ApertureCheckOpenInnerRadius,
            ApertureCheckClosedInnerRadius,
            closeAmount,
        ),
        innerTwistDeg = lerp(
            ApertureCheckOpenTwistDeg,
            ApertureCheckClosedTwistDeg,
            closeAmount,
        ),
        irisRotationDeg = closeAmount * 18f,
    )
}

internal fun apertureCheckPath(): Path = Path().apply {
    moveTo(ApertureCheckStartX, ApertureCheckStartY)
    lineTo(ApertureCheckMidX, ApertureCheckMidY)
    lineTo(ApertureCheckEndX, ApertureCheckEndY)
}

/** Path data for `ic_aperture_check`; keep the drawable in lockstep. */
internal fun apertureCheckVectorPathData(): String {
    fun n(value: Float): String {
        val asInt = value.toInt()
        return if (value == asInt.toFloat()) asInt.toString() else value.toString()
    }
    return "M${n(ApertureCheckStartX)},${n(ApertureCheckStartY)} " +
        "L${n(ApertureCheckMidX)},${n(ApertureCheckMidY)} " +
        "L${n(ApertureCheckEndX)},${n(ApertureCheckEndY)}"
}

internal fun DrawScope.drawApertureCheck(frame: ApertureCheckFrame, color: Color) {
    val scale = min(size.width, size.height) / ApertureCheckViewport
    if (scale <= 0f) return
    val cx = size.width / 2f
    val cy = size.height / 2f
    val stroke = Stroke(
        width = ApertureCheckStroke * scale,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
    if (frame.apertureAlpha > 0f) {
        val apertureColor = color.copy(alpha = color.alpha * frame.apertureAlpha)
        drawCircle(
            color = apertureColor,
            radius = ApertureCheckOuterRadius * scale,
            center = Offset(cx, cy),
            style = stroke,
        )
        for (i in 0 until ApertureCheckBladeCount) {
            val startDeg = i * 60f - 90f + frame.irisRotationDeg
            val innerDeg = startDeg + frame.innerTwistDeg
            val endDeg = startDeg + 60f
            val path = Path().apply {
                val start = polar(cx, cy, ApertureCheckOuterRadius * scale, startDeg)
                val inner = polar(cx, cy, frame.innerRadius * scale, innerDeg)
                val end = polar(cx, cy, ApertureCheckOuterRadius * scale, endDeg)
                moveTo(start.x, start.y)
                lineTo(inner.x, inner.y)
                lineTo(end.x, end.y)
            }
            drawPath(path, apertureColor, style = stroke)
        }
    }
    if (frame.checkReveal > 0f) {
        val full = Path().apply {
            val start = viewport(cx, cy, scale, ApertureCheckStartX, ApertureCheckStartY)
            val mid = viewport(cx, cy, scale, ApertureCheckMidX, ApertureCheckMidY)
            val end = viewport(cx, cy, scale, ApertureCheckEndX, ApertureCheckEndY)
            moveTo(start.x, start.y)
            lineTo(mid.x, mid.y)
            lineTo(end.x, end.y)
        }
        val drawn = if (frame.checkReveal >= 1f) {
            full
        } else {
            val dst = Path()
            val measure = PathMeasure()
            measure.setPath(full, false)
            measure.getSegment(0f, measure.length * frame.checkReveal, dst, true)
            dst
        }
        drawPath(drawn, color, style = stroke)
    }
}

private fun viewport(
    cx: Float,
    cy: Float,
    scale: Float,
    x: Float,
    y: Float,
): Offset = Offset(
    cx + (x - ApertureCheckViewport / 2f) * scale,
    cy + (y - ApertureCheckViewport / 2f) * scale,
)

private fun polar(cx: Float, cy: Float, radius: Float, degrees: Float): Offset {
    val rad = Math.toRadians(degrees.toDouble())
    return Offset(
        cx + radius * cos(rad).toFloat(),
        cy + radius * sin(rad).toFloat(),
    )
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

@Preview(name = "Light", showBackground = true)
@Composable
private fun ApertureCheckPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        ApertureCheckPreviewRow()
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ApertureCheckPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        ApertureCheckPreviewRow()
    }
}

@Composable
private fun ApertureCheckPreviewRow() {
    Surface {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ApertureCheckMark(
                size = 24.dp,
                color = MaterialTheme.colorScheme.onSurface,
                progress = 0f,
            )
            ApertureCheckMark(
                size = 24.dp,
                color = MaterialTheme.colorScheme.onSurface,
                progress = 0.5f,
            )
            ApertureCheckMark(
                size = 24.dp,
                color = MaterialTheme.colorScheme.secondary,
                progress = 1f,
            )
            ApertureCheckMark(
                size = 18.dp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                progress = 1f,
            )
        }
    }
}
