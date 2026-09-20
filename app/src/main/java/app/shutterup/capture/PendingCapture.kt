package app.shutterup.capture

/** In-flight still waiting to be archived into MediaStore (SPEC §9). */
data class PendingCapture(
    val path: String,
    val capturedAtEpoch: Long,
    val width: Int,
    val height: Int,
    val imported: Boolean,
)
