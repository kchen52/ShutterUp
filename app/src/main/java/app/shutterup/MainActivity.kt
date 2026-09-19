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
import androidx.navigation.compose.rememberNavController
import app.shutterup.ui.navigation.ShutterUpNavGraph
import app.shutterup.ui.navigation.navigateDayUri
import app.shutterup.ui.theme.ShutterUpTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var clock: Clock
    @Inject lateinit var zone: ZoneId

    private var deepLink by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLink = intent?.data
        val today = LocalDate.now(clock.withZone(zone)).toString()
        setContent {
            ShutterUpTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    LaunchedEffect(deepLink) {
                        navController.navigateDayUri(deepLink)
                    }
                    ShutterUpNavGraph(
                        navController = navController,
                        todayIso = today,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink = intent.data
    }
}
