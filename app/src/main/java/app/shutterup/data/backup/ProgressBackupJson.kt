package app.shutterup.data.backup

import app.shutterup.data.local.AchievementEntity
import app.shutterup.data.local.DayPromptEntity
import app.shutterup.data.local.EntryEntity
import app.shutterup.data.local.LibraryUsageEntity
import app.shutterup.data.local.MonthlyIssueEntity
import app.shutterup.data.local.SeriesEntity
import app.shutterup.data.local.StreakStateEntity
import app.shutterup.data.local.SupersededPromptEntity
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.MediaKind
import app.shutterup.domain.model.PromptSourceRef
import java.time.Instant
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

/** Encode / decode [ProgressBackup] as `progress.json`. */
object ProgressBackupJson {
    fun encode(backup: ProgressBackup): String {
        val root = JSONObject()
        root.put("format", backup.format)
        root.put("createdAt", backup.createdAt.toString())
        root.put("prefs", encodePrefs(backup.prefs))
        root.put("prompts", JSONArray(backup.prompts.map(::encodePrompt)))
        root.put("superseded", JSONArray(backup.superseded.map(::encodeSuperseded)))
        root.put("entries", JSONArray(backup.entries.map(::encodeEntry)))
        root.put("achievements", JSONArray(backup.achievements.map(::encodeAchievement)))
        backup.streak?.let { root.put("streak", encodeStreak(it)) }
        root.put("libraryUsage", JSONArray(backup.libraryUsage.map(::encodeLibraryUsage)))
        root.put("series", JSONArray(backup.series.map(::encodeSeries)))
        root.put("monthlyIssues", JSONArray(backup.monthlyIssues.map(::encodeMonthlyIssue)))
        return root.toString()
    }

    fun decode(raw: String): ProgressBackup {
        val root = try {
            JSONObject(raw)
        } catch (e: Exception) {
            throw ProgressBackupException("That file isn't a ShutterUp backup.", e)
        }
        val format = root.optInt("format", -1)
        if (format != ProgressBackupFormat.VERSION) {
            throw ProgressBackupException("That file isn't a ShutterUp backup.")
        }
        val createdAt = root.stringOrNull("createdAt")?.let {
            runCatching { Instant.parse(it) }.getOrNull()
        } ?: throw ProgressBackupException("That file isn't a ShutterUp backup.")
        return ProgressBackup(
            format = format,
            createdAt = createdAt,
            prefs = decodePrefs(root.optJSONObject("prefs") ?: JSONObject()),
            prompts = root.optJSONArray("prompts").mapObjects(::decodePrompt),
            superseded = root.optJSONArray("superseded").mapObjects(::decodeSuperseded),
            entries = root.optJSONArray("entries").mapObjects(::decodeEntry),
            achievements = root.optJSONArray("achievements").mapObjects(::decodeAchievement),
            streak = root.optJSONObject("streak")?.let(::decodeStreak),
            libraryUsage = root.optJSONArray("libraryUsage").mapObjects(::decodeLibraryUsage),
            series = root.optJSONArray("series").mapObjects(::decodeSeries),
            monthlyIssues = root.optJSONArray("monthlyIssues").mapObjects(::decodeMonthlyIssue),
        )
    }

    private fun encodePrefs(prefs: PrefsBackup): JSONObject = JSONObject().apply {
        put("notifyHour", prefs.notifyHour)
        put("notifyMinute", prefs.notifyMinute)
        put("preciseTiming", prefs.preciseTiming)
        putOpt("themeFocus", prefs.themeFocus)
        put("seriesEnabled", prefs.seriesEnabled)
        put("paused", prefs.paused)
        put("onboardingComplete", prefs.onboardingComplete)
        put("debugUseFakeAi", prefs.debugUseFakeAi)
        putOpt("lastNotifiedDate", prefs.lastNotifiedDate)
        putOpt("coarseCityId", prefs.coarseCityId)
    }

    private fun decodePrefs(obj: JSONObject): PrefsBackup = PrefsBackup(
        notifyHour = obj.optInt("notifyHour", 9),
        notifyMinute = obj.optInt("notifyMinute", 0),
        preciseTiming = obj.optBoolean("preciseTiming", false),
        themeFocus = obj.stringOrNull("themeFocus"),
        seriesEnabled = obj.optBoolean("seriesEnabled", false),
        paused = obj.optBoolean("paused", false),
        onboardingComplete = obj.optBoolean("onboardingComplete", false),
        debugUseFakeAi = obj.optBoolean("debugUseFakeAi", false),
        lastNotifiedDate = obj.stringOrNull("lastNotifiedDate"),
        coarseCityId = obj.stringOrNull("coarseCityId"),
    )

    private fun encodePrompt(prompt: DayPromptEntity): JSONObject = JSONObject().apply {
        put("date", prompt.date.toString())
        put("title", prompt.title)
        put("oneLiner", prompt.oneLiner)
        put("details", prompt.details)
        putOpt("constraint", prompt.constraint)
        put("theme", prompt.theme)
        put("tips", JSONArray(prompt.tips))
        put("source", prompt.source.name)
        putOpt("libraryId", prompt.libraryId)
        putOpt("modelName", prompt.modelName)
        put("generatedAt", prompt.generatedAt.toString())
        put("status", prompt.status.name)
        put("frozen", prompt.frozen)
        put("rerollUsed", prompt.rerollUsed)
        prompt.seriesId?.let { put("seriesId", it) }
        prompt.seriesIndex?.let { put("seriesIndex", it) }
        putOpt("repeatsDate", prompt.repeatsDate?.toString())
    }

    private fun decodePrompt(obj: JSONObject): DayPromptEntity = DayPromptEntity(
        date = obj.localDate("date"),
        title = obj.getString("title"),
        oneLiner = obj.getString("oneLiner"),
        details = obj.getString("details"),
        constraint = obj.stringOrNull("constraint"),
        theme = obj.getString("theme"),
        tips = obj.optJSONArray("tips").toStringList(),
        source = obj.enumValue("source", PromptSourceRef.LIBRARY),
        libraryId = obj.stringOrNull("libraryId"),
        modelName = obj.stringOrNull("modelName"),
        generatedAt = obj.instant("generatedAt"),
        status = obj.enumValue("status", DayStatus.PENDING),
        frozen = obj.optBoolean("frozen", false),
        rerollUsed = obj.optBoolean("rerollUsed", false),
        seriesId = obj.longOrNull("seriesId"),
        seriesIndex = obj.intOrNull("seriesIndex"),
        repeatsDate = obj.stringOrNull("repeatsDate")?.let(LocalDate::parse),
    )

    private fun encodeSuperseded(row: SupersededPromptEntity): JSONObject = JSONObject().apply {
        put("id", row.id)
        put("date", row.date.toString())
        put("title", row.title)
        put("theme", row.theme)
        put("generatedAt", row.generatedAt.toString())
    }

    private fun decodeSuperseded(obj: JSONObject): SupersededPromptEntity = SupersededPromptEntity(
        id = obj.optLong("id", 0L),
        date = obj.localDate("date"),
        title = obj.getString("title"),
        theme = obj.getString("theme"),
        generatedAt = obj.instant("generatedAt"),
    )

    private fun encodeEntry(row: EntryBackup): JSONObject = JSONObject().apply {
        val entry = row.entity
        put("id", entry.id)
        put("date", entry.date.toString())
        put("mediaUri", entry.mediaUri)
        put("thumbPath", entry.thumbPath)
        put("capturedAt", entry.capturedAt.toString())
        put("width", entry.width)
        put("height", entry.height)
        putOpt("note", entry.note)
        put("importedFromGallery", entry.importedFromGallery)
        put("createdAt", entry.createdAt.toString())
        put("mediaKind", entry.mediaKind.name)
        putOpt("displayName", row.displayName)
        putOpt("thumbZip", row.thumbZip)
        putOpt("photoZip", row.photoZip)
    }

    private fun decodeEntry(obj: JSONObject): EntryBackup = EntryBackup(
        entity = EntryEntity(
            id = obj.optLong("id", 0L),
            date = obj.localDate("date"),
            mediaUri = obj.getString("mediaUri"),
            thumbPath = obj.optString("thumbPath", ""),
            capturedAt = obj.instant("capturedAt"),
            width = obj.optInt("width", 0),
            height = obj.optInt("height", 0),
            note = obj.stringOrNull("note"),
            importedFromGallery = obj.optBoolean("importedFromGallery", false),
            createdAt = obj.instant("createdAt"),
            mediaKind = obj.enumValue("mediaKind", MediaKind.PHOTO),
        ),
        displayName = obj.stringOrNull("displayName"),
        thumbZip = obj.stringOrNull("thumbZip"),
        photoZip = obj.stringOrNull("photoZip"),
    )

    private fun encodeAchievement(row: AchievementEntity): JSONObject = JSONObject().apply {
        put("id", row.id)
        put("unlockedAt", row.unlockedAt.toString())
        put("unlockedOnDate", row.unlockedOnDate.toString())
    }

    private fun decodeAchievement(obj: JSONObject): AchievementEntity = AchievementEntity(
        id = obj.getString("id"),
        unlockedAt = obj.instant("unlockedAt"),
        unlockedOnDate = obj.localDate("unlockedOnDate"),
    )

    private fun encodeStreak(row: StreakStateEntity): JSONObject = JSONObject().apply {
        put("current", row.current)
        put("longest", row.longest)
        put("freezes", row.freezes)
        putOpt("lastProcessedDate", row.lastProcessedDate?.toString())
    }

    private fun decodeStreak(obj: JSONObject): StreakStateEntity = StreakStateEntity(
        id = 0,
        current = obj.optInt("current", 0),
        longest = obj.optInt("longest", 0),
        freezes = obj.optInt("freezes", 0),
        lastProcessedDate = obj.stringOrNull("lastProcessedDate")?.let(LocalDate::parse),
    )

    private fun encodeLibraryUsage(row: LibraryUsageEntity): JSONObject = JSONObject().apply {
        put("libraryId", row.libraryId)
        put("usedOnDate", row.usedOnDate.toString())
    }

    private fun decodeLibraryUsage(obj: JSONObject): LibraryUsageEntity = LibraryUsageEntity(
        libraryId = obj.getString("libraryId"),
        usedOnDate = obj.localDate("usedOnDate"),
    )

    private fun encodeSeries(row: SeriesEntity): JSONObject = JSONObject().apply {
        put("id", row.id)
        put("title", row.title)
        put("startDate", row.startDate.toString())
        put("endDate", row.endDate.toString())
        put("theme", row.theme)
        put("source", row.source.name)
    }

    private fun decodeSeries(obj: JSONObject): SeriesEntity = SeriesEntity(
        id = obj.optLong("id", 0L),
        title = obj.getString("title"),
        startDate = obj.localDate("startDate"),
        endDate = obj.localDate("endDate"),
        theme = obj.getString("theme"),
        source = obj.enumValue("source", PromptSourceRef.LIBRARY),
    )

    private fun encodeMonthlyIssue(row: MonthlyIssueEntity): JSONObject = JSONObject().apply {
        put("id", row.id)
        put("yearMonth", row.yearMonth)
        put("startDate", row.startDate.toString())
        put("endDate", row.endDate.toString())
        put("completedDayCount", row.completedDayCount)
        put("headline", row.headline)
        put("body", row.body)
        put("dominantTheme", row.dominantTheme)
        put("loudestThemes", JSONArray(row.loudestThemes))
        put("source", row.source.name)
        put("generatedAt", row.generatedAt.toString())
        put("dismissedFromFeed", row.dismissedFromFeed)
        putOpt("modelName", row.modelName)
    }

    private fun decodeMonthlyIssue(obj: JSONObject): MonthlyIssueEntity = MonthlyIssueEntity(
        id = obj.optLong("id", 0L),
        yearMonth = obj.getString("yearMonth"),
        startDate = obj.localDate("startDate"),
        endDate = obj.localDate("endDate"),
        completedDayCount = obj.optInt("completedDayCount", 0),
        headline = obj.getString("headline"),
        body = obj.getString("body"),
        dominantTheme = obj.getString("dominantTheme"),
        loudestThemes = obj.optJSONArray("loudestThemes").toStringList(),
        source = obj.enumValue("source", PromptSourceRef.LIBRARY),
        generatedAt = obj.instant("generatedAt"),
        dismissedFromFeed = obj.optBoolean("dismissedFromFeed", false),
        modelName = obj.stringOrNull("modelName"),
    )
}

private fun JSONObject.stringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val value = opt(key) ?: return null
    if (value === JSONObject.NULL) return null
    val text = value.toString()
    return text.takeIf { it.isNotEmpty() && it != "null" }
}

private fun JSONObject.longOrNull(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    return optLong(key)
}

private fun JSONObject.intOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}

private fun JSONObject.localDate(key: String): LocalDate =
    stringOrNull(key)?.let(LocalDate::parse)
        ?: throw ProgressBackupException("That file isn't a ShutterUp backup.")

private fun JSONObject.instant(key: String): Instant =
    stringOrNull(key)?.let(Instant::parse)
        ?: throw ProgressBackupException("That file isn't a ShutterUp backup.")

private inline fun <reified T : Enum<T>> JSONObject.enumValue(key: String, fallback: T): T {
    val raw = stringOrNull(key) ?: return fallback
    return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).map { getString(it) }
}

private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return (0 until length()).map { index -> transform(getJSONObject(index)) }
}
