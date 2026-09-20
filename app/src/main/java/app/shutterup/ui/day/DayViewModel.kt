package app.shutterup.ui.day

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shutterup.capture.DeleteDayPhotoUseCase
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.Entry
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.take.SecondTakeEligibility
import app.shutterup.domain.take.StartSecondTakeResult
import app.shutterup.domain.take.StartSecondTakeUseCase
import app.shutterup.domain.take.TakeChain
import app.shutterup.domain.take.TakeInterval
import app.shutterup.domain.take.diptychCrop
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    val canSecondTake: Boolean = false,
    val showSecondTakeDialog: Boolean = false,
    val secondTakeConfirmBody: String? = null,
    val laterTakesLine: String? = null,
    val laterTakeDateIso: String? = null,
    val diptych: DiptychUi? = null,
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
    private val deleteDayPhoto: DeleteDayPhotoUseCase,
    gamification: GamificationRepository,
    private val shareExporter: ShareCardExporter,
    private val startSecondTake: StartSecondTakeUseCase,
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
            @OptIn(ExperimentalCoroutinesApi::class)
            prompts.observeDay(date).flatMapLatest { prompt ->
                val original = prompt?.repeatsDate ?: date
                combine(
                    flowOf(prompt),
                    prompts.observeDay(original),
                    prompts.observeRepeatsOf(original),
                    entries.observeEntries(date),
                    gamification.observeAchievements(),
                ) { viewed, orig, repeats, dayEntries, achievements ->
                    LoadedDay(viewed, orig, repeats, dayEntries, achievements)
                }.flatMapLatest { loaded ->
                    val chain = loaded.chain(date)
                    val previous = chain.previous
                    val previousFlow = if (previous == null) {
                        flowOf(emptyList())
                    } else {
                        entries.observeEntries(previous)
                    }
                    previousFlow.map { prevEntries ->
                        PackedDay(loaded, prevEntries, chain)
                    }
                }
            }.collect { packed ->
                val prompt = packed.loaded.viewed
                val dayEntries = packed.loaded.dayEntries
                val chain = packed.chain
                _state.update {
                    it.copy(
                        prompt = prompt,
                        entries = dayEntries,
                        note = dayEntries.lastOrNull()?.note.orEmpty(),
                        badgeIds = packed.loaded.achievements
                            .filter { a -> a.unlockedOnDate == date }
                            .map { a -> a.id },
                        isToday = date == today,
                        originalMissing = dayEntries.any { entry -> isOriginalMissing(entry) },
                        freezeLine = freezeLine(prompt),
                        canSecondTake = SecondTakeEligibility.isRepeatable(
                            date = date,
                            today = today,
                            status = prompt?.status,
                            hasPhotograph = dayEntries.isNotEmpty(),
                        ),
                        laterTakesLine = if (!chain.isRepeat) {
                            TakeInterval.laterTakesLine(chain.laterFromOriginal)
                        } else {
                            null
                        },
                        laterTakeDateIso = chain.laterFromOriginal.firstOrNull()?.toString(),
                        diptych = diptychUi(chain, dayEntries, packed.prevEntries),
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

    fun onShootAgainClicked() {
        viewModelScope.launch {
            val preview = startSecondTake.preview(date) ?: return@launch
            _state.update {
                it.copy(
                    showSecondTakeDialog = true,
                    secondTakeConfirmBody = preview.confirmationBody,
                )
            }
        }
    }

    fun dismissSecondTake() {
        _state.update { it.copy(showSecondTakeDialog = false) }
    }

    fun confirmSecondTake() {
        viewModelScope.launch {
            when (val result = startSecondTake(date)) {
                is StartSecondTakeResult.Placed -> {
                    _state.update { it.copy(showSecondTakeDialog = false) }
                }
                StartSecondTakeResult.TargetTaken -> {
                    _state.update {
                        it.copy(
                            showSecondTakeDialog = false,
                            snackbar = "Tomorrow is already spoken for.",
                        )
                    }
                }
                StartSecondTakeResult.NotRepeatable -> {
                    _state.update { it.copy(showSecondTakeDialog = false) }
                }
            }
        }
    }

    /**
     * Removes the day's photo. The day stays complete (SPEC §4.5).
     */
    fun confirmDelete(alsoGallery: Boolean) {
        viewModelScope.launch {
            deleteDayPhoto(date, alsoFromGallery = alsoGallery)
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

    private fun diptychUi(
        chain: TakeChain,
        currentEntries: List<Entry>,
        previousEntries: List<Entry>,
    ): DiptychUi? {
        if (!chain.showDiptych) return null
        val previousDate = chain.previous ?: return null
        val firstEntry = previousEntries.lastOrNull()
        val secondEntry = currentEntries.lastOrNull()
        val crop = diptychCrop(
            firstEntry?.width ?: 0,
            firstEntry?.height ?: 0,
            secondEntry?.width ?: 0,
            secondEntry?.height ?: 0,
        )
        return DiptychUi(
            first = DiptychFrame(
                date = previousDate,
                kicker = TakeInterval.dateKicker(previousDate),
                entry = firstEntry,
                originalMissing = firstEntry?.let(::isOriginalMissing) ?: true,
            ),
            second = DiptychFrame(
                date = chain.viewed,
                kicker = TakeInterval.phrase(previousDate, chain.viewed),
                entry = secondEntry,
                originalMissing = secondEntry?.let(::isOriginalMissing) ?: true,
            ),
            crop = crop,
            rest = chain.rest,
        )
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

private data class LoadedDay(
    val viewed: DayPrompt?,
    val original: DayPrompt?,
    val repeats: List<DayPrompt>,
    val dayEntries: List<Entry>,
    val achievements: List<app.shutterup.domain.model.Achievement>,
) {
    fun chain(viewedDate: LocalDate): TakeChain {
        val days = buildList {
            original?.let(::add)
            addAll(repeats)
            viewed?.let(::add)
        }.distinctBy { it.date }
        return TakeChain.build(days, viewedDate)
    }
}

private data class PackedDay(
    val loaded: LoadedDay,
    val prevEntries: List<Entry>,
    val chain: TakeChain,
)
