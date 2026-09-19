package app.shutterup.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.shutterup.MainActivity
import app.shutterup.R
import app.shutterup.domain.model.DayPrompt
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Daily-prompt notifications (SPEC §8.1).
 *
 * Deep-link contract consumed by [MainActivity] (parsing lands in a later milestone):
 * - Content tap: `shutterup://day/<ISO-date>`
 * - Shoot action: `shutterup://day/<ISO-date>?autoLaunchCamera=true`
 * - Reroll action: `shutterup://day/<ISO-date>?reroll=true`
 *
 * Actions target [MainActivity] only (no broadcast trampolines).
 *
 * Large-icon monogram uses a fixed warm seed `#6B5B4E` with a white initial.
 * Milestone 6 replaces this with theme-tinted [colorScheme.primaryContainer] once
 * the design-system theme is wired outside Compose.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            setBypassDnd(false)
        }
        notificationManager().createNotificationChannel(channel)
    }

    /**
     * Posts today's prompt. Returns false when [Manifest.permission.POST_NOTIFICATIONS]
     * is denied (nothing is posted). Returns true when the notification is shown.
     */
    fun postPrompt(date: LocalDate, prompt: DayPrompt, rerollAvailable: Boolean): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        ensureChannel()
        val iso = date.toString()
        val contentIntent = activityIntent(Uri.parse("$SCHEME://$HOST/$iso"), REQUEST_CONTENT)
        val shootIntent = activityIntent(
            Uri.parse("$SCHEME://$HOST/$iso?autoLaunchCamera=true"),
            REQUEST_SHOOT,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_shoot)
            .setContentTitle(prompt.title)
            .setContentText(prompt.oneLiner)
            .setStyle(NotificationCompat.BigTextStyle().bigText(firstTwoSentences(prompt.details)))
            .setLargeIcon(monogram(prompt.title))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_stat_shoot, ACTION_SHOOT, shootIntent)
        if (rerollAvailable) {
            val rerollIntent = activityIntent(
                Uri.parse("$SCHEME://$HOST/$iso?reroll=true"),
                REQUEST_REROLL,
            )
            builder.addAction(R.drawable.ic_stat_shoot, ACTION_REROLL, rerollIntent)
        }
        notificationManager().notify(notificationId(date), builder.build())
        return true
    }

    fun cancel(date: LocalDate) {
        notificationManager().cancel(notificationId(date))
    }

    private fun activityIntent(uri: Uri, requestCode: Int): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, uri, context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationManager(): NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    private companion object {
        const val CHANNEL_ID = "daily_prompt"
        const val CHANNEL_NAME = "Daily prompt"
        const val SCHEME = "shutterup"
        const val HOST = "day"
        const val ACTION_SHOOT = "Shoot"
        const val ACTION_REROLL = "Reroll"
        const val REQUEST_CONTENT = 1001
        const val REQUEST_SHOOT = 1002
        const val REQUEST_REROLL = 1003
        const val MONOGRAM_SEED = 0xFF6B5B4E.toInt()

        fun notificationId(date: LocalDate): Int = date.toEpochDay().toInt()

        fun firstTwoSentences(details: String): String {
            val sentences = details
                .trim()
                .split(Regex("(?<=[.!?])\\s+"))
                .filter { it.isNotBlank() }
            if (sentences.isEmpty()) return details
            return sentences.take(2).joinToString(" ")
        }

        fun monogram(title: String): Bitmap {
            val size = 128
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = MONOGRAM_SEED
            }
            val radius = size / 2f
            canvas.drawCircle(radius, radius, radius, paint)
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = size * 0.5f
            paint.isFakeBoldText = true
            val letter = title.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            val textY = radius - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(letter, radius, textY, paint)
            return bitmap
        }
    }
}
