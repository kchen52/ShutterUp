package app.shutterup.ui.badges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shutterup.domain.model.Achievement
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.repository.GamificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class BadgeCellUi(
    val id: String,
    val unlocked: Boolean,
    val caption: String,
    val name: String,
    val description: String,
    val unlockedOnLabel: String?,
)

data class BadgeSectionUi(
    val title: String,
    val badges: List<BadgeCellUi>,
)

data class BadgesUiState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val sections: List<BadgeSectionUi> = emptyList(),
    val selected: BadgeCellUi? = null,
)

/**
 * Sectioned badge grid (DESIGN §4.7): Streaks → Count → Explorer → Time of day → Special.
 */
@HiltViewModel
class BadgesViewModel @Inject constructor(
    gamification: GamificationRepository,
) : ViewModel() {
    private val selectedId = MutableStateFlow<String?>(null)

    val state: StateFlow<BadgesUiState> = combine(
        gamification.observeAchievements(),
        gamification.observeStreak(),
        selectedId,
    ) { achievements, streak, selected ->
        badgesUiState(achievements, streak, selected)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        badgesUiState(emptyList(), StreakState(0, 0, 0, null), null),
    )

    fun selectBadge(id: String) {
        selectedId.value = id
    }

    fun dismissDetail() {
        selectedId.value = null
    }
}

private val UnlockDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

internal fun badgeUnlockDateLabel(date: LocalDate): String =
    date.format(UnlockDateFormatter)

internal fun badgesUiState(
    achievements: List<Achievement>,
    streak: StreakState,
    selectedId: String?,
): BadgesUiState {
    val unlocked = achievements.associateBy { it.id }
    val sections = BadgeSections.map { section ->
        BadgeSectionUi(
            title = section.title,
            badges = section.ids.map { id ->
                val achievement = unlocked[id]
                val isUnlocked = achievement != null
                BadgeCellUi(
                    id = id,
                    unlocked = isUnlocked,
                    caption = badgeGridCaption(id, isUnlocked),
                    name = badgeDisplayName(id),
                    description = badgeDescription(id),
                    unlockedOnLabel = achievement?.unlockedOnDate?.let { badgeUnlockDateLabel(it) },
                )
            },
        )
    }
    val cells = sections.flatMap { it.badges }
    return BadgesUiState(
        currentStreak = streak.current,
        longestStreak = streak.longest,
        sections = sections,
        selected = cells.firstOrNull { it.id == selectedId },
    )
}
