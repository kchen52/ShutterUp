package app.shutterup.ui.settings

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shutterup.data.ai.NanoPromptGenerator
import app.shutterup.domain.ai.Availability
import app.shutterup.domain.ai.PromptGenerator
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.work.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** On-screen Settings snapshot (DESIGN.md §4.8, SPEC §8 / §10). */
data class SettingsUiState(
    val notifyTime: LocalTime = LocalTime.of(9, 0),
    val preciseTiming: Boolean = false,
    val exactAlarmAllowed: Boolean = true,
    val paused: Boolean = false,
    val themeFocus: String = "",
    val freezeCount: Int = 0,
    val aiStatus: String = "",
    val aiSupporting: String? = null,
    val saveLocation: String = SettingsCopy.SAVE_LOCATION_VALUE,
    val versionName: String = "",
    val privacyBody: String = SettingsCopy.PRIVACY_BODY,
    val aboutLine: String = SettingsCopy.ABOUT_LINE,
    val showDebug: Boolean = false,
    val debugUseFakeAi: Boolean = false,
    val showBatteryHint: Boolean = false,
) {
    val preciseTimingStatus: String =
        if (exactAlarmAllowed) SettingsCopy.PRECISE_ALLOWED else SettingsCopy.PRECISE_NEEDS_PERMISSION
}

private data class PrefSlice(
    val notifyTime: LocalTime,
    val precise: Boolean,
    val focus: String?,
    val paused: Boolean,
    val fakeAi: Boolean,
)

private data class AiSlice(
    val status: String,
    val supporting: String?,
)

/**
 * Settings: notify time, precise timing, pause, theme focus, freeze count,
 * AI status, about/privacy, debug fake-AI. Export ZIP is v1.1 (SPEC §17) and
 * is omitted because no pipeline exists yet.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val scheduler: NotificationScheduler,
    @Named("primaryGenerator") private val generator: PromptGenerator,
    private val nano: NanoPromptGenerator,
    gamification: GamificationRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val ai = MutableStateFlow(
        AiSlice(SettingsCopy.AI_UNAVAILABLE_STATUS, SettingsCopy.AI_UNAVAILABLE),
    )

    val state: StateFlow<SettingsUiState> = combine(
        combine(
            preferences.observeNotifyTime(),
            preferences.observePreciseTiming(),
            preferences.observeThemeFocus(),
            preferences.observePaused(),
            preferences.observeDebugUseFakeAi(),
        ) { notifyTime, precise, focus, paused, fakeAi ->
            PrefSlice(notifyTime, precise, focus, paused, fakeAi)
        },
        gamification.observeStreak(),
        ai,
    ) { prefs, streak, aiSlice ->
        SettingsUiState(
            notifyTime = prefs.notifyTime,
            preciseTiming = prefs.precise,
            exactAlarmAllowed = canScheduleExactAlarms(appContext),
            paused = prefs.paused,
            themeFocus = prefs.focus.orEmpty(),
            freezeCount = streak.freezes,
            aiStatus = aiSlice.status,
            aiSupporting = aiSlice.supporting,
            versionName = versionName(appContext),
            showDebug = isDebuggable(appContext),
            debugUseFakeAi = prefs.fakeAi,
            showBatteryHint = isBatteryRestricted(appContext),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(
            exactAlarmAllowed = canScheduleExactAlarms(appContext),
            versionName = versionName(appContext),
            showDebug = isDebuggable(appContext),
            showBatteryHint = isBatteryRestricted(appContext),
            aiStatus = SettingsCopy.AI_UNAVAILABLE_STATUS,
            aiSupporting = SettingsCopy.AI_UNAVAILABLE,
        ),
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val availability = runCatching { generator.availability() }
                .getOrDefault(Availability.UNAVAILABLE)
            val modelName = runCatching { nano.baseModelName() }.getOrNull()
            ai.value = AiSlice(
                status = aiStatusLine(availability, modelName),
                supporting = aiSupporting(availability),
            )
        }
    }

    /** Hour/minute from the time picker; reschedules the daily worker (SPEC §8.2). */
    fun setNotifyTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferences.setNotifyTime(LocalTime.of(hour, minute))
            scheduler.onSettingsChanged()
        }
    }

    fun setPreciseTiming(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setPreciseTiming(enabled)
            scheduler.onSettingsChanged()
        }
    }

    fun setPaused(paused: Boolean) {
        viewModelScope.launch {
            preferences.setPaused(paused)
            scheduler.onSettingsChanged()
        }
    }

    /** Free text, ≤ 60 characters (SPEC §7.5). Empty clears the focus. */
    fun setThemeFocus(raw: String) {
        viewModelScope.launch {
            preferences.setThemeFocus(sanitizeThemeFocus(raw))
            scheduler.onSettingsChanged()
        }
    }

    fun setDebugUseFakeAi(useFake: Boolean) {
        viewModelScope.launch {
            preferences.setDebugUseFakeAi(useFake)
        }
    }

    companion object {
        const val THEME_FOCUS_MAX = 60
    }
}

/** SPEC §7.5: theme focus is optional free text, at most 60 characters. */
fun sanitizeThemeFocus(raw: String): String? =
    raw.trim().take(SettingsViewModel.THEME_FOCUS_MAX).ifEmpty { null }

internal fun aiStatusLine(availability: Availability, modelName: String?): String = when (availability) {
    Availability.AVAILABLE -> {
        val name = modelName?.takeIf { it.isNotBlank() }
        if (name == null) SettingsCopy.AI_READY else "${SettingsCopy.AI_READY} · $name"
    }
    Availability.DOWNLOADABLE, Availability.DOWNLOADING -> SettingsCopy.AI_PREPARING
    Availability.UNAVAILABLE -> SettingsCopy.AI_UNAVAILABLE_STATUS
}

internal fun aiSupporting(availability: Availability): String? = when (availability) {
    Availability.UNAVAILABLE -> SettingsCopy.AI_UNAVAILABLE
    else -> null
}

private fun canScheduleExactAlarms(context: Context): Boolean {
    val alarm = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
    return alarm?.canScheduleExactAlarms() != false
}

private fun isDebuggable(context: Context): Boolean =
    context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

private fun versionName(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
} catch (_: PackageManager.NameNotFoundException) {
    ""
}

private fun isBatteryRestricted(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    if (activityManager?.isBackgroundRestricted == true) return true
    val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return false
    return usage.appStandbyBucket >= UsageStatsManager.STANDBY_BUCKET_RESTRICTED
}
