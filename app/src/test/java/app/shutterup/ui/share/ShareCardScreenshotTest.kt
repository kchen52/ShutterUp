package app.shutterup.ui.share

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import app.shutterup.ui.theme.ProvideThemeTint
import app.shutterup.ui.theme.ShutterUpTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ShareCardScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shareCardPortraitLight() {
        capture(darkTheme = false, width = 300, height = 400)
    }

    @Test
    fun shareCardPortraitDark() {
        capture(darkTheme = true, width = 300, height = 400)
    }

    @Test
    fun shareCardLandscapeLight() {
        capture(darkTheme = false, width = 400, height = 300)
    }

    @Test
    fun shareCardLandscapeDark() {
        capture(darkTheme = true, width = 400, height = 300)
    }

    @Test
    fun shareCardLongTitleLight() {
        capture(
            darkTheme = false,
            width = 300,
            height = 400,
            title = LONG_TITLE,
        )
    }

    @Test
    fun shareCardLongTitleDark() {
        capture(
            darkTheme = true,
            width = 300,
            height = 400,
            title = LONG_TITLE,
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
        private const val LONG_TITLE =
            "Find the last remaining scrap of sky in a puddle after the rain has already gone and the street is almost dry"
    }
}
