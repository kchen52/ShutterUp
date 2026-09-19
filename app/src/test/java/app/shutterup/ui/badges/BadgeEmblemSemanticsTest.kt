package app.shutterup.ui.badges

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import app.shutterup.ui.theme.ShutterUpTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BadgeEmblemSemanticsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lockedEmblemUsesNameAndStateNotHint() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                BadgeEmblem(
                    badgeId = BadgeIds.STREAK_30,
                    unlocked = false,
                    size = 96.dp,
                )
            }
        }
        composeRule.onNode(hasContentDescription("Month of Light, locked")).assertExists()
        composeRule.onNode(hasContentDescription("Complete 30 days in a row", substring = true))
            .assertDoesNotExist()
    }

    @Test
    fun unlockedEmblemUsesNameAndState() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                BadgeEmblem(
                    badgeId = BadgeIds.FIRST_LIGHT,
                    unlocked = true,
                    size = 96.dp,
                )
            }
        }
        composeRule.onNode(hasContentDescription("First Light, unlocked")).assertExists()
    }
}
