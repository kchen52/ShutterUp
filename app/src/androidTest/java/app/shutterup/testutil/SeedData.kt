package app.shutterup.testutil

import app.shutterup.di.TestTimeModule
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.PromptSourceRef
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import java.time.Instant
import java.time.LocalDate

object SeedData {
    const val TODAY_TITLE = "Find the sky in a puddle"
    const val TODAY_ONE_LINER =
        "Turn the world upside down using any reflective surface you pass today."
    const val YESTERDAY_TITLE = "Window light after lunch"

    fun todayPrompt(
        date: LocalDate = TestTimeModule.TODAY,
        status: DayStatus = DayStatus.PENDING,
        rerollUsed: Boolean = false,
        title: String = TODAY_TITLE,
        oneLiner: String = TODAY_ONE_LINER,
        theme: String = "Reflections",
    ): DayPrompt = DayPrompt(
        date = date,
        title = title,
        oneLiner = oneLiner,
        details = "Look down, not up. Puddles, car roofs, and shop windows all hold a second sky.",
        constraint = "Don't rotate the photo afterwards.",
        theme = theme,
        tips = listOf("Tap to focus on the reflection.", "Try it after rain."),
        source = PromptSourceRef.ON_DEVICE_AI,
        libraryId = null,
        modelName = "test",
        generatedAt = Instant.parse("2026-09-19T08:00:00Z"),
        status = status,
        frozen = false,
        rerollUsed = rerollUsed,
    )

    suspend fun onboarded(
        preferences: PreferencesRepository,
        prompts: DayPromptRepository,
        gamification: GamificationRepository,
        streak: StreakState = StreakState(
            current = 3,
            longest = 5,
            freezes = 0,
            lastProcessedDate = TestTimeModule.TODAY.minusDays(1),
        ),
        extraDays: List<DayPrompt> = emptyList(),
    ) {
        preferences.setOnboardingComplete(true)
        prompts.upsert(todayPrompt())
        extraDays.forEach { prompts.upsert(it) }
        gamification.updateStreak(streak)
    }

    suspend fun monthWithHistory(
        prompts: DayPromptRepository,
        gamification: GamificationRepository,
    ) {
        val yesterday = TestTimeModule.TODAY.minusDays(1)
        prompts.upsert(
            todayPrompt(
                date = yesterday,
                status = DayStatus.COMPLETED,
                title = YESTERDAY_TITLE,
                oneLiner = "Use the brightest indoor window as your only light source.",
                theme = "Quiet hours",
            ),
        )
        prompts.upsert(todayPrompt())
        gamification.updateStreak(
            StreakState(
                current = 1,
                longest = 4,
                freezes = 1,
                lastProcessedDate = yesterday,
            ),
        )
    }
}
