package app.shutterup.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTintTest {

    @Test
    fun sameTheme_sameColorAcrossCalls() {
        val scheme = lightColorScheme()
        val first = themeTint("Reflections", scheme, darkTheme = false)
        val second = themeTint("Reflections", scheme, darkTheme = false)
        assertEquals(first, second)
    }

    @Test
    fun differentThemes_usuallyDiffer() {
        val scheme = lightColorScheme()
        val themes = listOf(
            "reflections",
            "light",
            "shadows",
            "people",
            "places",
            "objects",
            "nature",
            "urban",
            "stillness",
            "motion",
        )
        val distinct = themes.map { themeTint(it, scheme, darkTheme = false) }.toSet()
        assertTrue(
            "expected at least 8 distinct tints, got ${distinct.size}",
            distinct.size >= 8,
        )
    }

    @Test
    fun harmonizedHue_isWithin15DegreesOfRawHue() {
        val theme = "reflections"
        val rawHue = rawHue(theme)
        val primary = Color.hsl(hue = rawHue + 40f, saturation = 0.40f, lightness = 0.40f)
        val scheme = lightColorScheme(primary = primary)
        val result = themeTint(theme, scheme, darkTheme = false)
        val recovered = recoverOverlay(result, scheme.surfaceContainerLow, 0.12f)
        val recoveredHue = hueOf(recovered)
        assertTrue(
            "harmonized hue $recoveredHue should be within 15 of raw $rawHue",
            hueDelta(recoveredHue, rawHue) <= 15f + 0.5f,
        )
    }

    @Test
    fun resultBlendsBaseWithHarmonizedAtDocumentedAlpha() {
        val theme = "reflections"
        val lightScheme = lightColorScheme()
        val lightResult = themeTint(theme, lightScheme, darkTheme = false)
        val lightBase = lightScheme.surfaceContainerLow
        val lightOverlay = recoverOverlay(lightResult, lightBase, 0.12f)
        assertNotEquals(lightBase, lightResult)
        assertTrue(isBetween(lightResult, lightBase, lightOverlay))

        val darkScheme = darkColorScheme()
        val darkResult = themeTint(theme, darkScheme, darkTheme = true)
        val darkBase = darkScheme.surfaceContainer
        val darkOverlay = recoverOverlay(darkResult, darkBase, 0.18f)
        assertNotEquals(darkBase, darkResult)
        assertTrue(isBetween(darkResult, darkBase, darkOverlay))
    }

    @Test
    fun themeAccent_isFullStrengthNotBlended() {
        val scheme = lightColorScheme()
        val blended = themeTint("reflections", scheme, darkTheme = false)
        val accent = themeAccent("reflections", scheme)
        assertNotEquals(blended, accent)
        assertEquals(themeAccent("reflections", scheme), themeAccent("Reflections", scheme))
    }

    @Test
    fun emptyTheme_doesNotCrash() {
        themeTint("", lightColorScheme(), darkTheme = false)
        themeTint("", darkColorScheme(), darkTheme = true)
    }

    @Test
    fun complementaryAccent_isOppositePrimaryHue() {
        val primary = Color.hsl(hue = 30f, saturation = 0.40f, lightness = 0.40f)
        val scheme = lightColorScheme(primary = primary)
        val accent = complementaryAccent(scheme, darkTheme = false)
        assertTrue(
            "expected ~210°, got ${hueOf(accent)}",
            hueDelta(hueOf(accent), 210f) <= 2f,
        )
    }

    @Test
    fun complementaryAccent_tracksPrimaryAcrossHues() {
        for (hue in listOf(0f, 60f, 120f, 200f, 300f)) {
            val scheme = lightColorScheme(
                primary = Color.hsl(hue = hue, saturation = 0.45f, lightness = 0.40f),
            )
            val accent = complementaryAccent(scheme, darkTheme = false)
            val expected = (hue + 180f) % 360f
            assertTrue(
                "primary $hue should complement to $expected, got ${hueOf(accent)}",
                hueDelta(hueOf(accent), expected) <= 2f,
            )
        }
    }

    @Test
    fun complementaryAccent_lightensInDarkTheme() {
        val primary = Color.hsl(hue = 240f, saturation = 0.40f, lightness = 0.40f)
        val scheme = darkColorScheme(primary = primary)
        val light = complementaryAccent(scheme, darkTheme = false)
        val dark = complementaryAccent(scheme, darkTheme = true)
        assertTrue(
            "dark-theme complement should be lighter than light-theme",
            luminance(dark) > luminance(light),
        )
    }

    private fun rawHue(theme: String): Float =
        ((theme.lowercase().hashCode() and Int.MAX_VALUE) % 360).toFloat()

    private fun recoverOverlay(result: Color, base: Color, alpha: Float): Color {
        fun channel(resultChannel: Float, baseChannel: Float): Float =
            (resultChannel - (1f - alpha) * baseChannel) / alpha
        return Color(
            red = channel(result.red, base.red).coerceIn(0f, 1f),
            green = channel(result.green, base.green).coerceIn(0f, 1f),
            blue = channel(result.blue, base.blue).coerceIn(0f, 1f),
            alpha = 1f,
        )
    }

    private fun isBetween(result: Color, a: Color, b: Color): Boolean {
        fun between(value: Float, start: Float, end: Float): Boolean {
            val lo = minOf(start, end) - 0.002f
            val hi = maxOf(start, end) + 0.002f
            return value in lo..hi
        }
        return between(result.red, a.red, b.red) &&
            between(result.green, a.green, b.green) &&
            between(result.blue, a.blue, b.blue)
    }

    private fun hueDelta(a: Float, b: Float): Float {
        var delta = kotlin.math.abs(a - b) % 360f
        if (delta > 180f) delta = 360f - delta
        return delta
    }

    private fun luminance(color: Color): Float =
        0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue

    private fun hueOf(color: Color): Float {
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        if (delta == 0f) return 0f
        val h = when (max) {
            r -> ((g - b) / delta) % 6f
            g -> ((b - r) / delta) + 2f
            else -> ((r - g) / delta) + 4f
        }
        var hue = (h * 60f) % 360f
        if (hue < 0f) hue += 360f
        return hue
    }
}
