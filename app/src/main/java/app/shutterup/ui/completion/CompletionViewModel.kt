package app.shutterup.ui.completion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.SeriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CompletionUiState(
    val date: LocalDate,
    val prompt: DayPrompt? = null,
    val entries: List<Entry> = emptyList(),
    val streak: StreakState = StreakState(0, 0, 0, null),
    val note: String = "",
    val newBadgeIds: List<String> = emptyList(),
    val freezeEarned: Boolean = false,
    val previousStreak: Int = 0,
    val firstEver: Boolean = false,
    val seriesTitle: String? = null,
)

/**
 * Completion: note, streak, newly unlocked badges, Done / Retake.
 */
@HiltViewModel
class CompletionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    prompts: DayPromptRepository,
    private val entries: EntryRepository,
    gamification: GamificationRepository,
    seriesRepo: SeriesRepository,
) : ViewModel() {
    val date: LocalDate = LocalDate.parse(checkNotNull(savedStateHandle.get<String>("dateIso")))
    private val newBadges = savedStateHandle.get<String>("newBadges").orEmpty()
        .split(',').map { it.trim() }.filter { it.isNotEmpty() }
    private val freezeEarned = savedStateHandle.get<Boolean>("freezeEarned") ?: false
    private val previousStreak = savedStateHandle.get<Int>("previousStreak") ?: 0

    val state: StateFlow<CompletionUiState> = combine(
        prompts.observeDay(date),
        entries.observeEntries(date),
        gamification.observeStreak(),
        gamification.observeAchievements(),
        seriesRepo.observeCovering(date),
    ) { prompt, dayEntries, streak, achievements, series ->
        CompletionUiState(
            date = date,
            prompt = prompt,
            entries = dayEntries,
            streak = streak,
            note = dayEntries.lastOrNull()?.note.orEmpty(),
            newBadgeIds = newBadges.ifEmpty {
                achievements.filter { it.unlockedOnDate == date }.map { it.id }
            },
            freezeEarned = freezeEarned,
            previousStreak = previousStreak,
            firstEver = achievements.any { it.id == "first_light" && it.unlockedOnDate == date },
            seriesTitle = series?.title,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CompletionUiState(date = date, newBadgeIds = newBadges, freezeEarned = freezeEarned, previousStreak = previousStreak),
    )

    fun updateNote(note: String) {
        viewModelScope.launch {
            val last = entries.observeEntries(date).first().lastOrNull() ?: return@launch
            entries.upsert(last.copy(note = note.ifBlank { null }))
        }
    }
}
