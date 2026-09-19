package app.shutterup.ui.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import app.shutterup.domain.ai.Availability
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
class OnboardingScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun welcomeLight() = capture(page = 0, dark = false)

    @Test
    fun welcomeDark() = capture(page = 0, dark = true)

    @Test
    fun notificationsLight() = capture(page = 1, dark = false)

    @Test
    fun notificationsDark() = capture(page = 1, dark = true)

    @Test
    fun preferencesLight() = capture(page = 2, dark = false)

    @Test
    fun preferencesDark() = capture(page = 2, dark = true)

    @Test
    fun aiStatusLight() = capture(page = 3, dark = false)

    @Test
    fun aiStatusDark() = capture(page = 3, dark = true)

    private fun capture(page: Int, dark: Boolean) {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = dark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OnboardingPageContent(
                        page = page,
                        state = OnboardingUiState(
                            page = page,
                            availability = if (page == 3) Availability.DOWNLOADING else null,
                        ),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
