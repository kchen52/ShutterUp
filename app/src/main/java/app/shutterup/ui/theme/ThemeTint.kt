package app.shutterup.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val LocalThemeTint: ProvidableCompositionLocal<Color> =
    compositionLocalOf { Color.Unspecified }

// Raw tint: S 55%, L 65% light / 45% dark — mid-chroma so the 12%/18% overlay stays paper-like.
private const val RAW_SATURATION = 0.55f
private const val RAW_LIGHTNESS_LIGHT = 0.65f
private const val RAW_LIGHTNESS_DARK = 0.45f
private const val HARMONIZE_FRACTION = 0.15f
private const val TINT_ALPHA_LIGHT = 0.12f
private const val TINT_ALPHA_DARK = 0.18f

/**
 * Paper-like theme overlay: the theme's harmonized hue blended onto a surface
 * container at 12 % (light) or 18 % (dark).
 *
 * [darkTheme] is a required third argument because DESIGN §2.1 keys both the
 * blend base (`surfaceContainerLow` vs `surfaceContainer`) and the overlay
 * alpha to appearance, not to [scheme] alone. Tests and previews construct
 * schemes independently of the system night mode, so callers pass the same
 * flag they used to choose the scheme.
 */
fun themeTint(theme: String, scheme: ColorScheme, darkTheme: Boolean): Color {
    val lightness = if (darkTheme) RAW_LIGHTNESS_DARK else RAW_LIGHTNESS_LIGHT
    val harmonized = Color(hslToColor(floatArrayOf(harmonizedHue(theme, scheme), RAW_SATURATION, lightness)))
    val base = if (darkTheme) scheme.surfaceContainer else scheme.surfaceContainerLow
    val alpha = if (darkTheme) TINT_ALPHA_DARK else TINT_ALPHA_LIGHT
    return Color(blendArgb(base.toArgb(), harmonized.toArgb(), alpha))
}

/** Harmonized theme hue at full strength for 4 dp accent bars and dots. */
fun themeAccent(theme: String, scheme: ColorScheme): Color {
    val darkTheme = scheme.surface.luminance() < 0.5f
    val lightness = if (darkTheme) RAW_LIGHTNESS_DARK else RAW_LIGHTNESS_LIGHT
    return Color(hslToColor(floatArrayOf(harmonizedHue(theme, scheme), RAW_SATURATION, lightness)))
}

@Composable
fun ProvideThemeTint(theme: String, darkTheme: Boolean, content: @Composable () -> Unit) {
    val tint = themeTint(theme, MaterialTheme.colorScheme, darkTheme)
    CompositionLocalProvider(LocalThemeTint provides tint, content = content)
}

private fun harmonizedHue(theme: String, scheme: ColorScheme): Float {
    val rawHue = (stableHash(theme.lowercase()) % 360).toFloat()
    val primaryHsl = FloatArray(3)
    colorToHsl(scheme.primary.toArgb(), primaryHsl)
    return wrapHue(rawHue + HARMONIZE_FRACTION * shortestArc(rawHue, primaryHsl[0]))
}

/** Java String.hashCode made non-negative; stable across runs and processes. */
private fun stableHash(value: String): Int = value.hashCode() and Int.MAX_VALUE

private fun shortestArc(fromHue: Float, toHue: Float): Float {
    var delta = (toHue - fromHue) % 360f
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f
    return delta
}

private fun wrapHue(hue: Float): Float {
    val wrapped = hue % 360f
    return if (wrapped < 0f) wrapped + 360f else wrapped
}

// ColorUtils.colorToHSL / HSLToColor / blendARGB via ARGB bit ops so unit tests stay JVM-safe
// (android.graphics.Color is unmocked on the unit-test classpath).
private fun colorToHsl(color: Int, outHsl: FloatArray) {
    try {
        ColorUtils.colorToHSL(color, outHsl)
    } catch (_: RuntimeException) {
        rgbToHsl((color shr 16) and 0xFF, (color shr 8) and 0xFF, color and 0xFF, outHsl)
    }
}

private fun hslToColor(hsl: FloatArray): Int {
    return try {
        ColorUtils.HSLToColor(hsl)
    } catch (_: RuntimeException) {
        hslToColorArgb(hsl)
    }
}

private fun blendArgb(color1: Int, color2: Int, ratio: Float): Int {
    return try {
        ColorUtils.blendARGB(color1, color2, ratio)
    } catch (_: RuntimeException) {
        val inverse = 1f - ratio
        val a = channel(color1, 24) * inverse + channel(color2, 24) * ratio
        val r = channel(color1, 16) * inverse + channel(color2, 16) * ratio
        val g = channel(color1, 8) * inverse + channel(color2, 8) * ratio
        val b = channel(color1, 0) * inverse + channel(color2, 0) * ratio
        (a.toInt() shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
    }
}

private fun channel(color: Int, shift: Int): Int = (color shr shift) and 0xFF

private fun rgbToHsl(r: Int, g: Int, b: Int, outHsl: FloatArray) {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val max = max(rf, max(gf, bf))
    val min = min(rf, min(gf, bf))
    val delta = max - min
    val l = (max + min) / 2f
    val h: Float
    val s: Float
    if (max == min) {
        h = 0f
        s = 0f
    } else {
        h = when (max) {
            rf -> ((gf - bf) / delta) % 6f
            gf -> ((bf - rf) / delta) + 2f
            else -> ((rf - gf) / delta) + 4f
        }
        s = delta / (1f - abs(2f * l - 1f))
    }
    var hue = (h * 60f) % 360f
    if (hue < 0f) hue += 360f
    outHsl[0] = hue.coerceIn(0f, 360f)
    outHsl[1] = s.coerceIn(0f, 1f)
    outHsl[2] = l.coerceIn(0f, 1f)
}

private fun hslToColorArgb(hsl: FloatArray): Int {
    val h = hsl[0]
    val s = hsl[1]
    val l = hsl[2]
    val c = (1f - abs(2f * l - 1f)) * s
    val m = l - 0.5f * c
    val x = c * (1f - abs((h / 60f % 2f) - 1f))
    val (r, g, b) = when ((h / 60f).toInt()) {
        0 -> Triple(c + m, x + m, m)
        1 -> Triple(x + m, c + m, m)
        2 -> Triple(m, c + m, x + m)
        3 -> Triple(m, x + m, c + m)
        4 -> Triple(x + m, m, c + m)
        else -> Triple(c + m, m, x + m)
    }
    val ri = (r * 255f).roundToInt().coerceIn(0, 255)
    val gi = (g * 255f).roundToInt().coerceIn(0, 255)
    val bi = (b * 255f).roundToInt().coerceIn(0, 255)
    return (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
}
