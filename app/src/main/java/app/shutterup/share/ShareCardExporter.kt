package app.shutterup.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.MediaKind
import app.shutterup.domain.share.ShareCardContent
import app.shutterup.domain.share.isDayShareable
import app.shutterup.ui.share.ShareCardMetrics
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the day's photo, renders a share card off the UI thread as far as
 * ComposeView allows, encodes a PNG, and returns a chooser [Intent].
 */
@Singleton
class ShareCardExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val renderer: ShareCardRenderer,
    private val cache: ShareCache,
) {
    suspend fun export(
        prompt: DayPrompt,
        entries: List<Entry>,
        darkTheme: Boolean,
    ): Result<Intent> = withContext(Dispatchers.Default) {
        runCatching {
            check(isDayShareable(prompt.status, entries)) { "Day is not shareable" }
            val entry = entries.last { it.mediaKind == MediaKind.PHOTO }
            val photo = decodePhoto(entry) ?: error("Photo could not be decoded")
            try {
                val content = ShareCardContent.from(prompt.date, prompt.theme, prompt.title)
                val card = renderer.render(content, photo.asImageBitmap(), darkTheme)
                try {
                    val uri = cache.writePng(card)
                    ShareIntents.chooser(uri)
                } finally {
                    if (!card.isRecycled) card.recycle()
                }
            } finally {
                if (!photo.isRecycled) photo.recycle()
            }
        }
    }

    private fun decodePhoto(entry: Entry): Bitmap? {
        val longest = ShareCardMetrics.PhotoLongestPx
        decodeUri(entry.mediaUri, longest)?.let { return it }
        val thumb = File(entry.thumbPath)
        if (thumb.isFile) return decodeFile(thumb, longest)
        return null
    }

    private fun decodeUri(uriString: String, longest: Int): Bitmap? {
        val uri = runCatching { uriString.toUri() }.getOrNull() ?: return null
        if (uri.scheme == "file") {
            uri.path?.let { decodeFile(File(it), longest) }?.let { return it }
        }
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, longest)
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeFile(file: File, longest: Int): Bitmap? {
        if (!file.isFile) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, longest)
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } catch (_: Exception) {
            null
        }
    }

    private fun sampleSize(width: Int, height: Int, longest: Int): Int {
        val max = maxOf(width, height).coerceAtLeast(1)
        return (max / longest).coerceAtLeast(1)
    }
}
