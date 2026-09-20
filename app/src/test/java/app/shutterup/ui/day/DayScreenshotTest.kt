package app.shutterup.ui.day

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
class DayScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dayLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                DayScreen(state = sampleDayState())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun dayDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                DayScreen(state = sampleDayState())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w400dp-h1600dp")
    fun diptychCompactLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                DayScreen(state = sampleDiptychState())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w400dp-h1600dp")
    fun diptychCompactDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                DayScreen(state = sampleDiptychState())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w840dp-h900dp")
    fun diptychExpanded() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                DayScreen(state = sampleDiptychState(), expanded = true)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w400dp-h1400dp", fontScale = 2f)
    fun diptychFontScale2() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                DayScreen(state = sampleDiptychState())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w400dp-h1600dp")
    fun diptychPortraitLandscape() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                DayScreen(state = sampleDiptychState(mixedAspect = true))
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w400dp-h1600dp")
    fun diptychThreeTakeChain() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                DayScreen(state = sampleDiptychState(threeTakes = true))
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
