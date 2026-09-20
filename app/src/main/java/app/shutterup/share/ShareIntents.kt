package app.shutterup.share

import android.content.ClipData
import android.content.Intent
import android.net.Uri

/** System share-sheet hand-off. No network permission; the user picks the destination. */
object ShareIntents {
    const val MIME_PNG: String = "image/png"

    /**
     * [Intent.ACTION_SEND] of [uri] as a PNG. Read permission is granted via
     * both [Intent.setData] and [Intent.clipData] because extras alone do not
     * carry [Intent.FLAG_GRANT_READ_URI_PERMISSION] to the receiving app.
     */
    fun send(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            setDataAndType(uri, MIME_PNG)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("share-card", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** Chooser wrapping [send]. Title is null so the system label is used. */
    fun chooser(uri: Uri): Intent = Intent.createChooser(send(uri), null)
}
