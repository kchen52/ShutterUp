package app.shutterup.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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
@Config(sdk = [35], qualifiers = "w400dp-h900dp")
class HomeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                HomeScreen(state = sampleHomeState())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun homeDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                HomeScreen(state = sampleHomeState())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
