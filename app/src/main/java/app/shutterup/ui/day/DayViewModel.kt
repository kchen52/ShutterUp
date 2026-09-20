package app.shutterup.ui.day

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.Entry
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.share.ShareCardExporter
import app.shutterup.share.ShareCopy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Single-day journal: prompt, entries, status, badges, note. */
data class DayUiState(
    val date: LocalDate,
    val prompt: DayPrompt? = null,
    val entries: List<Entry> = emptyList(),
    val note: String = "",
    val badgeIds: List<String> = emptyList(),
    val isToday: Boolean = false,
    val originalMissing: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val freezeLine: String? = null,
    val snackbar: String? = null,
    val shareChooser: Intent? = null,
)

/**
 * Loads one date through repositories and handles note autosave / delete.
 */
@HiltViewModel
class DayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val prompts: DayPromptRepository,
    private val entries: EntryRepository,
    gamification: GamificationRepository,
    private val shareExporter: ShareCardExporter,
    clock: Clock,
    zone: ZoneId,
) : ViewModel() {

    val date: LocalDate = savedStateHandle.get<String>("dateIso")?.let(LocalDate::parse)
        ?: LocalDate.now(clock.withZone(zone))

    private val today = LocalDate.now(clock.withZone(zone))
    private var shareJob: Job? = null

    private val _state = MutableStateFlow(DayUiState(date = date, isToday = date == today))
    val state: StateFlow<DayUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prompts.observeDay(date),
                entries.observeEntries(date),
                gamification.observeAchievements(),
            ) { prompt, dayEntries, achievements ->
                Triple(prompt, dayEntries, achievements)
            }.collect { (prompt, dayEntries, achievements) ->
                _state.update {
                    it.copy(
                        prompt = prompt,
                        entries = dayEntries,
                        note = dayEntries.lastOrNull()?.note.orEmpty(),
                        badgeIds = achievements.filter { a -> a.unlockedOnDate == date }.map { a -> a.id },
                        isToday = date == today,
                        originalMissing = dayEntries.any { entry -> isOriginalMissing(entry) },
                        freezeLine = freezeLine(prompt),
                    )
                }
            }
        }
    }

    fun updateNote(note: String) {
        _state.update { it.copy(note = note) }
        viewModelScope.launch {
            val last = entries.observeEntries(date).first().lastOrNull() ?: return@launch
            entries.upsert(last.copy(note = note.ifBlank { null }))
        }
    }

    fun onDeleteClicked() {
        _state.update { it.copy(showDeleteDialog = true) }
    }

    fun dismissDelete() {
        _state.update { it.copy(showDeleteDialog = false) }
    }

    /**
     * Removes the day's photo. The day stays complete (SPEC §4.5).
     */
    fun confirmDelete(alsoGallery: Boolean) {
        viewModelScope.launch {
            val current = entries.observeEntries(date).first()
            if (alsoGallery) {
                current.forEach { entry ->
                    runCatching {
                        context.contentResolver.delete(entry.mediaUri.toUri(), null, null)
                    }
                }
            }
            entries.delete(date)
            val prompt = prompts.getDay(date)
            if (prompt != null &&
                (prompt.status == DayStatus.COMPLETED || prompt.status == DayStatus.COMPLETED_NO_PHOTO)
            ) {
                prompts.upsert(prompt.copy(status = DayStatus.COMPLETED_NO_PHOTO))
            }
            _state.update { it.copy(showDeleteDialog = false, snackbar = "Deleted.") }
        }
    }

    fun consumeSnackbar() {
        _state.update { it.copy(snackbar = null) }
    }

    fun share(darkTheme: Boolean) {
        if (shareJob?.isActive == true) return
        shareJob = viewModelScope.launch {
            val current = _state.value
            val prompt = current.prompt ?: return@launch
            shareExporter.export(prompt, current.entries, darkTheme)
                .onSuccess { intent -> _state.update { it.copy(shareChooser = intent) } }
                .onFailure { _state.update { it.copy(snackbar = ShareCopy.FAILED) } }
        }
    }

    fun consumeShareChooser() {
        _state.update { it.copy(shareChooser = null) }
    }

    fun onShareLaunchFailed() {
        _state.update { it.copy(shareChooser = null, snackbar = ShareCopy.FAILED) }
    }

    private fun isOriginalMissing(entry: Entry): Boolean {
        return runCatching {
            context.contentResolver.openInputStream(entry.mediaUri.toUri()).use { it == null }
        }.getOrDefault(true)
    }

    private fun freezeLine(prompt: DayPrompt?): String? {
        if (prompt == null || !prompt.frozen) return null
        return "A freeze kept your streak on ${prompt.date.format(FREEZE_DATE)}."
    }

    companion object {
        private val FREEZE_DATE: DateTimeFormatter =
            DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH)
    }
}
