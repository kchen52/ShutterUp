package app.shutterup.data.local

import androidx.room.TypeConverter
import app.shutterup.domain.model.DayStatus
import app.shutterup.domain.model.MediaKind
import app.shutterup.domain.model.PromptSourceRef
import java.time.Instant
import java.time.LocalDate

class ShutterUpConverters {
    @TypeConverter
    fun localDateToEpochDay(value: LocalDate): Long = value.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long): LocalDate = LocalDate.ofEpochDay(value)

    @TypeConverter
    fun nullableLocalDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToNullableLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun instantToEpochMilli(value: Instant): Long = value.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(value: Long): Instant = Instant.ofEpochMilli(value)

    @TypeConverter
    fun dayStatusToName(value: DayStatus): String = value.name

    @TypeConverter
    fun nameToDayStatus(value: String): DayStatus = DayStatus.valueOf(value)

    @TypeConverter
    fun promptSourceRefToName(value: PromptSourceRef): String = value.name

    @TypeConverter
    fun nameToPromptSourceRef(value: String): PromptSourceRef = PromptSourceRef.valueOf(value)

    @TypeConverter
    fun stringListToStorage(value: List<String>): String = value.joinToString("\u001F")

    @TypeConverter
    fun storageToStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split("\u001F")

    @TypeConverter
    fun mediaKindToName(value: MediaKind): String = value.name

    @TypeConverter
    fun nameToMediaKind(value: String): MediaKind = MediaKind.valueOf(value)
}
