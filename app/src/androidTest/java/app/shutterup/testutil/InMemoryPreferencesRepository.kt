package app.shutterup.testutil

import app.shutterup.domain.repository.PreferencesRepository
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class InMemoryPreferencesRepository @Inject constructor() : PreferencesRepository {
    private val notifyTime = MutableStateFlow(LocalTime.of(9, 0))
    private val preciseTiming = MutableStateFlow(false)
    private val themeFocus = MutableStateFlow<String?>(null)
    private val paused = MutableStateFlow(false)
    private val onboardingComplete = MutableStateFlow(false)
    private val debugUseFakeAi = MutableStateFlow(false)
    private val lastNotifiedDate = MutableStateFlow<LocalDate?>(null)

    override fun observeNotifyTime(): Flow<LocalTime> = notifyTime.asStateFlow()
    override suspend fun setNotifyTime(time: LocalTime) {
        notifyTime.value = time
    }

    override fun observePreciseTiming(): Flow<Boolean> = preciseTiming.asStateFlow()
    override suspend fun setPreciseTiming(enabled: Boolean) {
        preciseTiming.value = enabled
    }

    override fun observeThemeFocus(): Flow<String?> = themeFocus.asStateFlow()
    override suspend fun setThemeFocus(focus: String?) {
        themeFocus.value = focus?.ifBlank { null }
    }

    override fun observePaused(): Flow<Boolean> = paused.asStateFlow()
    override suspend fun setPaused(paused: Boolean) {
        this.paused.value = paused
    }

    override fun observeOnboardingComplete(): Flow<Boolean> = onboardingComplete.asStateFlow()
    override suspend fun setOnboardingComplete(complete: Boolean) {
        onboardingComplete.value = complete
    }

    override fun observeDebugUseFakeAi(): Flow<Boolean> = debugUseFakeAi.asStateFlow()
    override suspend fun setDebugUseFakeAi(useFake: Boolean) {
        debugUseFakeAi.value = useFake
    }

    override fun observeLastNotifiedDate(): Flow<LocalDate?> = lastNotifiedDate.asStateFlow()
    override suspend fun setLastNotifiedDate(date: LocalDate?) {
        lastNotifiedDate.value = date
    }

    private val seriesEnabled = MutableStateFlow(false)

    override fun observeSeriesEnabled(): Flow<Boolean> = seriesEnabled.asStateFlow()
    override suspend fun setSeriesEnabled(enabled: Boolean) {
        seriesEnabled.value = enabled
    }
}
