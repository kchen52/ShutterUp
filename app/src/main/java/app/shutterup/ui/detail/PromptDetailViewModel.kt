package app.shutterup.ui.detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shutterup.capture.CaptureFileStore
import app.shutterup.capture.CaptureMetadataReader
import app.shutterup.capture.CapturePipeline
import app.shutterup.capture.PendingCapture
import app.shutterup.capture.PersistOutcome
import app.shutterup.capture.SkipTodayAction
import app.shutterup.data.notifications.NotificationHelper
import app.shutterup.domain.ai.GeneratePromptUseCase
import app.shutterup.domain.capture.CaptureDateValidator
import app.shutterup.domain.capture.CaptureLimits
import app.shutterup.work.NotificationScheduler
import app.shutterup.domain.capture.RemainingToday
import app.shutterup.domain.capture.DaylightRemaining
import app.shutterup.domain.geo.CityCatalog
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import app.shutterup.domain.repository.SeriesRepository
import app.shutterup.domain.series.SeriesProgress
import app.shutterup.domain.series.SeriesProgressCalculator
import app.shutterup.widget.TodayWidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One-shot request for the system camera. */
data class CaptureLaunch(
    val uri: Uri,
    val nonce: Long,
)

/** Navigation payload after a successful save. */
data class CompletionNav(
    val dateIso: String,
    val newBadges: String,
    val freezeEarned: Boolean,
    val previousStreak: Int,
)

data class PromptDetailUiState(
    val date: LocalDate,
    val prompt: DayPrompt? = null,
    val entries: List<Entry> = emptyList(),
    val remainingLabel: String = "",
    val streak: StreakState = StreakState(0, 0, 0, null),
    val snackbar: String? = null,
    val showSkipDialog: Boolean = false,
    val showStorageDialog: Boolean = false,
    val offerGallery: Boolean = false,
    val pending: PendingCapture? = null,
    val launch: CaptureLaunch? = null,
    val pickGallery: Boolean = false,
    val completion: CompletionNav? = null,
    val isToday: Boolean = true,
    val debugSource: Boolean = false,
    val seriesProgress: SeriesProgress? = null,
)

/**
 * Loads the day's prompt, launches [android.provider.MediaStore.ACTION_IMAGE_CAPTURE],
 * and persists one still (SPEC §1.2 / §4.2).
 */
@HiltViewModel
class PromptDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val prompts: DayPromptRepository,
    private val entries: EntryRepository,
    private val gamification: GamificationRepository,
    private val preferences: PreferencesRepository,
    private val pipeline: CapturePipeline,
    private val skipToday: SkipTodayAction,
    private val seriesRepo: SeriesRepository,
    private val generatePrompt: GeneratePromptUseCase,
    private val files: CaptureFileStore,
    private val metadata: CaptureMetadataReader,
    private val notifications: NotificationHelper,
    private val widgetUpdater: TodayWidgetUpdater,
    private val scheduler: NotificationScheduler,
    private val clock: Clock,
    private val zone: ZoneId,
) : ViewModel() {

    val date: LocalDate = savedStateHandle.get<String>("dateIso")?.let(LocalDate::parse)
        ?: LocalDate.now(clock.withZone(zone))

    private val autoLaunch = savedStateHandle.get<Boolean>("autoLaunchCamera") ?: false
    private val rerollOnOpen = savedStateHandle.get<Boolean>("reroll") ?: false

    private val _state = MutableStateFlow(
        PromptDetailUiState(
            date = date,
            remainingLabel = RemainingToday.label(clock.instant().atZone(zone)),
            isToday = date == LocalDate.now(clock.withZone(zone)),
            pending = restorePending(),
        ),
    )
    val state: StateFlow<PromptDetailUiState> = _state.asStateFlow()

    private var cameraFailures = 0
    private var launchNonce = 0L

    init {
        viewModelScope.launch {
            combine(
                prompts.observeDay(date),
                entries.observeEntries(date),
                gamification.observeStreak(),
                seriesRepo.observeCovering(date),
                combine(
                    prompts.observeDays(date.minusDays(6), date.plusDays(6)),
                    preferences.observeCoarseCityId(),
                ) { nearby, cityId -> nearby to cityId },
            ) { prompt, dayEntries, streak, series, nearbyAndCity ->
                val (nearby, cityId) = nearbyAndCity
                val city = CityCatalog.find(cityId)
                val now = clock.instant().atZone(zone)
                _state.update {
                    it.copy(
                        prompt = prompt,
                        entries = dayEntries,
                        streak = streak,
                        remainingLabel = DaylightRemaining.label(now, city?.latitude, city?.longitude),
                        isToday = date == LocalDate.now(clock.withZone(zone)),
                        seriesProgress = series?.let {
                            SeriesProgressCalculator.progress(it, nearby, date)
                        },
                    )
                }
            }.collect { }
        }
        viewModelScope.launch {
            if (rerollOnOpen) reroll()
            if (autoLaunch) requestStill()
        }
    }

    /** Launch a still capture when under the per-day cap (retake replaces). */
    fun requestStill() {
        if (!_state.value.isToday) return
        viewModelScope.launch {
            val count = entries.countForDate(date)
            if (count > CaptureLimits.MAX_ENTRIES_PER_DAY) {
                _state.update { it.copy(snackbar = COPY_CAP) }
                return@launch
            }
            val file = files.createPending("jpg")
            launchNonce += 1
            val pending = PendingCapture(
                path = file.absolutePath,
                capturedAtEpoch = clock.instant().toEpochMilli(),
                width = 0,
                height = 0,
                imported = false,
            )
            rememberPending(pending)
            _state.update {
                it.copy(
                    launch = CaptureLaunch(files.uriFor(file), launchNonce),
                    pending = pending,
                    showStorageDialog = false,
                )
            }
        }
    }

    fun consumeLaunch() {
        _state.update { it.copy(launch = null) }
    }

    fun consumeGallery() {
        _state.update { it.copy(pickGallery = false) }
    }

    fun consumeSnackbar() {
        _state.update { it.copy(snackbar = null) }
    }

    fun consumeCompletion() {
        _state.update { it.copy(completion = null) }
    }

    fun chooseFromGallery() {
        _state.update { it.copy(pickGallery = true) }
    }

    fun dismissStorageDialog() {
        _state.update { it.copy(showStorageDialog = false) }
    }

    fun retryStorage() {
        val pending = _state.value.pending ?: return
        viewModelScope.launch { persist(pending) }
    }

    fun onSkipClicked() {
        _state.update { it.copy(showSkipDialog = true) }
    }

    fun dismissSkip() {
        _state.update { it.copy(showSkipDialog = false) }
    }

    fun confirmSkip() {
        viewModelScope.launch {
            skipToday()
            widgetUpdater.refresh()
            _state.update { it.copy(showSkipDialog = false) }
        }
    }

    fun reroll() {
        viewModelScope.launch {
            val prompt = _state.value.prompt ?: return@launch
            if (prompt.rerollUsed) return@launch
            val focus = preferences.observeThemeFocus().first()
            generatePrompt.reroll(date, focus)
        }
    }

    fun onCameraReturned(success: Boolean) {
        val pendingPath = _state.value.pending?.path ?: savedPendingPath()
        if (!success) {
            cameraFailures++
            pendingPath?.let { files.deleteQuietly(File(it)) }
            rememberPending(null)
            _state.update { it.copy(pending = null, launch = null) }
            if (cameraFailures >= 2) {
                _state.update { it.copy(offerGallery = true) }
            }
            return
        }
        val file = pendingPath?.let(::File)
        if (file == null || !file.exists() || file.length() == 0L) {
            viewModelScope.launch {
                val prompt = prompts.getDay(date)
                if (prompt?.status == DayStatus.COMPLETED) {
                    rememberPending(null)
                    notifications.cancel(date)
                    _state.update {
                        it.copy(
                            pending = null,
                            launch = null,
                            completion = CompletionNav(
                                dateIso = date.toString(),
                                newBadges = "",
                                freezeEarned = false,
                                previousStreak = it.streak.current,
                            ),
                        )
                    }
                    return@launch
                }
                cameraFailures++
                file?.let(files::deleteQuietly)
                rememberPending(null)
                _state.update { it.copy(pending = null, launch = null) }
                if (cameraFailures >= 2) {
                    _state.update { it.copy(offerGallery = true) }
                } else {
                    requestStill()
                }
            }
            return
        }
        cameraFailures = 0
        ingestFile(file, imported = false, pickerUri = null)
    }

    fun onGalleryPicked(uri: Uri?) {
        _state.update { it.copy(pickGallery = false) }
        if (uri == null) return
        viewModelScope.launch {
            val dest = files.createPending("jpg")
            if (!files.copyFrom(uri, dest)) {
                files.deleteQuietly(dest)
                _state.update { it.copy(showStorageDialog = true) }
                return@launch
            }
            ingestFile(dest, imported = true, pickerUri = uri)
        }
    }

    private fun ingestFile(file: File, imported: Boolean, pickerUri: Uri?) {
        viewModelScope.launch {
            val today = LocalDate.now(clock.withZone(zone))
            val meta = if (pickerUri != null) {
                metadata.readPicked(pickerUri, file, clock.instant())
            } else {
                metadata.read(file, clock.instant())
            }
            if (!CaptureDateValidator.isCapturedToday(meta.capturedAt, today, zone) || date != today) {
                files.deleteQuietly(file)
                rememberPending(null)
                _state.update {
                    it.copy(
                        snackbar = COPY_NOT_TODAY,
                        pending = null,
                        launch = null,
                    )
                }
                return@launch
            }
            val pending = PendingCapture(
                path = file.absolutePath,
                capturedAtEpoch = meta.capturedAt.toEpochMilli(),
                width = meta.width,
                height = meta.height,
                imported = imported,
            )
            rememberPending(pending)
            _state.update { it.copy(pending = pending, launch = null) }
            persist(pending)
        }
    }

    private suspend fun persist(pending: PendingCapture) {
        when (val result = pipeline.persist(pending, date)) {
            PersistOutcome.CapReached -> {
                _state.update { it.copy(snackbar = COPY_CAP, showStorageDialog = false) }
            }
            PersistOutcome.NotToday -> {
                _state.update { it.copy(snackbar = COPY_NOT_TODAY, showStorageDialog = false) }
            }
            PersistOutcome.MissingPrompt, PersistOutcome.Undecodable -> {
                _state.update { it.copy(showStorageDialog = true) }
            }
            PersistOutcome.StorageError -> {
                _state.update { it.copy(showStorageDialog = true) }
            }
            is PersistOutcome.Saved -> {
                rememberPending(null)
                notifications.cancel(date)
                widgetUpdater.refresh()
                scheduler.scheduleTopUpNow()
                _state.update {
                    it.copy(
                        pending = null,
                        offerGallery = false,
                        showStorageDialog = false,
                        completion = CompletionNav(
                            dateIso = date.toString(),
                            newBadges = result.result.newlyUnlocked.joinToString(",") { a -> a.id },
                            freezeEarned = result.result.freezeEarned,
                            previousStreak = result.result.previousStreak,
                        ),
                    )
                }
            }
        }
    }

    private fun rememberPending(pending: PendingCapture?) {
        savedStateHandle[KEY_PENDING_PATH] = pending?.path
        savedStateHandle[KEY_PENDING_IMPORTED] = pending?.imported
        savedStateHandle[KEY_PENDING_CAPTURED_AT] = pending?.capturedAtEpoch
        savedStateHandle[KEY_PENDING_WIDTH] = pending?.width
        savedStateHandle[KEY_PENDING_HEIGHT] = pending?.height
    }

    private fun savedPendingPath(): String? = savedStateHandle.get<String>(KEY_PENDING_PATH)

    private fun restorePending(): PendingCapture? {
        val path = savedPendingPath() ?: return null
        val file = File(path)
        if (!file.exists()) return null
        return PendingCapture(
            path = path,
            capturedAtEpoch = savedStateHandle.get<Long>(KEY_PENDING_CAPTURED_AT) ?: file.lastModified(),
            width = savedStateHandle.get<Int>(KEY_PENDING_WIDTH) ?: 0,
            height = savedStateHandle.get<Int>(KEY_PENDING_HEIGHT) ?: 0,
            imported = savedStateHandle.get<Boolean>(KEY_PENDING_IMPORTED) ?: false,
        )
    }

    companion object {
        const val COPY_NOT_TODAY = "That one's from another day — only today's photos count."
        const val COPY_CAP = "Today already has a photo."
        const val COPY_GALLERY_CARD =
            "Camera didn't return a photo. You can pick one you took today instead."
        const val COPY_STORAGE_BODY = "Today's capture isn't completed."
        const val COPY_TRY_AGAIN = "Try again"
        const val COPY_CHOOSE_GALLERY = "Choose from Gallery"
        private const val KEY_PENDING_PATH = "pendingPath"
        private const val KEY_PENDING_IMPORTED = "pendingImported"
        private const val KEY_PENDING_CAPTURED_AT = "pendingCapturedAt"
        private const val KEY_PENDING_WIDTH = "pendingWidth"
        private const val KEY_PENDING_HEIGHT = "pendingHeight"
    }
}
