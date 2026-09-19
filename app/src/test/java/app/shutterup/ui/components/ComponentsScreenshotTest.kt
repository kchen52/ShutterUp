package app.shutterup.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import app.shutterup.ui.theme.ShutterUpTheme
import app.shutterup.ui.theme.themeTint
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ComponentsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chipsCardsButtons_light() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                ChipsCardsButtons(darkTheme = false)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun chipsCardsButtons_dark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                ChipsCardsButtons(darkTheme = true)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun statusRow_light() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                StatusRowVariants()
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun statusRow_dark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                StatusRowVariants()
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun permissionCard_light() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                PermissionCardPreview()
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun permissionCard_dark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                PermissionCardPreview()
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}

@Composable
private fun ChipsCardsButtons(darkTheme: Boolean) {
    val tint = themeTint("reflections", MaterialTheme.colorScheme, darkTheme)
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ThemeChip(label = "Reflections", selected = true, tint = tint, onClick = {})
                ThemeChip(label = "Quiet hours", selected = false, tint = tint, onClick = {})
                ThemeChip(label = "Still", selected = false, tint = tint, onClick = null)
            }
            ConstraintCard(text = "Don't rotate the photo afterwards.", tint = tint)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShootButton(onClick = {})
                LibraryTag()
            }
            Kicker(text = "Tuesday · Reflections")
        }
    }
}

@Composable
private fun StatusRowVariants() {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StreakStatus(streakDays = 14, freezes = 2, monthCompleted = 18, monthEligible = 19)
            StreakStatus(streakDays = 3, freezes = 0, monthCompleted = 1, monthEligible = 19)
        }
    }
}

@Composable
private fun PermissionCardPreview() {
    Surface(color = MaterialTheme.colorScheme.surface) {
        NotificationPermissionCard(
            onOpenSettings = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
