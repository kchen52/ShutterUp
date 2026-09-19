package app.shutterup.domain.repository

import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

/** DataStore-backed in the persistence milestone (SPEC §10). */
interface PreferencesRepository {
    fun observeNotifyTime(): Flow<LocalTime>
    suspend fun setNotifyTime(time: LocalTime)
    fun observePreciseTiming(): Flow<Boolean>
    suspend fun setPreciseTiming(enabled: Boolean)
    fun observeThemeFocus(): Flow<String?>
    suspend fun setThemeFocus(focus: String?)
    fun observePaused(): Flow<Boolean>
    suspend fun setPaused(paused: Boolean)
    fun observeOnboardingComplete(): Flow<Boolean>
    suspend fun setOnboardingComplete(complete: Boolean)
    fun observeDebugUseFakeAi(): Flow<Boolean>
    suspend fun setDebugUseFakeAi(useFake: Boolean)
    fun observeLastNotifiedDate(): Flow<LocalDate?>
    suspend fun setLastNotifiedDate(date: LocalDate?)
}
