package app.shutterup.ui.components

import androidx.compose.foundation.layout.Arrangement
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
class ApertureCheckScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun progressStripLight() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                ProgressStrip()
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun progressStripDark() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = true) {
                ProgressStrip()
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}

@Composable
private fun ProgressStrip() {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ApertureCheckMark(
                size = 24.dp,
                color = MaterialTheme.colorScheme.onSurface,
                progress = 0f,
            )
            ApertureCheckMark(
                size = 24.dp,
                color = MaterialTheme.colorScheme.onSurface,
                progress = 0.5f,
            )
            ApertureCheckMark(
                size = 24.dp,
                color = MaterialTheme.colorScheme.secondary,
                progress = 1f,
            )
            ApertureCheckMark(
                size = 18.dp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                progress = 1f,
            )
        }
    }
}
