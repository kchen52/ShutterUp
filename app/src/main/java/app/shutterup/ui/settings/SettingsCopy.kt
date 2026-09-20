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
        "Some weeks arrive as a set of seven related prompts instead of seven separate ones. Turning this on starts a series tomorrow. Today's prompt stays."

    const val SERIES_SUPPORTING_ON =
        "Some weeks arrive as a set of seven related prompts instead of seven separate ones. The current series finishes even if you turn this off."

    const val WHERE_YOU_ARE = "Where you are"

    const val WHERE_YOU_ARE_UNSET = "Not set"

    const val WHERE_YOU_ARE_SUPPORTING =
        "A city-level guess for daylight. No location permission, nothing leaves the phone."

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

    /** DESIGN.md §4.8 */
    const val LIBRARY_STATUS = "Built-in library"

    const val LIBRARY_SUPPORTING =
        "Daily prompts come from a curated bank on this phone."

    const val CHOOSING_PROMPT = "Choosing today's prompt."

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
    const val SAVE_LOCATION_LABEL = "Save location"
    const val SAVE_LOCATION_VALUE = "Pictures/ShutterUp"
    const val VERSION = "Version"
    const val PRIVACY = "Privacy"
    const val LICENCES = "Licences"
    const val DEBUG_FAKE_AI = "Use fake AI"
    const val OPEN_SETTINGS = "Open settings"

    const val LICENCES_BODY = "Fraunces is licensed under the SIL Open Font License."

    const val STORAGE_USED = "Storage used"
    const val BACKUP = "Backup progress"
    const val BACKUP_SUPPORTING =
        "Writes a ZIP of prompts, notes, streaks, badges, and a copy of each photo. Gallery originals stay if you uninstall."
    const val RESTORE = "Restore progress"
    const val RESTORE_SUPPORTING =
        "Puts prompts, notes, streaks, and badges back. Missing Gallery photos are copied from the backup."
    const val RESTORE_TITLE = "Restore this backup?"
    const val RESTORE_BODY =
        "Current prompts, notes, streaks, and badges will be replaced. Photos already in Gallery stay as they are."
    const val RESTORE_CONFIRM = "Restore"
    const val BACKUP_SAVED = "Backup saved."
    const val RESTORE_DONE = "Progress restored."
    const val BACKUP_FAILED = "The backup didn't come together. Try again."
    const val RESTORE_INVALID = "That file isn't a ShutterUp backup."
    const val DEBUG_FORCE_ROLLOVER = "Force day rollover"
    const val DEBUG_SEED = "Seed 60 days of history"
    const val DEBUG_RESET = "Reset all data"

    /** PRIVACY.md stub, shown in a dialog without markdown. */
    val PRIVACY_BODY =
        "Everything stays on your phone. ShutterUp has no internet access. " +
            "No accounts, no cloud sync, no analytics, no crash reporting. " +
            "Your photos live in your Gallery (Pictures/ShutterUp). " +
            "The app's database and settings participate in Android Auto Backup like any other app data. " +
            "Settings → Photos can write a backup ZIP of progress and photos that you keep. " +
            "The daily prompts come from a built-in library that stays on your phone."
}
