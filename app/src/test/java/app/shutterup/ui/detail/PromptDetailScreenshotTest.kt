package app.shutterup.ui.detail

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
class PromptDetailScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun promptDetailLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                PromptDetailScreen(
                    state = PromptDetailUiState(
                        date = samplePrompt().date,
                        prompt = samplePrompt(),
                        remainingLabel = "9 hours left today",
                        isToday = true,
                    ),
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun promptDetailDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                PromptDetailScreen(
                    state = PromptDetailUiState(
                        date = samplePrompt().date,
                        prompt = samplePrompt(),
                        remainingLabel = "9 hours left today",
                        isToday = true,
                    ),
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun promptDetailSeriesLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                PromptDetailScreen(
                    state = PromptDetailUiState(
                        date = samplePrompt().date,
                        prompt = samplePrompt(),
                        remainingLabel = "9 hours left today",
                        isToday = true,
                        seriesProgress = app.shutterup.ui.home.sampleSeriesProgress(),
                    ),
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun promptDetailSeriesDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                PromptDetailScreen(
                    state = PromptDetailUiState(
                        date = samplePrompt().date,
                        prompt = samplePrompt(),
                        remainingLabel = "9 hours left today",
                        isToday = true,
                        seriesProgress = app.shutterup.ui.home.sampleSeriesProgress(),
                    ),
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
