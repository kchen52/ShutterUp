package app.shutterup.capture

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import app.shutterup.domain.capture.MediaNaming
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inserts original JPEG bytes into [MediaStore.Images] under Pictures/ShutterUp
 * (SPEC §9). Bytes are stream-copied; the image is never re-encoded.
 */
@Singleton
class MediaStorePhotoArchiver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun insertOriginal(source: File, displayName: String, capturedAt: Instant): Uri {
        source.inputStream().use { input ->
            return insertOriginal(input, source.length(), displayName, capturedAt, source)
        }
    }

    fun insertOriginalFromStream(
        input: java.io.InputStream,
        sizeHint: Long,
        displayName: String,
        capturedAt: Instant,
    ): Uri = insertOriginal(input, sizeHint, displayName, capturedAt, exifSource = null)

    fun delete(uriString: String): Boolean {
        return runCatching {
            context.contentResolver.delete(uriString.toUri(), null, null) > 0
        }.getOrDefault(false)
    }

    fun exists(uriString: String): Boolean {
        return runCatching {
            context.contentResolver.openInputStream(uriString.toUri()).use { it != null }
        }.getOrDefault(false)
    }

    fun albumBytes(): Long {
        val resolver = context.contentResolver
        var total = 0L
        resolver.query(
            collection(),
            arrayOf(MediaStore.Images.Media.SIZE),
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("%${Environment.DIRECTORY_PICTURES}/ShutterUp%"),
            null,
        )?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
            if (sizeIndex >= 0) {
                while (cursor.moveToNext()) {
                    total += cursor.getLong(sizeIndex)
                }
            }
        }
        return total
    }

    /** Match a Gallery original by filename after a reinstall (SPEC §14). */
    fun findByDisplayName(displayName: String): Uri? {
        if (displayName.isBlank()) return null
        val inAlbum = queryId(
            "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
            arrayOf(displayName, "%${Environment.DIRECTORY_PICTURES}/ShutterUp%"),
        )
        if (inAlbum != null) return inAlbum
        return queryId(
            "${MediaStore.Images.Media.DISPLAY_NAME} = ?",
            arrayOf(displayName),
        )
    }

    private fun queryId(selection: String, args: Array<String>): Uri? {
        return runCatching {
            context.contentResolver.query(
                collection(),
                arrayOf(MediaStore.Images.Media._ID),
                selection,
                args,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                ContentUris.withAppendedId(collection(), cursor.getLong(0))
            }
        }.getOrNull()
    }

    fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                cursor.getString(0)
            }
        }.getOrNull()
    }

    fun queryRelativePath(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.RELATIVE_PATH),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                cursor.getString(0)
            }
        }.getOrNull()
    }

    private fun insertOriginal(
        input: java.io.InputStream,
        sizeHint: Long,
        displayName: String,
        capturedAt: Instant,
        exifSource: File?,
    ): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, MediaNaming.RELATIVE_PATH)
            put(MediaStore.Images.Media.IS_PENDING, 1)
            put(MediaStore.Images.Media.DATE_TAKEN, capturedAt.toEpochMilli())
            if (sizeHint > 0L) put(MediaStore.Images.Media.SIZE, sizeHint)
            orientationDegrees(exifSource)?.let { put(MediaStore.Images.Media.ORIENTATION, it) }
        }
        val uri = resolver.insert(collection(), values)
            ?: error("MediaStore insert returned null")
        try {
            val out = resolver.openOutputStream(uri)
                ?: error("Could not open MediaStore output stream")
            out.use { output -> input.copyTo(output) }
            val done = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, done, null, null)
            return uri
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
    }

    private fun collection(): Uri = try {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } catch (_: Exception) {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    private fun orientationDegrees(source: File?): Int? {
        if (source == null) return null
        return try {
            when (ExifInterface(source).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Exception) {
            null
        }
    }
}
