package app.shutterup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.ui.navigation.ShutterUpNavGraph
import app.shutterup.ui.navigation.navigateDayUri
import app.shutterup.ui.onboarding.OnboardingRoute
import app.shutterup.ui.theme.ShutterUpTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var clock: Clock
    @Inject lateinit var zone: ZoneId
    @Inject lateinit var preferences: PreferencesRepository

    private var pendingDeepLink by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDeepLink = intent?.data
        val today = LocalDate.now(clock.withZone(zone)).toString()
        val onboardedInitially = runBlocking { preferences.observeOnboardingComplete().first() }
        setContent {
            ShutterUpTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val onboarded by preferences.observeOnboardingComplete()
                        .collectAsStateWithLifecycle(initialValue = onboardedInitially)
                    if (!onboarded) {
                        OnboardingRoute(onFinished = {})
                    } else {
                        val navController = rememberNavController()
                        LaunchedEffect(onboarded, pendingDeepLink) {
                            val uri = pendingDeepLink ?: return@LaunchedEffect
                            navController.navigateDayUri(uri)
                            consumePendingDeepLink()
                        }
                        ShutterUpNavGraph(
                            navController = navController,
                            todayIso = today,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink = intent.data
    }

    private fun consumePendingDeepLink() {
        pendingDeepLink = null
        val cleared = Intent(intent)
        cleared.data = null
        setIntent(cleared)
    }
}
