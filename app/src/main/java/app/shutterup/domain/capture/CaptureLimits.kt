package app.shutterup.domain.capture

/** Caps for a single day's capture session (SPEC §1.2: one photo answers one prompt). */
object CaptureLimits {
    /** Maximum persisted [app.shutterup.domain.model.Entry] rows for one local date. */
    const val MAX_ENTRIES_PER_DAY = 1
}
