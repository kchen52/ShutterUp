package app.shutterup.domain.capture

/** Inclusive start/end window on a recorded clip, in milliseconds. */
data class VideoClipWindow(
    val startMs: Long,
    val endMs: Long,
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

/**
 * User-driven trim rules: saved video must be at most 10 seconds.
 * Windows are clamped to the source; they are never auto-centred.
 */
object VideoClipRules {
    val maxSavedDurationMs: Long
        get() = CaptureLimits.MAX_SAVED_VIDEO_SECONDS * 1_000L

    /** True when [durationMs] may be persisted without a further trim. */
    fun isSaveable(durationMs: Long): Boolean = durationMs in 1..maxSavedDurationMs

    /**
     * Clamps [startMs], [endMs] into `[0, sourceDurationMs]` and shortens
     * an over-long window from the end so the user-chosen start is kept.
     */
    fun clampWindow(startMs: Long, endMs: Long, sourceDurationMs: Long): VideoClipWindow {
        val duration = sourceDurationMs.coerceAtLeast(0L)
        var start = startMs.coerceIn(0L, duration)
        var end = endMs.coerceIn(0L, duration)
        if (end < start) {
            val swap = start
            start = end
            end = swap
        }
        if (end - start > maxSavedDurationMs) {
            end = start + maxSavedDurationMs
            if (end > duration) {
                end = duration
                start = (end - maxSavedDurationMs).coerceAtLeast(0L)
            }
        }
        return VideoClipWindow(start, end)
    }
}
