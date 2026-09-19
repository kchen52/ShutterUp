package app.shutterup.ui.settings

/**
 * User-visible Settings copy. SPEC / DESIGN strings are used verbatim.
 */
object SettingsCopy {
    /** SPEC §7.5 */
    const val THEME_FOCUS_LABEL = "Theme focus (optional)"

    /** DESIGN.md §7 */
    const val THEME_FOCUS_PLACEHOLDER = "Leave blank and I'll surprise you"

    /** DESIGN.md §4.8 supporting text */
    const val THEME_FOCUS_SUPPORTING = "Leave blank to be surprised"

    const val SERIES = "Series"

    const val SERIES_SUPPORTING =
        "Some weeks arrive as a set of seven related prompts instead of seven separate ones."

    /** DESIGN.md §7 */
    const val PAUSE_SUPPORTING =
        "No prompts or notifications. Paused days don't affect your streak."

    /** DESIGN.md §7 */
    const val PRECISE_SUPPORTING =
        "Delivers at the exact minute. Android needs you to allow alarms and reminders for ShutterUp."

    /** DESIGN.md §4.8 */
    const val PRECISE_ALLOWED = "Allowed"

    /** DESIGN.md §4.8 */
    const val PRECISE_NEEDS_PERMISSION = "Needs permission → Open settings"

    /** DESIGN.md §7 */
    const val ABOUT_LINE =
        "Everything stays on your phone. ShutterUp has no internet access."

    /** DESIGN.md §7 / onboarding */
    const val AI_UNAVAILABLE =
        "On-device AI isn't available on this phone right now. ShutterUp is using its built-in prompt library."

    /** DESIGN.md §4.8 */
    const val AI_UNAVAILABLE_STATUS = "Unavailable — using the built-in library"

    /** DESIGN.md §4.8 */
    const val AI_READY = "Ready"

    /** SPEC §4.1 / §7.6 */
    const val AI_PREPARING = "Preparing on-device AI"

    /** SPEC §8.2 battery-restriction hint (no auto-exemption prompt). */
    const val BATTERY_HINT =
        "ShutterUp may be delayed by battery restrictions."

    const val BATTERY_ACTION = "Open battery settings"

    const val SECTION_DAILY = "Daily prompt"
    const val SECTION_PROMPTS = "Prompts"
    const val SECTION_PHOTOS = "Photos"
    const val SECTION_ABOUT = "About"
    const val SECTION_DEBUG = "Debug"

    const val NOTIFICATION_TIME = "Notification time"
    const val PRECISE_TIMING = "Precise timing"
    const val PAUSE = "Pause"
    const val FREEZES = "Freezes"
    const val AI_STATUS = "On-device AI status"
    const val SAVE_LOCATION_LABEL = "Save location"
    const val SAVE_LOCATION_VALUE = "Pictures/ShutterUp"
    const val VERSION = "Version"
    const val PRIVACY = "Privacy"
    const val LICENCES = "Licences"
    const val DEBUG_FAKE_AI = "Use fake AI"
    const val OPEN_SETTINGS = "Open settings"

    const val LICENCES_BODY = "Fraunces is licensed under the SIL Open Font License."

    const val STORAGE_USED = "Storage used"
    const val DEBUG_FORCE_ROLLOVER = "Force day rollover"
    const val DEBUG_SEED = "Seed 60 days of history"
    const val DEBUG_RESET = "Reset all data"

    /** PRIVACY.md stub, shown in a dialog without markdown. */
    val PRIVACY_BODY =
        "Everything stays on your phone. ShutterUp has no internet access. " +
            "No accounts, no cloud sync, no analytics, no crash reporting. " +
            "Your photos live in your Gallery (Pictures/ShutterUp). " +
            "The app's database and settings participate in Android Auto Backup like any other app data. " +
            "The on-device AI (Gemini Nano) runs entirely on your phone."
}
