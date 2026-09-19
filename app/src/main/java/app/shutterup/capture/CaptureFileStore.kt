package app.shutterup.capture

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import app.shutterup.domain.capture.MediaNaming
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-private capture files under [Context.getExternalFilesDir] pictures.
 * Camera output is a pending [FileProvider] URI (SPEC §2 / §4.2).
 */
@Singleton
class CaptureFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val picturesDir: File
        get() {
            val root = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: File(context.filesDir, "Pictures")
            if (!root.exists()) root.mkdirs()
            return root
        }

    /** Directory for in-flight camera output. */
    fun pendingDir(): File = File(picturesDir, "pending").also { it.mkdirs() }

    /** Directory for 400 px thumbnails. */
    fun thumbsDir(): File = File(context.filesDir, "thumbs").also { it.mkdirs() }

    /** Creates a pending still file for [FileProvider]. */
    fun createPending(extension: String = "jpg"): File {
        val ext = extension.trimStart('.').ifBlank { "jpg" }
        return File(pendingDir(), "${UUID.randomUUID()}.$ext")
    }

    /** Final name via [MediaNaming] in the private pictures directory. */
    fun destinationFile(date: LocalDate, theme: String, index: Int, extension: String): File =
        File(picturesDir, MediaNaming.fileName(date, theme, index, extension))

    /** Thumbnail path `files/thumbs/<date>_<index>.jpg`. */
    fun thumbFile(date: LocalDate, index: Int): File =
        File(thumbsDir(), "${date}_$index.jpg")

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

    fun deleteQuietly(file: File?) {
        try {
            file?.delete()
        } catch (_: SecurityException) {
            // Best-effort cleanup of a cancelled pending capture.
        }
    }
}
