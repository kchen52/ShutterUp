package app.shutterup.ui.calendar

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
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
@Config(sdk = [35], qualifiers = "w400dp-h1400dp")
class YearViewScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun yearSparseLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                CalendarScreen(
                    state = sampleYearCalendarState(full = false),
                    startInYearView = true,
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun yearSparseDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                CalendarScreen(
                    state = sampleYearCalendarState(full = false),
                    startInYearView = true,
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun yearFullLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                CalendarScreen(
                    state = sampleYearCalendarState(full = true),
                    startInYearView = true,
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun yearFullDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                CalendarScreen(
                    state = sampleYearCalendarState(full = true),
                    startInYearView = true,
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w1000dp-h900dp")
    fun yearExpandedLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                CalendarScreen(
                    state = sampleYearCalendarState(full = true),
                    startInYearView = true,
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w400dp-h1800dp")
    fun yearFontScale2() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density = density.density, fontScale = 2f),
                ) {
                    CalendarScreen(
                        state = sampleYearCalendarState(full = false),
                        startInYearView = true,
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
