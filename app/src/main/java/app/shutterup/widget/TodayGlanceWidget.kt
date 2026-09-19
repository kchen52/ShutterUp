package app.shutterup.widget

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProvider
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.shutterup.MainActivity
import app.shutterup.navigation.DeepLinks
import java.io.File

/**
 * Glance home-screen widget: 2×2 (small) and 4×2 (medium) (DESIGN.md §8).
 */
class TodayGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val state = prefs.toWidgetState()
                val size = LocalSize.current
                val medium = size.width >= MEDIUM.width
                TodayGlanceContent(state = state, medium = medium)
            }
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 110.dp)
    }
}

@Composable
private fun TodayGlanceContent(state: TodayWidgetState, medium: Boolean) {
    val context = LocalContext.current
    val detail = DeepLinks.detail(state.dateIso)
    val shoot = DeepLinks.detail(state.dateIso, autoLaunchCamera = true)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(widgetTint(state.theme))
            .padding(if (medium) 12.dp else 10.dp)
            .clickable(actionStartActivity(viewIntent(context, detail))),
    ) {
        if (state.completed) {
            CompletedGlance(state, medium)
        } else {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Text(
                    text = if (medium) state.kicker else state.shortKicker,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp,
                    ),
                    maxLines = if (medium) 1 else 2,
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = state.title,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = if (medium) 20.sp else 18.sp,
                        fontFamily = FontFamily.Serif,
                    ),
                    maxLines = if (medium) 2 else 3,
                )
                if (medium && !state.paused) {
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = state.oneLiner,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 14.sp,
                        ),
                        maxLines = 2,
                    )
                    Spacer(GlanceModifier.height(8.dp))
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .cornerRadius(50.dp)
                                .background(GlanceTheme.colors.primary)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clickable(actionStartActivity(viewIntent(context, shoot))),
                        ) {
                            Text(
                                text = "Shoot",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onPrimary,
                                    fontSize = 13.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedGlance(state: TodayWidgetState, medium: Boolean) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (medium) state.kicker else state.shortKicker,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp,
                ),
                maxLines = if (medium) 1 else 2,
            )
            Box(
                modifier = GlanceModifier
                    .cornerRadius(50.dp)
                    .background(GlanceTheme.colors.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "✓",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
        if (medium) {
            Spacer(GlanceModifier.height(8.dp))
            val path = state.thumbPath
            if (!path.isNullOrEmpty() && File(path).isFile) {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = state.title,
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .cornerRadius(16.dp),
                    )
                }
            }
        }
    }
}

private fun Preferences.toWidgetState(): TodayWidgetState = TodayWidgetState(
    dateIso = this[TodayWidgetKeys.dateIso] ?: "1970-01-01",
    weekday = this[TodayWidgetKeys.weekday] ?: "Today",
    theme = this[TodayWidgetKeys.theme] ?: "Today",
    title = this[TodayWidgetKeys.title] ?: "Today's prompt",
    oneLiner = this[TodayWidgetKeys.oneLiner] ?: "",
    constraint = this[TodayWidgetKeys.constraint],
    streakDays = this[TodayWidgetKeys.streakDays] ?: 0,
    paused = this[TodayWidgetKeys.paused] ?: false,
    completed = this[TodayWidgetKeys.completed] ?: false,
    thumbPath = this[TodayWidgetKeys.thumbPath],
)

private fun viewIntent(context: Context, uri: String): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse(uri), context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

/** 12 % / 18 % theme overlay (DESIGN.md §2.1 / §8). */
private fun widgetTint(theme: String): androidx.glance.unit.ColorProvider {
    val hue = (theme.lowercase().hashCode() and Int.MAX_VALUE) % 360
    val raw = android.graphics.Color.HSVToColor(floatArrayOf(hue.toFloat(), 0.55f, 0.85f))
    return ColorProvider(
        day = Color(blend(0xFFF7F2EC.toInt(), raw, 0.12f)),
        night = Color(blend(0xFF1C1B1A.toInt(), raw, 0.18f)),
    )
}

private fun blend(base: Int, overlay: Int, amount: Float): Int {
    val inv = 1f - amount
    fun ch(c: Int, shift: Int) =
        ((c shr shift) and 0xFF) * inv + ((overlay shr shift) and 0xFF) * amount
    val r = ch(base, 16).toInt()
    val g = ch(base, 8).toInt()
    val b = ch(base, 0).toInt()
    return (255 shl 24) or (r shl 16) or (g shl 8) or b
}
