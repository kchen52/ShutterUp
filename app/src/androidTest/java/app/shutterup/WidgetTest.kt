package app.shutterup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.shutterup.di.TestTimeModule
import app.shutterup.testutil.SeedData
import app.shutterup.ui.theme.ShutterUpTheme
import app.shutterup.widget.TodayWidgetPreviewContent
import app.shutterup.widget.TodayWidgetPreviewSize
import app.shutterup.widget.TodayWidgetState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun glancePreviewRendersTodayTitle() {
        val state = TodayWidgetState.from(
            date = TestTimeModule.TODAY,
            prompt = SeedData.todayPrompt(),
            streakDays = 3,
            paused = false,
            thumbPath = null,
        )
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                TodayWidgetPreviewContent(
                    state = state,
                    size = TodayWidgetPreviewSize.Medium,
                )
            }
        }
        composeRule.onNodeWithText(SeedData.TODAY_TITLE).assertIsDisplayed()
    }
}
