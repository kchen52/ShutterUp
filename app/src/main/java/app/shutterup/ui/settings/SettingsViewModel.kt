package app.shutterup.ui.settings

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.MediaStore
import android.text.format.Formatter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shutterup.data.ai.NanoPromptGenerator
import app.shutterup.data.local.ShutterUpDatabase
import app.shutterup.domain.ai.Availability
import app.shutterup.domain.ai.GeneratePromptUseCase
import app.shutterup.domain.ai.PromptGenerator
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.domain.rollover.DayRolloverUseCase
import app.shutterup.widget.TodayWidgetUpdater
import app.shutterup.work.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** On-screen Settings snapshot (DESIGN.md §4.8, SPEC §8 / §10). */
data class SettingsUiState(
    val notifyTime: LocalTime = LocalTime.of(9, 0),
    val preciseTiming: Boolean = false,
    val exactAlarmAllowed: Boolean = true,
    val paused: Boolean = false,
    val themeFocus: String = "",
    val seriesEnabled: Boolean = false,
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
    val storageUsed: String = "0 B",
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
    val seriesEnabled: Boolean,
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
    private val widgetUpdater: TodayWidgetUpdater,
    @Named("primaryGenerator") private val generator: PromptGenerator,
    private val nano: NanoPromptGenerator,
    private val generatePrompt: GeneratePromptUseCase,
    private val gamification: GamificationRepository,
    private val prompts: DayPromptRepository,
    private val rollover: DayRolloverUseCase,
    private val database: ShutterUpDatabase,
    private val clock: Clock,
    private val zone: ZoneId,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val ai = MutableStateFlow(
        AiSlice(SettingsCopy.AI_UNAVAILABLE_STATUS, SettingsCopy.AI_UNAVAILABLE),
    )
    private val storageUsed = MutableStateFlow("0 B")

    val state: StateFlow<SettingsUiState> = combine(
        combine(
            combine(
                preferences.observeNotifyTime(),
                preferences.observePreciseTiming(),
                preferences.observeThemeFocus(),
                preferences.observePaused(),
                preferences.observeDebugUseFakeAi(),
            ) { notifyTime, precise, focus, paused, fakeAi ->
                PrefSlice(notifyTime, precise, focus, paused, fakeAi, seriesEnabled = false)
            },
            preferences.observeSeriesEnabled(),
        ) { slice, seriesEnabled -> slice.copy(seriesEnabled = seriesEnabled) },
        gamification.observeStreak(),
        ai,
        storageUsed,
    ) { prefs, streak, aiSlice, storage ->
        SettingsUiState(
            notifyTime = prefs.notifyTime,
            preciseTiming = prefs.precise,
            exactAlarmAllowed = canScheduleExactAlarms(appContext),
            paused = prefs.paused,
            themeFocus = prefs.focus.orEmpty(),
            seriesEnabled = prefs.seriesEnabled,
            freezeCount = streak.freezes,
            aiStatus = aiSlice.status,
            aiSupporting = aiSlice.supporting,
            versionName = versionName(appContext),
            showDebug = isDebuggable(appContext),
            debugUseFakeAi = prefs.fakeAi,
            showBatteryHint = isBatteryRestricted(appContext),
            storageUsed = storage,
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
        refreshStorage()
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
            widgetUpdater.refresh()
        }
    }

    /** Free text, ≤ 60 characters (SPEC §7.5). Empty clears the focus. */
    fun setThemeFocus(raw: String) {
        viewModelScope.launch {
            val focus = sanitizeThemeFocus(raw)
            val previous = preferences.observeThemeFocus().first()
            preferences.setThemeFocus(focus)
            if (focus != previous) {
                val today = LocalDate.now(clock.withZone(zone))
                generatePrompt.discardUnshownFuture(today)
                generatePrompt.topUpBuffer(today, focus)
            }
            scheduler.onSettingsChanged()
        }
    }

    fun setSeriesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setSeriesEnabled(enabled)
        }
    }

    fun setDebugUseFakeAi(useFake: Boolean) {
        viewModelScope.launch {
            preferences.setDebugUseFakeAi(useFake)
        }
    }

    fun forceDayRollover() {
        viewModelScope.launch {
            val today = LocalDate.now(clock.withZone(zone))
            val paused = preferences.observePaused().first()
            rollover.rollover(today.plusDays(1), paused)
        }
    }

    fun seedSixtyDays() {
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now(clock.withZone(zone))
            val cycle = listOf(
                DayStatus.COMPLETED,
                DayStatus.COMPLETED,
                DayStatus.SKIPPED,
                DayStatus.MISSED,
                DayStatus.COMPLETED_NO_PHOTO,
                DayStatus.PAUSED,
                DayStatus.COMPLETED,
            )
            for (offset in 59 downTo 0) {
                val date = today.minusDays(offset.toLong())
                val status = if (offset == 0) DayStatus.PENDING else cycle[offset % cycle.size]
                val frozen = (status == DayStatus.SKIPPED || status == DayStatus.MISSED) && offset % 4 == 0
                prompts.upsert(
                    DayPrompt(
                        date = date,
                        title = "Find the sky in a puddle",
                        oneLiner = "Turn the world upside down using any reflective surface you pass today.",
                        details = "Look down, not up.",
                        constraint = "Don't rotate the photo afterwards.",
                        theme = if (offset % 3 == 0) "Quiet hours" else "Reflections",
                        tips = emptyList(),
                        source = PromptSourceRef.LIBRARY,
                        libraryId = "seed-$offset",
                        modelName = null,
                        generatedAt = Instant.parse("2026-01-01T08:00:00Z"),
                        status = status,
                        frozen = frozen,
                        rerollUsed = false,
                    ),
                )
            }
            gamification.updateStreak(
                StreakState(current = 3, longest = 12, freezes = 1, lastProcessedDate = today),
            )
            refreshStorage()
        }
    }

    fun resetAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
            gamification.updateStreak(StreakState(0, 0, 0, null))
            refreshStorage()
        }
    }

    private fun refreshStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            storageUsed.value = Formatter.formatShortFileSize(appContext, storageUsedBytes(appContext))
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

internal fun storageUsedBytes(context: Context): Long {
    var total = 0L
    context.filesDir.walkTopDown().forEach { file ->
        if (file.isFile) total += file.length()
    }
    val resolver = context.contentResolver
    resolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Images.Media.SIZE),
        "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
        arrayOf("%${Environment.DIRECTORY_PICTURES}/ShutterUp%"),
        null,
    )?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
        if (sizeIndex >= 0) {
            while (cursor.moveToNext()) {
                total += cursor.getLong(sizeIndex)
            }
        }
    }
    return total
}
