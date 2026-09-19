package app.shutterup.ui.feed

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
class FeedScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun feedLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                FeedScreen(state = sampleFeedState(), onOpenDay = {})
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun feedDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                FeedScreen(state = sampleFeedState(), onOpenDay = {})
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
