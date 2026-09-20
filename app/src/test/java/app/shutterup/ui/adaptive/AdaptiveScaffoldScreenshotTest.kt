package app.shutterup.ui.adaptive

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
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
@Config(sdk = [35], qualifiers = "w1000dp-h800dp")
class AdaptiveScaffoldScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun expandedScaffoldLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                SampleScaffold(
                    selected = ShutterUpDestination.Settings,
                    layoutType = NavigationSuiteType.NavigationRail,
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun expandedScaffoldDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                SampleScaffold(
                    selected = ShutterUpDestination.Settings,
                    layoutType = NavigationSuiteType.NavigationRail,
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun expandedScaffoldGeneratingLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                SampleScaffold(
                    selected = ShutterUpDestination.Settings,
                    layoutType = NavigationSuiteType.NavigationRail,
                    generationMessage = "Generating prompts for a seven-day series",
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp")
    fun compactScaffoldGeneratingLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                SampleScaffold(
                    selected = ShutterUpDestination.Home,
                    generationMessage = "Generating prompts for Monday",
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
