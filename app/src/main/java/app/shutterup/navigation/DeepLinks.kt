package app.shutterup.navigation

/**
 * Deep-link contract (SPEC §8.1; widget Shoot uses the detail host).
 *
 * - Notification tap: `shutterup://day/{date}`
 * - Notification / widget Shoot: `shutterup://day/{date}?autoLaunchCamera=true`
 *   or `shutterup://detail/{date}?autoLaunchCamera=true`
 * - Widget body: `shutterup://detail/{date}`
 */
object DeepLinks {
    const val SCHEME = "shutterup"
    const val HOST_DAY = "day"
    const val HOST_DETAIL = "detail"
    const val QUERY_AUTO_LAUNCH = "autoLaunchCamera"
    const val QUERY_REROLL = "reroll"

    fun day(dateIso: String, autoLaunchCamera: Boolean = false, reroll: Boolean = false): String =
        buildUri(HOST_DAY, dateIso, autoLaunchCamera, reroll)

    fun detail(dateIso: String, autoLaunchCamera: Boolean = false): String =
        buildUri(HOST_DETAIL, dateIso, autoLaunchCamera, reroll = false)

    fun isPromptLink(scheme: String?, host: String?): Boolean =
        scheme == SCHEME && (host == HOST_DAY || host == HOST_DETAIL)

    private fun buildUri(
        host: String,
        dateIso: String,
        autoLaunchCamera: Boolean,
        reroll: Boolean,
    ): String {
        val base = "$SCHEME://$host/$dateIso"
        val params = buildList {
            if (autoLaunchCamera) add("$QUERY_AUTO_LAUNCH=true")
            if (reroll) add("$QUERY_REROLL=true")
        }
        return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
    }
}
