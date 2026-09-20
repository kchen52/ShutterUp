package app.shutterup.ui.settings

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
@Config(sdk = [35], qualifiers = "w400dp-h1600dp")
class SettingsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                SettingsScreen(state = sampleSettingsState())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun settingsDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                SettingsScreen(state = sampleSettingsState())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp")
    fun settingsExpandedLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                SettingsScreen(state = sampleSettingsState())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp")
    fun settingsExpandedDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                SettingsScreen(state = sampleSettingsState())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w400dp-h2000dp", fontScale = 2f)
    fun settingsFontScale2() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                SettingsScreen(state = sampleSettingsState())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun settingsCitySetLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                SettingsScreen(
                    state = sampleSettingsState(
                        coarseCityId = "sydney",
                        coarseCityName = "Sydney",
                    ),
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun settingsSeriesOnLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                SettingsScreen(state = sampleSettingsState(seriesEnabled = true))
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
