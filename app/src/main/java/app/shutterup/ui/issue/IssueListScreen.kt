package app.shutterup.ui.issue

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shutterup.ui.components.Kicker
import app.shutterup.ui.theme.ShutterUpTheme
import app.shutterup.ui.theme.themeTint

@Composable
fun IssueListRoute(
    onBack: () -> Unit,
    onOpenIssue: (String) -> Unit,
    viewModel: IssueListViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    IssueListScreen(
        items = items,
        onBack = onBack,
        onOpenIssue = onOpenIssue,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueListScreen(
    items: List<IssueListItemUi>,
    onBack: () -> Unit,
    onOpenIssue: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("The Monthly", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.yearMonth }) { item ->
                IssueListRow(item = item, onClick = { onOpenIssue(item.yearMonth) })
            }
        }
    }
}

@Composable
private fun IssueListRow(
    item: IssueListItemUi,
    onClick: () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val tint = themeTint(item.theme, MaterialTheme.colorScheme, dark)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = item.kicker },
        shape = RoundedCornerShape(28.dp),
        color = tint,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Kicker(text = item.kicker)
            Text(
                text = item.headline,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(name = "compact-light", showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun IssueListPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        IssueListScreen(items = sampleIssueList(), onBack = {}, onOpenIssue = {})
    }
}

@Preview(
    name = "compact-dark",
    showBackground = true,
    widthDp = 400,
    heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun IssueListPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        IssueListScreen(items = sampleIssueList(), onBack = {}, onOpenIssue = {})
    }
}
