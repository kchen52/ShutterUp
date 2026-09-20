package app.shutterup.ui.issue

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shutterup.ui.theme.ShutterUpTheme

@Composable
fun IssueRoute(
    onBack: () -> Unit,
    onOpenDay: (String) -> Unit,
    viewModel: IssueViewModel = hiltViewModel(),
) {
    val page by viewModel.page.collectAsStateWithLifecycle()
    IssueScreen(
        page = page,
        onBack = onBack,
        onOpenDay = onOpenDay,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueScreen(
    page: IssuePageUi?,
    onBack: () -> Unit,
    onOpenDay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val expanded = maxWidth >= 600.dp
            val horizontal = if (expanded) 24.dp else 16.dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontal, vertical = 8.dp),
                contentAlignment = if (expanded) Alignment.TopCenter else Alignment.TopStart,
            ) {
                if (page != null) {
                    MonthlyIssuePage(
                        page = page,
                        onOpenDay = onOpenDay,
                        fillSheet = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Preview(name = "compact-light", showBackground = true, widthDp = 400, heightDp = 1100)
@Composable
private fun IssueScreenPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        IssueScreen(page = sampleIssuePage(), onBack = {}, onOpenDay = {})
    }
}

@Preview(
    name = "compact-dark",
    showBackground = true,
    widthDp = 400,
    heightDp = 1100,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun IssueScreenPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        IssueScreen(page = sampleIssuePage(), onBack = {}, onOpenDay = {})
    }
}

@Preview(name = "expanded-light", showBackground = true, widthDp = 840, heightDp = 1100)
@Composable
private fun IssueScreenPreviewExpanded() {
    ShutterUpTheme(darkTheme = false) {
        IssueScreen(page = sampleIssuePage(), onBack = {}, onOpenDay = {})
    }
}

@Preview(name = "font-scale-2", showBackground = true, widthDp = 400, heightDp = 1400, fontScale = 2f)
@Composable
private fun IssueScreenPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        IssueScreen(page = sampleIssuePage(), onBack = {}, onOpenDay = {})
    }
}

@Preview(name = "sparse-light", showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun IssueScreenPreviewSparse() {
    ShutterUpTheme(darkTheme = false) {
        IssueScreen(page = sampleIssuePage(sparse = true), onBack = {}, onOpenDay = {})
    }
}
