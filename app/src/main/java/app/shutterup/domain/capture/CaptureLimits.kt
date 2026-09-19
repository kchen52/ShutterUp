package app.shutterup.domain.capture

/** Caps for a single day's capture session. */
object CaptureLimits {
    /** Maximum persisted [app.shutterup.domain.model.Entry] rows for one local date. */
    const val MAX_ENTRIES_PER_DAY = 3

    /** Maximum recording length offered to the camera (seconds). */
    const val MAX_RECORD_SECONDS = 15

    /** Maximum saved video length (seconds). Longer clips must be trimmed. */
    const val MAX_SAVED_VIDEO_SECONDS = 10
}
