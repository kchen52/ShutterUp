package app.shutterup

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Uses the Hilt-generated test application so [TestInstallIn] modules replace
 * Clock, Room, prefs, and the prompt generator (SPEC §15.2).
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(cl, ShutterUpTestApp_Application::class.java.name, context)
}
