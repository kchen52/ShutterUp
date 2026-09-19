package app.shutterup.ui.components

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import app.shutterup.ui.theme.ShutterUpTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StreakStatusSemanticsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun exposesOneCombinedDescription() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                StreakStatus(
                    streakDays = 14,
                    freezes = 2,
                    monthCompleted = 18,
                    monthEligible = 19,
                )
            }
        }
        composeRule.onNode(
            hasContentDescription("14 days, 2 freezes, 18 of 19 days this month"),
        ).assertExists()
    }
}
