package app.shutterup.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
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
class TodayWidgetScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mediumLight() = capture(TodayWidgetPreviewSize.Medium, dark = false)

    @Test
    fun mediumDark() = capture(TodayWidgetPreviewSize.Medium, dark = true)

    @Test
    fun smallLight() = capture(TodayWidgetPreviewSize.Small, dark = false)

    @Test
    fun smallDark() = capture(TodayWidgetPreviewSize.Small, dark = true)

    private fun capture(size: TodayWidgetPreviewSize, dark: Boolean) {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = dark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        TodayWidgetPreviewContent(
                            state = sampleTodayWidgetState(),
                            size = size,
                            darkTheme = dark,
                        )
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
