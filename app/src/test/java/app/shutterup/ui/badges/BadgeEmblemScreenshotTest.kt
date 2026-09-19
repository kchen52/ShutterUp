package app.shutterup.ui.badges

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
@Config(sdk = [35], qualifiers = "w400dp-h1600dp")
class BadgeEmblemScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unlockedGridLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                AllBadgesGrid(unlocked = true)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun unlockedGridDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                AllBadgesGrid(unlocked = true)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun lockedGridLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                AllBadgesGrid(unlocked = false)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun lockedGridDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                AllBadgesGrid(unlocked = false)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w400dp-h2400dp")
    fun unlockedGridFontScale2() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density = density.density, fontScale = 2f),
                ) {
                    AllBadgesGrid(unlocked = true)
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
