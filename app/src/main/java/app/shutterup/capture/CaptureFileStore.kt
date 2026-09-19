package app.shutterup.capture

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import app.shutterup.domain.capture.MediaNaming
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pending camera output lives under `cache/pending/` (SPEC §9). Thumbnails stay
 * in `files/thumbs/`. Originals are archived to MediaStore, not this store.
 */
@Singleton
class CaptureFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Directory for in-flight camera output (`cache/pending/`). */
    fun pendingDir(): File = File(context.cacheDir, "pending").also { it.mkdirs() }

    /** Pre-MediaStore pending location; scanned during recovery and then removed. */
    fun legacyPendingDir(): File {
        val root = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: File(context.filesDir, "Pictures")
        return File(root, "pending")
    }

    /** Pre-MediaStore originals under app-private Pictures/. */
    fun legacyPicturesDir(): File {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: File(context.filesDir, "Pictures")
    }

    /** Directory for 400 px thumbnails. */
    fun thumbsDir(): File = File(context.filesDir, "thumbs").also { it.mkdirs() }

    /** Creates a pending still file for [FileProvider]. */
    fun createPending(extension: String = "jpg"): File {
        val ext = extension.trimStart('.').ifBlank { "jpg" }
        return File(pendingDir(), "${UUID.randomUUID()}.$ext")
    }

    /** Legacy private filename used by installs before MediaStore. */
    fun destinationFile(date: LocalDate, theme: String, index: Int, extension: String): File =
        File(legacyPicturesDir().also { it.mkdirs() }, MediaNaming.fileName(date, theme, index, extension))

    /** Thumbnail path `files/thumbs/<date>.jpg` (SPEC §9). */
    fun thumbFile(date: LocalDate): File = File(thumbsDir(), "$date.jpg")

    /** Older `files/thumbs/<date>_<index>.jpg` layout; still resolved for existing rows. */
    fun thumbFile(date: LocalDate, index: Int): File =
        File(thumbsDir(), "${date}_$index.jpg")

    fun listPendingFiles(): List<File> {
        val current = pendingDir().listFiles()?.filter { it.isFile } ?: emptyList()
        val legacy = legacyPendingDir().listFiles()?.filter { it.isFile } ?: emptyList()
        return current + legacy
    }

    /** [FileProvider] URI for camera [android.app.Activity] extra output. */
    fun uriFor(file: File): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

    /** Copies a picker URI into [dest]. Originals are never moved. */
    fun copyFrom(uri: Uri, dest: File): Boolean {
        return try {
            dest.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } != null && dest.exists() && dest.length() > 0L
        } catch (_: Exception) {
            false
        }
    }

    fun openReadable(mediaUri: String): InputStream? {
        return try {
            val file = resolvePrivateFile(mediaUri)
            if (file != null && file.isFile) return file.inputStream()
            context.contentResolver.openInputStream(mediaUri.toUri())
        } catch (_: Exception) {
            null
        }
    }

    fun resolvePrivateFile(mediaUri: String): File? {
        val trimmed = mediaUri.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("/")) {
            return File(trimmed).takeIf { it.isFile }
        }
        val uri = runCatching { trimmed.toUri() }.getOrNull() ?: return null
        when (uri.scheme) {
            "file" -> return uri.path?.let(::File)?.takeIf { it.isFile }
            "content" -> {
                val segments = uri.pathSegments
                if (segments.size < 2) return null
                val relative = segments.drop(1).joinToString(File.separator)
                val mapped = when (segments.first()) {
                    "ext_pictures" -> File(legacyPicturesDir(), relative)
                    "pending" -> {
                        val cache = File(pendingDir(), relative)
                        if (cache.isFile) cache else File(legacyPendingDir(), relative)
                    }
                    "thumbs" -> File(thumbsDir(), relative)
                    else -> null
                }
                return mapped?.takeIf { it.isFile }
            }
        }
        return null
    }

    fun deleteMediaUri(mediaUri: String) {
        resolvePrivateFile(mediaUri)?.let { deleteQuietly(it) }
    }

    fun thumbsBytes(): Long {
        var total = 0L
        thumbsDir().walkTopDown().forEach { file ->
            if (file.isFile) total += file.length()
        }
        return total
    }

    fun leftoverPrivateOriginalBytes(): Long {
        var total = 0L
        val pictures = legacyPicturesDir()
        if (pictures.isDirectory) {
            pictures.walkTopDown().forEach { file ->
                if (file.isFile && file.parentFile != legacyPendingDir()) total += file.length()
            }
        }
        return total
    }

    fun deleteQuietly(file: File?) {
        try {
            file?.delete()
        } catch (_: SecurityException) {
            // Best-effort cleanup of a cancelled pending capture.
        }
    }
}
