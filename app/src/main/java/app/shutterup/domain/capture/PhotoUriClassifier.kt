package app.shutterup.domain.capture

/**
 * Classifies an [app.shutterup.domain.model.Entry.mediaUri] string without Android types
 * so migration rules stay JVM-testable.
 */
enum class PhotoUriKind {
    /** `content://media/…` — already in MediaStore. */
    MEDIA_STORE,

    /** App-private FileProvider, `file://`, or a raw filesystem path. */
    PRIVATE,

    /** Empty or unrecognised. */
    UNKNOWN,
}

object PhotoUriClassifier {
    fun kind(mediaUri: String): PhotoUriKind {
        val uri = mediaUri.trim()
        if (uri.isEmpty()) return PhotoUriKind.UNKNOWN
        return when {
            isMediaStore(uri) -> PhotoUriKind.MEDIA_STORE
            isPrivate(uri) -> PhotoUriKind.PRIVATE
            else -> PhotoUriKind.UNKNOWN
        }
    }

    /** True when the entry still points at an app-private original. */
    fun needsMigration(mediaUri: String): Boolean =
        kind(mediaUri) != PhotoUriKind.MEDIA_STORE

    fun isMediaStore(mediaUri: String): Boolean {
        val uri = mediaUri.trim()
        if (!uri.startsWith("content://")) return false
        return uri.removePrefix("content://").startsWith("media/")
    }

    private fun isPrivate(mediaUri: String): Boolean {
        val uri = mediaUri.trim()
        if (uri.startsWith("file:")) return true
        if (uri.startsWith("/")) return true
        return uri.startsWith("content://") && uri.contains(".fileprovider")
    }
}
