package app.shutterup.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** EXIF / container metadata for a capture file. Bytes are never re-encoded. */
data class CaptureMetadata(
    val capturedAt: Instant,
    val width: Int,
    val height: Int,
    val durationMs: Long?,
)

/** Reads capture time, size, and optional duration from a private file. */
@Singleton
class CaptureMetadataReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val zone: ZoneId,
) {
    private val exifDate: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

    fun read(file: File, fallback: Instant): CaptureMetadata {
        val isVideo = file.extension.equals("mp4", ignoreCase = true) ||
            file.extension.equals("3gp", ignoreCase = true)
        return if (isVideo) readVideo(file, fallback) else readImage(file, fallback)
    }

    private fun readImage(file: File, fallback: Instant): CaptureMetadata {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val capturedAt = readExifInstant(file) ?: fallback
        return CaptureMetadata(
            capturedAt = capturedAt,
            width = bounds.outWidth.coerceAtLeast(0),
            height = bounds.outHeight.coerceAtLeast(0),
            durationMs = null,
        )
    }

    private fun readVideo(file: File, fallback: Instant): CaptureMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
            val dated = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            CaptureMetadata(
                capturedAt = parseRetrieverDate(dated) ?: fallback,
                width = width,
                height = height,
                durationMs = duration,
            )
        } catch (_: RuntimeException) {
            CaptureMetadata(fallback, 0, 0, null)
        } finally {
            retriever.release()
        }
    }

    private fun readExifInstant(file: File): Instant? {
        return try {
            val exif = ExifInterface(file)
            val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                ?: return null
            LocalDateTime.parse(raw, exifDate).atZone(zone).toInstant()
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRetrieverDate(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return try {
            Instant.parse(raw)
        } catch (_: Exception) {
            null
        }
    }

    fun openUri(uri: Uri): File? {
        if (uri.scheme == "file") return uri.path?.let(::File)
        return null
    }
}

/**
 * Writes a 400 px longest-side JPEG thumbnail. Source bytes are left untouched.
 */
@Singleton
class ThumbnailWriter @Inject constructor() {
    fun write(source: File, destination: File, isVideo: Boolean): Boolean {
        destination.parentFile?.mkdirs()
        val bitmap = if (isVideo) {
            frameFromVideo(source)
        } else {
            decodeScaled(source)
        } ?: return false
        return FileOutputStream(destination).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
    }

    private fun decodeScaled(source: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        val longest = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        val sample = (longest / 400).coerceAtLeast(1)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = BitmapFactory.decodeFile(source.absolutePath, opts) ?: return null
        return scaleToLongest(raw, 400)
    }

    private fun frameFromVideo(source: File): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(source.absolutePath)
            val frame = retriever.frameAtTime ?: return null
            scaleToLongest(frame, 400)
        } catch (_: RuntimeException) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun scaleToLongest(source: Bitmap, longest: Int): Bitmap {
        val max = maxOf(source.width, source.height)
        if (max <= longest) return source
        val scale = longest.toFloat() / max.toFloat()
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, w, h, true)
    }
}
