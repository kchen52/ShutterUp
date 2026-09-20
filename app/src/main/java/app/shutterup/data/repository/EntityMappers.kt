package app.shutterup.data.repository

import app.shutterup.data.local.AchievementEntity
import app.shutterup.data.local.DayPromptEntity
import app.shutterup.data.local.EntryEntity
import app.shutterup.data.local.LibraryUsageEntity
import app.shutterup.data.local.MonthlyIssueEntity
import app.shutterup.data.local.SeriesEntity
import app.shutterup.data.local.StreakStateEntity
import app.shutterup.data.local.SupersededPromptEntity
import app.shutterup.domain.model.Achievement
import app.shutterup.domain.model.DayPrompt
import app.shutterup.domain.model.Entry
import app.shutterup.domain.model.LibraryUsage
import app.shutterup.domain.model.MonthlyIssue
import app.shutterup.domain.model.Series
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
    seriesId = seriesId,
    seriesIndex = seriesIndex,
    repeatsDate = repeatsDate,
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
    seriesId = seriesId,
    seriesIndex = seriesIndex,
    repeatsDate = repeatsDate,
)

internal fun SeriesEntity.toDomain(): Series = Series(
    id = id,
    title = title,
    startDate = startDate,
    endDate = endDate,
    theme = theme,
    source = source,
)

internal fun Series.toEntity(): SeriesEntity = SeriesEntity(
    id = id,
    title = title,
    startDate = startDate,
    endDate = endDate,
    theme = theme,
    source = source,
)

internal fun MonthlyIssueEntity.toDomain(): MonthlyIssue = MonthlyIssue(
    id = id,
    yearMonth = yearMonth,
    startDate = startDate,
    endDate = endDate,
    completedDayCount = completedDayCount,
    headline = headline,
    body = body,
    dominantTheme = dominantTheme,
    loudestThemes = loudestThemes,
    source = source,
    generatedAt = generatedAt,
    dismissedFromFeed = dismissedFromFeed,
    modelName = modelName,
)

internal fun MonthlyIssue.toEntity(): MonthlyIssueEntity = MonthlyIssueEntity(
    id = id,
    yearMonth = yearMonth,
    startDate = startDate,
    endDate = endDate,
    completedDayCount = completedDayCount,
    headline = headline,
    body = body,
    dominantTheme = dominantTheme,
    loudestThemes = loudestThemes,
    source = source,
    generatedAt = generatedAt,
    dismissedFromFeed = dismissedFromFeed,
    modelName = modelName,
)

internal fun EntryEntity.toDomain(): Entry = Entry(
    id = id,
    date = date,
    mediaUri = mediaUri,
    thumbPath = thumbPath,
    capturedAt = capturedAt,
    width = width,
    height = height,
    note = note,
    importedFromGallery = importedFromGallery,
    createdAt = createdAt,
    mediaKind = mediaKind,
)

internal fun Entry.toEntity(): EntryEntity = EntryEntity(
    id = id,
    date = date,
    mediaUri = mediaUri,
    thumbPath = thumbPath,
    capturedAt = capturedAt,
    width = width,
    height = height,
    note = note,
    importedFromGallery = importedFromGallery,
    createdAt = createdAt,
    mediaKind = mediaKind,
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
