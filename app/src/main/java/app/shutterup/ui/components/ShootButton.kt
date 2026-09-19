package app.shutterup.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.shutterup.ui.icons.ApertureIcon
import app.shutterup.ui.theme.ShutterUpTheme

@Composable
fun ShootButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = ApertureIcon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = "Shoot")
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun ShootButtonPreviewLight() {
    ShutterUpTheme(darkTheme = false) {
        Surface { ShootButton(onClick = {}, modifier = Modifier.padding(16.dp)) }
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ShootButtonPreviewDark() {
    ShutterUpTheme(darkTheme = true) {
        Surface { ShootButton(onClick = {}, modifier = Modifier.padding(16.dp)) }
    }
}

@Preview(name = "FontScale 2x", showBackground = true, fontScale = 2f)
@Composable
private fun ShootButtonPreviewFontScale() {
    ShutterUpTheme(darkTheme = false) {
        Surface { ShootButton(onClick = {}, modifier = Modifier.padding(16.dp)) }
    }
}
