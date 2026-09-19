package app.shutterup.ui.theme

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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
class ThemeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lightSampler() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                ThemeTypeSampler()
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun darkSampler() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                ThemeTypeSampler()
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
