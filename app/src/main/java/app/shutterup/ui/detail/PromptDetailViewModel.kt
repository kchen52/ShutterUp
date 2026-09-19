package app.shutterup.ui.detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shutterup.capture.CaptureFileStore
import app.shutterup.capture.CaptureMetadataReader
import app.shutterup.capture.ThumbnailWriter
import app.shutterup.capture.VideoClipExporter
import app.shutterup.data.notifications.NotificationHelper
import app.shutterup.domain.ai.GeneratePromptUseCase
import app.shutterup.domain.capture.CaptureDateValidator
import app.shutterup.domain.capture.CaptureLimits
import app.shutterup.domain.capture.CompleteCaptureResult
import app.shutterup.domain.capture.CompleteCaptureUseCase
import app.shutterup.domain.capture.RemainingToday
import app.shutterup.domain.capture.SkipDayUseCase
import app.shutterup.domain.capture.VideoClipRules
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.MediaKind
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
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

/** One-shot request for the system camera or picker. */
data class CaptureLaunch(
    val uri: Uri,
    val kind: MediaKind,
    val nonce: Long,
)

/** Navigation payload after a successful save. */
data class CompletionNav(
    val dateIso: String,
    val newBadges: String,
    val freezeEarned: Boolean,
    val previousStreak: Int,
)

data class PendingCapture(
    val path: String,
    val kind: MediaKind,
    val durationMs: Long?,
    val capturedAtEpoch: Long,
    val width: Int,
    val height: Int,
    val imported: Boolean,
)

data class TrimUi(
    val startMs: Long,
    val endMs: Long,
    val durationMs: Long,
)

data class PromptDetailUiState(
    val date: LocalDate,
    val prompt: DayPrompt? = null,
    val entries: List<Entry> = emptyList(),
    val remainingLabel: String = "",
    val streak: StreakState = StreakState(0, 0, 0, null),
    val snackbar: String? = null,
    val showSkipDialog: Boolean = false,
    val pending: PendingCapture? = null,
    val trim: TrimUi? = null,
    val trimming: Boolean = false,
    val launch: CaptureLaunch? = null,
    val pickGallery: Boolean = false,
    val completion: CompletionNav? = null,
    val isToday: Boolean = true,
    val debugSource: Boolean = false,
)

/**
 * Loads the day's prompt and entries, launches capture, trims video, and persists.
 */
@HiltViewModel
class PromptDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val prompts: DayPromptRepository,
    private val entries: EntryRepository,
    private val gamification: GamificationRepository,
    private val preferences: PreferencesRepository,
    private val completeCapture: CompleteCaptureUseCase,
    private val skipDay: SkipDayUseCase,
    private val generatePrompt: GeneratePromptUseCase,
    private val files: CaptureFileStore,
    private val metadata: CaptureMetadataReader,
    private val thumbs: ThumbnailWriter,
    private val trimmer: VideoClipExporter,
    private val notifications: NotificationHelper,
    private val widgetUpdater: TodayWidgetUpdater,
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
            ) { prompt, dayEntries, streak ->
                Triple(prompt, dayEntries, streak)
            }.collect { (prompt, dayEntries, streak) ->
                _state.update {
                    it.copy(
                        prompt = prompt,
                        entries = dayEntries,
                        streak = streak,
                        remainingLabel = RemainingToday.label(clock.instant().atZone(zone)),
                        isToday = date == LocalDate.now(clock.withZone(zone)),
                    )
                }
            }
        }
        viewModelScope.launch {
            if (rerollOnOpen) reroll()
            if (autoLaunch) requestStill()
        }
    }

    /** Launch a still capture when under the per-day cap. */
    fun requestStill() {
        requestCapture(MediaKind.PHOTO)
    }

    /** Launch a video capture (15 s camera limit; 10 s save rule). */
    fun requestVideo() {
        requestCapture(MediaKind.VIDEO)
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

    fun onSkipClicked() {
        _state.update { it.copy(showSkipDialog = true) }
    }

    fun dismissSkip() {
        _state.update { it.copy(showSkipDialog = false) }
    }

    fun confirmSkip() {
        viewModelScope.launch {
            skipDay()
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
        val pendingPath = _state.value.pending?.path
        val kind = _state.value.pending?.kind ?: MediaKind.PHOTO
        if (!success) {
            cameraFailures++
            pendingPath?.let { files.deleteQuietly(File(it)) }
            _state.update { it.copy(pending = null, launch = null) }
            if (cameraFailures >= 2) {
                _state.update { it.copy(pickGallery = true) }
            }
            return
        }
        val file = pendingPath?.let(::File)
        if (file == null || !file.exists() || file.length() == 0L) {
            _state.update { it.copy(snackbar = COPY_EMPTY, pending = null, launch = null) }
            return
        }
        ingestFile(file, kind, imported = false)
    }

    fun onGalleryPicked(uri: Uri?) {
        _state.update { it.copy(pickGallery = false) }
        if (uri == null) return
        viewModelScope.launch {
            val dest = files.createPending("jpg")
            if (!files.copyFrom(uri, dest)) {
                files.deleteQuietly(dest)
                _state.update { it.copy(snackbar = COPY_EMPTY) }
                return@launch
            }
            ingestFile(dest, MediaKind.PHOTO, imported = true)
        }
    }

    fun updateTrim(startMs: Long, endMs: Long) {
        val trim = _state.value.trim ?: return
        val window = VideoClipRules.clampWindow(startMs, endMs, trim.durationMs)
        _state.update { it.copy(trim = trim.copy(startMs = window.startMs, endMs = window.endMs)) }
    }

    fun confirmTrim() {
        val pending = _state.value.pending ?: return
        val trim = _state.value.trim ?: return
        if (!VideoClipRules.isSaveable(trim.endMs - trim.startMs)) {
            _state.update { it.copy(snackbar = COPY_NEED_TRIM) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(trimming = true) }
            val source = File(pending.path)
            val out = files.createPending("mp4")
            val result = trimmer.clip(files.uriFor(source), out, trim.startMs, trim.endMs)
            _state.update { it.copy(trimming = false) }
            result.fold(
                onSuccess = { file ->
                    files.deleteQuietly(source)
                    val meta = metadata.read(file, clock.instant())
                    _state.update {
                        it.copy(
                            pending = pending.copy(
                                path = file.absolutePath,
                                durationMs = meta.durationMs ?: (trim.endMs - trim.startMs),
                                width = meta.width,
                                height = meta.height,
                            ),
                            trim = null,
                        )
                    }
                },
                onFailure = {
                    files.deleteQuietly(out)
                    _state.update { it.copy(snackbar = COPY_TRIM_FAIL) }
                },
            )
        }
    }

    fun retakePending() {
        _state.value.pending?.path?.let { files.deleteQuietly(File(it)) }
        _state.update { it.copy(pending = null, trim = null) }
    }

    fun confirmPending() {
        val pending = _state.value.pending ?: return
        if (pending.kind == MediaKind.VIDEO) {
            val duration = pending.durationMs ?: 0L
            if (!VideoClipRules.isSaveable(duration)) {
                val window = VideoClipRules.clampWindow(0, duration, duration)
                _state.update {
                    it.copy(
                        trim = TrimUi(window.startMs, window.endMs, duration),
                        snackbar = COPY_NEED_TRIM,
                    )
                }
                return
            }
        }
        viewModelScope.launch {
            persist(pending)
        }
    }

    fun viewInGallery() {
        _state.update { it.copy(snackbar = COPY_GALLERY_STUB) }
    }

    private fun requestCapture(kind: MediaKind) {
        if (!_state.value.isToday) return
        viewModelScope.launch {
            if (entries.countForDate(date) >= CaptureLimits.MAX_ENTRIES_PER_DAY) {
                _state.update { it.copy(snackbar = COPY_CAP) }
                return@launch
            }
            val ext = if (kind == MediaKind.VIDEO) "mp4" else "jpg"
            val file = files.createPending(ext)
            launchNonce += 1
            _state.update {
                it.copy(
                    launch = CaptureLaunch(files.uriFor(file), kind, launchNonce),
                    pending = PendingCapture(
                        path = file.absolutePath,
                        kind = kind,
                        durationMs = null,
                        capturedAtEpoch = clock.instant().toEpochMilli(),
                        width = 0,
                        height = 0,
                        imported = false,
                    ),
                )
            }
        }
    }

    private fun ingestFile(file: File, kind: MediaKind, imported: Boolean) {
        viewModelScope.launch {
            val today = LocalDate.now(clock.withZone(zone))
            val meta = metadata.read(file, clock.instant())
            if (!CaptureDateValidator.isCapturedToday(meta.capturedAt, today, zone) || date != today) {
                files.deleteQuietly(file)
                _state.update {
                    it.copy(
                        snackbar = COPY_NOT_TODAY,
                        pending = null,
                        launch = null,
                        trim = null,
                    )
                }
                return@launch
            }
            val pending = PendingCapture(
                path = file.absolutePath,
                kind = kind,
                durationMs = meta.durationMs,
                capturedAtEpoch = meta.capturedAt.toEpochMilli(),
                width = meta.width,
                height = meta.height,
                imported = imported,
            )
            val needsTrim = kind == MediaKind.VIDEO &&
                !VideoClipRules.isSaveable(meta.durationMs ?: 0L)
            val trim = if (needsTrim) {
                val duration = meta.durationMs ?: 0L
                val window = VideoClipRules.clampWindow(0, minOf(duration, VideoClipRules.maxSavedDurationMs), duration)
                TrimUi(window.startMs, window.endMs, duration)
            } else {
                null
            }
            _state.update { it.copy(pending = pending, launch = null, trim = trim) }
        }
    }

    private suspend fun persist(pending: PendingCapture) {
        val prompt = _state.value.prompt ?: return
        val index = entries.countForDate(date) + 1
        if (index > CaptureLimits.MAX_ENTRIES_PER_DAY) {
            _state.update { it.copy(snackbar = COPY_CAP) }
            return
        }
        val source = File(pending.path)
        val ext = if (pending.kind == MediaKind.VIDEO) "mp4" else "jpg"
        val dest = files.destinationFile(date, prompt.theme, index, ext)
        try {
            source.copyTo(dest, overwrite = true)
        } catch (_: Exception) {
            _state.update { it.copy(snackbar = COPY_STORAGE) }
            return
        }
        val thumb = files.thumbFile(date, index)
        thumbs.write(dest, thumb, pending.kind == MediaKind.VIDEO)
        val entry = Entry(
            date = date,
            mediaUri = files.uriFor(dest).toString(),
            thumbPath = thumb.absolutePath,
            capturedAt = java.time.Instant.ofEpochMilli(pending.capturedAtEpoch),
            width = pending.width,
            height = pending.height,
            note = null,
            importedFromGallery = pending.imported,
            createdAt = clock.instant(),
            mediaKind = pending.kind,
        )
        when (val result = completeCapture(entry)) {
            CompleteCaptureResult.CapReached -> {
                files.deleteQuietly(dest)
                files.deleteQuietly(thumb)
                _state.update { it.copy(snackbar = COPY_CAP) }
            }
            CompleteCaptureResult.NotToday -> {
                files.deleteQuietly(dest)
                files.deleteQuietly(thumb)
                _state.update { it.copy(snackbar = COPY_NOT_TODAY) }
            }
            CompleteCaptureResult.MissingPrompt -> {
                files.deleteQuietly(dest)
                files.deleteQuietly(thumb)
            }
            is CompleteCaptureResult.Saved -> {
                files.deleteQuietly(source)
                notifications.cancel(date)
                widgetUpdater.refresh()
                _state.update {
                    it.copy(
                        pending = null,
                        trim = null,
                        completion = CompletionNav(
                            dateIso = date.toString(),
                            newBadges = result.newlyUnlocked.joinToString(",") { a -> a.id },
                            freezeEarned = result.freezeEarned,
                            previousStreak = result.previousStreak,
                        ),
                    )
                }
            }
        }
    }

    companion object {
        const val COPY_NOT_TODAY = "Only photos taken today count"
        const val COPY_CAP = "Three captures is the most for one day."
        const val COPY_TRIM_FAIL = "The clip could not be trimmed. Nothing was saved."
        const val COPY_NEED_TRIM = "Trim the clip to 10 seconds."
        const val COPY_EMPTY = "That capture was empty. Nothing was saved."
        const val COPY_STORAGE = "Not enough storage to save."
        const val COPY_GALLERY_STUB = "VIEW IN GALLERY"
    }
}
