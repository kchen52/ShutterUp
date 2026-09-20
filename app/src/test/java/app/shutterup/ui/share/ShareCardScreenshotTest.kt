package app.shutterup.ui.share

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import app.shutterup.ui.theme.ProvideThemeTint
import app.shutterup.ui.theme.ShutterUpTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h1400dp")
class ShareCardScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun titleAtLimit_isFortyCharacters() {
        assertEquals(40, TITLE_AT_LIMIT.length)
    }

    @Test
    fun shareCardPortraitLight() {
        capture(darkTheme = false, width = 900, height = 1200)
    }

    @Test
    fun shareCardPortraitDark() {
        capture(darkTheme = true, width = 900, height = 1200)
    }

    @Test
    fun shareCardLandscapeLight() {
        capture(darkTheme = false, width = 1200, height = 900)
    }

    @Test
    fun shareCardLandscapeDark() {
        capture(darkTheme = true, width = 1200, height = 900)
    }

    @Test
    fun shareCardSquareLight() {
        capture(darkTheme = false, width = 1000, height = 1000)
    }

    @Test
    fun shareCardSquareDark() {
        capture(darkTheme = true, width = 1000, height = 1000)
    }

    @Test
    fun shareCardTitleLimitLight() {
        capture(
            darkTheme = false,
            width = 900,
            height = 1200,
            title = TITLE_AT_LIMIT,
        )
    }

    @Test
    fun shareCardTitleLimitDark() {
        capture(
            darkTheme = true,
            width = 900,
            height = 1200,
            title = TITLE_AT_LIMIT,
        )
    }

    @Test
    fun shareCardLongTitleLight() {
        capture(
            darkTheme = false,
            width = 900,
            height = 1200,
            title = OVERLONG_TITLE,
        )
    }

    @Test
    fun shareCardLongTitleDark() {
        capture(
            darkTheme = true,
            width = 900,
            height = 1200,
            title = OVERLONG_TITLE,
        )
    }

    private fun capture(
        darkTheme: Boolean,
        width: Int,
        height: Int,
        title: String = "Find the sky in a puddle",
    ) {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = darkTheme) {
                ProvideThemeTint(theme = "Reflections", darkTheme = darkTheme) {
                    ShareCard(
                        content = sampleShareContent(title = title),
                        photo = sampleSharePhoto(width, height),
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    companion object {
        private const val OVERLONG_TITLE =
            "Find the last remaining scrap of sky in a puddle after the rain has already gone and the street is almost dry"
    }
}
