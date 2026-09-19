package app.shutterup.ui.badges

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
@Config(sdk = [35], qualifiers = "w400dp-h2400dp")
class BadgesScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun badgesLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                BadgesScreen(state = sampleBadgesState())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun badgesDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                BadgesScreen(state = sampleBadgesState())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
