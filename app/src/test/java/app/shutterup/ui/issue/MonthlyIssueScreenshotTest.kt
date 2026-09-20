package app.shutterup.ui.issue

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import app.shutterup.ui.feed.FeedScreen
import app.shutterup.ui.feed.sampleFeedStateWithIssue
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
class MonthlyIssueScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun issuePageLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                IssueScreen(page = sampleIssuePage(), onBack = {}, onOpenDay = {})
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun issuePageDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                IssueScreen(page = sampleIssuePage(), onBack = {}, onOpenDay = {})
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w840dp-h1400dp")
    fun issuePageExpanded() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                IssueScreen(page = sampleIssuePage(), onBack = {}, onOpenDay = {})
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun issuePageFontScale() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                ShutterUpTheme(darkTheme = false) {
                    IssueScreen(page = sampleIssuePage(), onBack = {}, onOpenDay = {})
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun issuePageSparseLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                IssueScreen(page = sampleIssuePage(sparse = true), onBack = {}, onOpenDay = {})
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun issuePageSparseDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                IssueScreen(page = sampleIssuePage(sparse = true), onBack = {}, onOpenDay = {})
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun feedCardLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                FeedScreen(state = sampleFeedStateWithIssue(), onOpenDay = {})
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun feedCardDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                FeedScreen(state = sampleFeedStateWithIssue(), onOpenDay = {})
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w840dp-h1400dp")
    fun feedCardExpanded() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                FeedScreen(state = sampleFeedStateWithIssue(), onOpenDay = {})
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun feedCardFontScale() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                ShutterUpTheme(darkTheme = false) {
                    FeedScreen(state = sampleFeedStateWithIssue(), onOpenDay = {})
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun feedCardSparse() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                FeedScreen(state = sampleFeedStateWithIssue(sparse = true), onOpenDay = {})
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
