package app.shutterup

import android.app.Application
import dagger.hilt.android.testing.CustomTestApplication

/**
 * Plain [Application] so [app.shutterup.ShutterUpApplication] startup (WorkManager
 * + widget refresh) does not run under instrumentation. Tests initialize
 * WorkManager themselves via [app.shutterup.testutil.initTestWorkManager].
 */
@CustomTestApplication(Application::class)
interface ShutterUpTestApp
