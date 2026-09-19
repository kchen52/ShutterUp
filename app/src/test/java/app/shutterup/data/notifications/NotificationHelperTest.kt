package app.shutterup.data.notifications

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.PromptSourceRef
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationHelperTest {

    @get:Rule
    val jdk17: TestRule = Jdk17Rule()

    private lateinit var application: Application
    private lateinit var helper: NotificationHelper
    private lateinit var manager: NotificationManager

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        helper = NotificationHelper(application)
        manager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun ensureChannel_createsDailyPromptAtDefaultImportance() {
        helper.ensureChannel()
        val channel = manager.getNotificationChannel("daily_prompt")
        assertNotNull(channel)
        assertEquals("daily_prompt", channel.id)
        assertEquals("Daily prompt", channel.name.toString())
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
        assertFalse(channel.canBypassDnd())
    }

    @Test
    fun postPrompt_includesTitleOneLinerAndBothActionsWhenRerollAvailable() {
        val date = LocalDate.of(2024, 6, 15)
        val posted = helper.postPrompt(date, samplePrompt(date), rerollAvailable = true)
        assertTrue(posted)

        val notification = shadowManager().getNotification(date.toEpochDay().toInt())
        assertNotNull(notification)
        assertEquals("Steam Maps", notification.extras.getString(android.app.Notification.EXTRA_TITLE))
        assertEquals(
            "Turn kitchen steam into contour lines of light.",
            notification.extras.getString(android.app.Notification.EXTRA_TEXT),
        )
        val actions = notification.actions
        assertEquals(2, actions.size)
        assertEquals("Shoot", actions[0].title.toString())
        assertEquals("Reroll", actions[1].title.toString())
        val shootUri = Shadows.shadowOf(actions[0].actionIntent).savedIntent.data.toString()
        val rerollUri = Shadows.shadowOf(actions[1].actionIntent).savedIntent.data.toString()
        assertEquals("shutterup://day/2024-06-15?autoLaunchCamera=true", shootUri)
        assertEquals("shutterup://day/2024-06-15?reroll=true", rerollUri)
    }

    @Test
    fun postPrompt_omitsRerollActionWhenUsed() {
        val date = LocalDate.of(2024, 6, 16)
        helper.postPrompt(date, samplePrompt(date, rerollUsed = true), rerollAvailable = false)
        val notification = shadowManager().getNotification(date.toEpochDay().toInt())
        assertNotNull(notification)
        assertEquals(1, notification.actions.size)
        assertEquals("Shoot", notification.actions[0].title.toString())
    }

    @Test
    fun cancel_removesPostedNotification() {
        val date = LocalDate.of(2024, 6, 17)
        helper.postPrompt(date, samplePrompt(date), rerollAvailable = true)
        assertNotNull(shadowManager().getNotification(date.toEpochDay().toInt()))
        helper.cancel(date)
        assertNull(shadowManager().getNotification(date.toEpochDay().toInt()))
    }

    @Test
    fun postPrompt_returnsFalseWhenNotificationsDenied() {
        Shadows.shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val date = LocalDate.of(2024, 6, 18)
        val posted = helper.postPrompt(date, samplePrompt(date), rerollAvailable = true)
        assertFalse(posted)
        assertNull(shadowManager().getNotification(date.toEpochDay().toInt()))
    }

    private fun shadowManager(): ShadowNotificationManager = Shadows.shadowOf(manager)

    private fun samplePrompt(date: LocalDate, rerollUsed: Boolean = false): DayPrompt = DayPrompt(
        date = date,
        title = "Steam Maps",
        oneLiner = "Turn kitchen steam into contour lines of light.",
        details = "Wait for a kettle or hot tap. Side-light the plume and expose for the brightest edge.",
        constraint = "No zoom",
        theme = "Steam",
        tips = listOf("Use a dark backdrop."),
        source = PromptSourceRef.LIBRARY,
        libraryId = "lib-1",
        modelName = null,
        generatedAt = Instant.parse("2024-06-15T08:00:00Z"),
        status = DayStatus.PENDING,
        frozen = false,
        rerollUsed = rerollUsed,
    )
}

private class Jdk17Rule : TestRule {
    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                val spec = System.getProperty("java.specification.version")
                val major = spec?.substringBefore('.')?.toIntOrNull() ?: 0
                Assume.assumeTrue("Robolectric SDK 35 requires JDK 17", major == 17)
                base.evaluate()
            }
        }
}
