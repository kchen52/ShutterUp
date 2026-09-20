package app.shutterup.ui.completion

import androidx.compose.material3.Surface
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
class CompletionScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun completionLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                Surface {
                    CompletionScreen(
                        state = sampleCompletionState(firstEver = true),
                        showBadgeSheet = false,
                        apertureProgress = 1f,
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun completionDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                Surface {
                    CompletionScreen(
                        state = sampleCompletionState(firstEver = false),
                        showBadgeSheet = false,
                        apertureProgress = 1f,
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun completionSeriesLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                CompletionScreen(
                    state = sampleCompletionState(firstEver = false, seriesTitle = "A Week of Hands"),
                    showBadgeSheet = false,
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun completionActionRowLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                CompletionActionRow(canShare = true)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun completionActionRowDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                CompletionActionRow(canShare = true)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
