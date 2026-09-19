package app.shutterup.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shutterup.R
import app.shutterup.ui.components.Kicker

@OptIn(ExperimentalTextApi::class)
val FrauncesDisplay: FontFamily = FontFamily(
    Font(
        resId = R.font.fraunces_variable,
        weight = FontWeight.W500,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500),
            FontVariation.Setting("opsz", 144f),
        ),
    ),
)

@OptIn(ExperimentalTextApi::class)
val FrauncesHeadline: FontFamily = FontFamily(
    Font(
        resId = R.font.fraunces_variable,
        weight = FontWeight.W500,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500),
            FontVariation.Setting("opsz", 48f),
        ),
    ),
)

val ShutterUpTypography: Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = FrauncesDisplay,
        fontWeight = FontWeight.W500,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FrauncesHeadline,
        fontWeight = FontWeight.W500,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FrauncesHeadline,
        fontWeight = FontWeight.W500,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FrauncesHeadline,
        fontWeight = FontWeight.W500,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
)

// Fallback seed #6B5B4E. The try/catch is a dead path on our minSdk (34),
// which always has dynamic colour; it honors DESIGN.md §2.1.
private val FallbackSeed = Color(0xFF6B5B4E)

@Composable
fun ShutterUpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = try {
        if (darkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    } catch (_: Throwable) {
        if (darkTheme) {
            darkColorScheme(primary = FallbackSeed)
        } else {
            lightColorScheme(primary = FallbackSeed)
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ShutterUpTypography,
        content = content,
    )
}

@Composable
internal fun ThemeTypeSampler() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Find the sky in a puddle", style = MaterialTheme.typography.displaySmall)
            Text("September 2026", style = MaterialTheme.typography.headlineMedium)
            Text("First light.", style = MaterialTheme.typography.headlineSmall)
            Text("Reflections", style = MaterialTheme.typography.titleLarge)
            Text(
                "Turn the world upside down using any reflective surface you pass today.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Kicker(text = "Tuesday · Reflections")
        }
    }
}

@Preview(name = "Light")
@Composable
private fun ThemeTypeSamplerLightPreview() {
    ShutterUpTheme(darkTheme = false) {
        ThemeTypeSampler()
    }
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThemeTypeSamplerDarkPreview() {
    ShutterUpTheme(darkTheme = true) {
        ThemeTypeSampler()
    }
}

@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun ThemeTypeSamplerLargeFontPreview() {
    ShutterUpTheme {
        ThemeTypeSampler()
    }
}
