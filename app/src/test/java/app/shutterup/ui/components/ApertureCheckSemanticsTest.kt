package app.shutterup.ui.components

import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import app.shutterup.R
import app.shutterup.ui.calendar.CalendarScreen
import app.shutterup.ui.calendar.sampleCalendarState
import app.shutterup.ui.completion.CompletionScreen
import app.shutterup.ui.completion.sampleCompletionState
import app.shutterup.ui.theme.ShutterUpTheme
import app.shutterup.widget.TodayWidgetPreviewContent
import app.shutterup.widget.TodayWidgetPreviewSize
import app.shutterup.widget.sampleCompletedTodayWidgetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ApertureCheckSemanticsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun decorativeMarkHasNoCompletedDescription() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                ApertureCheckMark(
                    size = 18.dp,
                    color = Color.Black,
                    progress = 1f,
                )
            }
        }
        composeRule.onNode(hasContentDescription("Completed")).assertDoesNotExist()
    }

    @Test
    fun announcedMarkExposesCompletedOnce() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                ApertureCheckMark(
                    size = 28.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    progress = 1f,
                    contentDescription = "Completed",
                )
            }
        }
        composeRule.onNode(hasContentDescription("Completed")).assertExists()
    }

    @Test
    fun completionAnnouncesCompletedOnce() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                CompletionScreen(
                    state = sampleCompletionState(firstEver = true),
                    showBadgeSheet = false,
                    apertureProgress = 1f,
                )
            }
        }
        composeRule.onAllNodes(hasContentDescription("Completed")).assertCountEquals(1)
    }

    @Test
    fun calendarCompletedCellKeepsParentDescription() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                CalendarScreen(state = sampleCalendarState())
            }
        }
        composeRule.onNode(hasContentDescription("18 September, completed")).assertExists()
        composeRule.onNode(hasContentDescription("Completed")).assertDoesNotExist()
    }

    @Test
    fun widgetCompletedChipAnnouncesCompleted() {
        composeRule.setContent {
            ShutterUpTheme(darkTheme = false) {
                TodayWidgetPreviewContent(
                    state = sampleCompletedTodayWidgetState(),
                    size = TodayWidgetPreviewSize.Medium,
                )
            }
        }
        composeRule.onNode(hasContentDescription("Completed")).assertExists()
    }

    @Test
    fun checkPathUsesSharedViewportPoints() {
        val bounds = apertureCheckPath().getBounds()
        assertEquals(ApertureCheckStartX, bounds.left, 0.05f)
        assertEquals(ApertureCheckEndY, bounds.top, 0.05f)
        assertEquals(ApertureCheckEndX, bounds.right, 0.05f)
        assertEquals(ApertureCheckMidY, bounds.bottom, 0.05f)
    }

    @Test
    fun sharedVectorDrawableMatchesCanvasPath() {
        val drawable = RuntimeEnvironment.getApplication().getDrawable(R.drawable.ic_aperture_check)
        assertNotNull(drawable)
        assertEquals(24, drawable!!.intrinsicWidth)
        assertEquals(24, drawable.intrinsicHeight)
        val parsed = parseApertureCheckVector()
        assertEquals(apertureCheckVectorPathData(), parsed.pathData)
        assertEquals(ApertureCheckStroke, parsed.strokeWidth?.toFloatOrNull() ?: -1f, 0.001f)
        assertTrue(isVectorRound(parsed.strokeLineCap))
        assertTrue(isVectorRound(parsed.strokeLineJoin))
    }

    @Test
    fun reducedMotionPlayOnceSettlesOnFinalFrame() {
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        val previous = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        try {
            composeRule.setContent {
                ShutterUpTheme(darkTheme = false) {
                    Surface {
                        ApertureCheckMark(
                            size = 24.dp,
                            color = Color.Black,
                            playOnce = true,
                            contentDescription = "Completed",
                        )
                    }
                }
            }
            composeRule.onNode(hasContentDescription("Completed")).assertExists()
        } finally {
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, previous)
        }
    }
}

private data class ParsedApertureCheckVector(
    val pathData: String?,
    val strokeWidth: String?,
    val strokeLineCap: String?,
    val strokeLineJoin: String?,
)

private const val AndroidNs = "http://schemas.android.com/apk/res/android"

private fun parseApertureCheckVector(): ParsedApertureCheckVector {
    val parser = RuntimeEnvironment.getApplication().resources.getXml(R.drawable.ic_aperture_check)
    var pathData: String? = null
    var strokeWidth: String? = null
    var strokeLineCap: String? = null
    var strokeLineJoin: String? = null
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG && parser.name == "path") {
            pathData = xmlAttr(parser, "pathData")
            strokeWidth = xmlAttr(parser, "strokeWidth")
            strokeLineCap = xmlAttr(parser, "strokeLineCap")
            strokeLineJoin = xmlAttr(parser, "strokeLineJoin")
        }
        event = parser.next()
    }
    return ParsedApertureCheckVector(pathData, strokeWidth, strokeLineCap, strokeLineJoin)
}

private fun xmlAttr(parser: XmlPullParser, name: String): String? =
    parser.getAttributeValue(AndroidNs, name) ?: parser.getAttributeValue(null, name)

private fun isVectorRound(value: String?): Boolean =
    value.equals("round", ignoreCase = true) || value == "1"
