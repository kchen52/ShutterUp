package app.shutterup.share

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes composed share cards into `cache/share/` and hands back a
 * [FileProvider] URI. Old files are deleted so the cache stays bounded.
 */
@Singleton
class ShareCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) {
    fun shareDir(): File = File(context.cacheDir, DIR_NAME)

    /**
     * Encodes [bitmap] as PNG via a temp file, then renames into place.
     * On any failure the temp and dest are deleted so nothing half-written
     * remains. Callers must run this off the main thread.
     */
    fun writePng(bitmap: Bitmap): Uri {
        val dir = shareDir()
        if (!dir.exists() && !dir.mkdirs()) {
            error("Cannot create share cache")
        }
        if (!dir.isDirectory) {
            error("Share cache is not a directory")
        }
        val id = clock.millis()
        val dest = File(dir, "share-$id.png")
        val tmp = File(dir, "share-$id.png.tmp")
        try {
            FileOutputStream(tmp).use { out ->
                val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                if (!ok) error("PNG compress failed")
                out.flush()
                out.fd.sync()
            }
            if (dest.exists() && !dest.delete()) {
                error("Cannot replace existing share file")
            }
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
            if (!dest.isFile || dest.length() == 0L) {
                error("Share file empty")
            }
            prune(keeping = dest)
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                dest,
            )
        } catch (t: Throwable) {
            tmp.delete()
            dest.delete()
            throw t
        }
    }

    /** Deletes every file in the share cache except [keeping]. */
    fun prune(keeping: File? = null) {
        val dir = shareDir()
        if (!dir.isDirectory) return
        val keepPath = keeping?.absolutePath
        dir.listFiles()?.forEach { file ->
            if (file.absolutePath != keepPath) {
                file.delete()
            }
        }
    }

    companion object {
        const val DIR_NAME: String = "share"
    }
}
