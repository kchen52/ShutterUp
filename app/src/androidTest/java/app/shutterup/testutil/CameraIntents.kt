package app.shutterup.testutil

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.IntentCompat
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

/**
 * Stubs [MediaStore.ACTION_IMAGE_CAPTURE] and writes a tiny JPEG to
 * [MediaStore.EXTRA_OUTPUT] so [app.shutterup.ui.detail.PromptDetailViewModel]
 * sees a non-empty pending file.
 */
object CameraIntents {
    fun stubSuccessfulCapture() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        intending(writesOutputThenMatches(context)).respondWith(
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()),
        )
    }

    fun assertCaptureLaunched() {
        intended(hasAction(MediaStore.ACTION_IMAGE_CAPTURE))
        intended(hasExtraOutput())
    }

    private fun writesOutputThenMatches(context: Context): Matcher<Intent> =
        object : TypeSafeMatcher<Intent>() {
            override fun describeTo(description: Description) {
                description.appendText("ACTION_IMAGE_CAPTURE with writable EXTRA_OUTPUT")
            }

            override fun matchesSafely(intent: Intent): Boolean {
                if (intent.action != MediaStore.ACTION_IMAGE_CAPTURE) return false
                val uri = IntentCompat.getParcelableExtra(
                    intent,
                    MediaStore.EXTRA_OUTPUT,
                    Uri::class.java,
                ) ?: return false
                writeMinimalJpeg(context, uri)
                return true
            }
        }

    private fun hasExtraOutput(): Matcher<Intent> =
        object : TypeSafeMatcher<Intent>() {
            override fun describeTo(description: Description) {
                description.appendText("intent with EXTRA_OUTPUT")
            }

            override fun matchesSafely(intent: Intent): Boolean =
                IntentCompat.getParcelableExtra(
                    intent,
                    MediaStore.EXTRA_OUTPUT,
                    Uri::class.java,
                ) != null
        }

    fun writeMinimalJpeg(context: Context, uri: Uri) {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(40, 90, 140))
        val written = context.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        } == true
        check(written) { "Could not write JPEG to $uri" }
    }
}
