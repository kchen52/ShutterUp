package app.shutterup.data.repository

import app.shutterup.data.local.AchievementEntity
import app.shutterup.data.local.DayPromptEntity
import app.shutterup.data.local.EntryEntity
import app.shutterup.data.local.LibraryUsageEntity
import app.shutterup.data.local.StreakStateEntity
import app.shutterup.data.local.SupersededPromptEntity
import app.shutterup.domain.model.Achievement
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.LibraryUsage
import app.shutterup.domain.model.StreakState
import app.shutterup.domain.model.SupersededPrompt

internal fun DayPromptEntity.toDomain(): DayPrompt = DayPrompt(
    date = date,
    title = title,
    oneLiner = oneLiner,
    details = details,
    constraint = constraint,
    theme = theme,
    tips = tips,
    source = source,
    libraryId = libraryId,
    modelName = modelName,
    generatedAt = generatedAt,
    status = status,
    frozen = frozen,
    rerollUsed = rerollUsed,
)

internal fun DayPrompt.toEntity(): DayPromptEntity = DayPromptEntity(
    date = date,
    title = title,
    oneLiner = oneLiner,
    details = details,
    constraint = constraint,
    theme = theme,
    tips = tips,
    source = source,
    libraryId = libraryId,
    modelName = modelName,
    generatedAt = generatedAt,
    status = status,
    frozen = frozen,
    rerollUsed = rerollUsed,
)

internal fun EntryEntity.toDomain(): Entry = Entry(
    date = date,
    mediaUri = mediaUri,
    thumbPath = thumbPath,
    capturedAt = capturedAt,
    width = width,
    height = height,
    note = note,
    importedFromGallery = importedFromGallery,
    createdAt = createdAt,
)

internal fun Entry.toEntity(): EntryEntity = EntryEntity(
    date = date,
    mediaUri = mediaUri,
    thumbPath = thumbPath,
    capturedAt = capturedAt,
    width = width,
    height = height,
    note = note,
    importedFromGallery = importedFromGallery,
    createdAt = createdAt,
)

internal fun AchievementEntity.toDomain(): Achievement = Achievement(
    id = id,
    unlockedAt = unlockedAt,
    unlockedOnDate = unlockedOnDate,
)

internal fun Achievement.toEntity(): AchievementEntity = AchievementEntity(
    id = id,
    unlockedAt = unlockedAt,
    unlockedOnDate = unlockedOnDate,
)

internal fun StreakStateEntity.toDomain(): StreakState = StreakState(
    current = current,
    longest = longest,
    freezes = freezes,
    lastProcessedDate = lastProcessedDate,
)

internal fun StreakState.toEntity(): StreakStateEntity = StreakStateEntity(
    id = 0,
    current = current,
    longest = longest,
    freezes = freezes,
    lastProcessedDate = lastProcessedDate,
)

internal fun SupersededPrompt.toEntity(): SupersededPromptEntity = SupersededPromptEntity(
    id = 0,
    date = date,
    title = title,
    theme = theme,
    generatedAt = generatedAt,
)

internal fun LibraryUsageEntity.toDomain(): LibraryUsage = LibraryUsage(
    libraryId = libraryId,
    usedOnDate = usedOnDate,
)

internal fun LibraryUsage.toEntity(): LibraryUsageEntity = LibraryUsageEntity(
    libraryId = libraryId,
    usedOnDate = usedOnDate,
)
