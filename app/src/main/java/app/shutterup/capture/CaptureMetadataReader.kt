package app.shutterup.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
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

/** EXIF / MediaStore metadata for a still. Bytes are never re-encoded. */
data class CaptureMetadata(
    val capturedAt: Instant,
    val width: Int,
    val height: Int,
)

/** Reads capture time and size from a private JPEG (SPEC §4.3 date gate). */
@Singleton
class CaptureMetadataReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val zone: ZoneId,
) {
    private val exifDate: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

    fun read(file: File, fallback: Instant): CaptureMetadata = readImage(file, fallback)

    /** Bounds-decode only: the original bytes are never re-encoded. */
    fun isDecodable(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        return bounds.outWidth > 0 && bounds.outHeight > 0
    }

    /**
     * Capture time for pending-file recovery: EXIF first, then file mtime.
     * Does not fall back to "now" — that would make yesterday's leftovers look like today.
     */
    fun capturedAtForRecovery(file: File): Instant =
        readExifInstant(file) ?: Instant.ofEpochMilli(file.lastModified())

    /**
     * Picker photos: prefer [MediaStore.Images.Media.DATE_TAKEN], then EXIF
     * `DateTimeOriginal`, then [fallback].
     */
    fun readPicked(uri: Uri, file: File, fallback: Instant): CaptureMetadata {
        val image = readImage(file, fallback)
        val taken = queryDateTaken(uri)
        return if (taken != null) image.copy(capturedAt = taken) else image
    }

    private fun readImage(file: File, fallback: Instant): CaptureMetadata {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val capturedAt = readExifInstant(file) ?: fallback
        return CaptureMetadata(
            capturedAt = capturedAt,
            width = bounds.outWidth.coerceAtLeast(0),
            height = bounds.outHeight.coerceAtLeast(0),
        )
    }

    private fun queryDateTaken(uri: Uri): Instant? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.DATE_TAKEN),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val taken = cursor.getLong(0)
                if (taken > 0L) Instant.ofEpochMilli(taken) else null
            }
        } catch (_: Exception) {
            null
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
}

/**
 * Writes a 400 px longest-side JPEG thumbnail. Source bytes are left untouched.
 */
@Singleton
class ThumbnailWriter @Inject constructor() {
    fun write(source: File, destination: File): Boolean {
        destination.parentFile?.mkdirs()
        val bitmap = decodeScaled(source) ?: return false
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
        val oriented = applyExifOrientation(raw, source)
        return scaleToLongest(oriented, 400)
    }

    private fun applyExifOrientation(source: Bitmap, file: File): Bitmap {
        val orientation = try {
            ExifInterface(file).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } catch (_: Exception) {
            return source
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
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
