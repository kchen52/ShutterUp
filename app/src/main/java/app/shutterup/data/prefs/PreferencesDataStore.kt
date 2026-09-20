package app.shutterup.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.shutterup.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PreferencesDataStore(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        @Named("appScope") scope: CoroutineScope,
    ) : this(
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { context.filesDir.resolve(PREFS_FILE) },
        ),
    )

    override fun observeNotifyTime(): Flow<LocalTime> = dataStore.data.map { prefs ->
        LocalTime.of(
            prefs[KEY_NOTIFY_HOUR] ?: DEFAULT_NOTIFY_HOUR,
            prefs[KEY_NOTIFY_MINUTE] ?: DEFAULT_NOTIFY_MINUTE,
        )
    }

    override suspend fun setNotifyTime(time: LocalTime) {
        dataStore.edit { prefs ->
            prefs[KEY_NOTIFY_HOUR] = time.hour
            prefs[KEY_NOTIFY_MINUTE] = time.minute
        }
    }

    override fun observePreciseTiming(): Flow<Boolean> =
        dataStore.data.map { it[KEY_PRECISE_TIMING] ?: false }

    override suspend fun setPreciseTiming(enabled: Boolean) {
        dataStore.edit { it[KEY_PRECISE_TIMING] = enabled }
    }

    override fun observeThemeFocus(): Flow<String?> =
        dataStore.data.map { it[KEY_THEME_FOCUS] }

    override suspend fun setThemeFocus(focus: String?) {
        dataStore.edit { prefs ->
            if (focus.isNullOrEmpty()) {
                prefs.remove(KEY_THEME_FOCUS)
            } else {
                prefs[KEY_THEME_FOCUS] = focus
            }
        }
    }

    override fun observeSeriesEnabled(): Flow<Boolean> =
        dataStore.data.map { it[KEY_SERIES_ENABLED] ?: false }

    override suspend fun setSeriesEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SERIES_ENABLED] = enabled }
    }

    override fun observePaused(): Flow<Boolean> =
        dataStore.data.map { it[KEY_PAUSED] ?: false }

    override suspend fun setPaused(paused: Boolean) {
        dataStore.edit { it[KEY_PAUSED] = paused }
    }

    override fun observeOnboardingComplete(): Flow<Boolean> =
        dataStore.data.map { it[KEY_ONBOARDING_COMPLETE] ?: false }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETE] = complete }
    }

    override fun observeDebugUseFakeAi(): Flow<Boolean> =
        dataStore.data.map { it[KEY_DEBUG_FAKE_AI] ?: false }

    override suspend fun setDebugUseFakeAi(useFake: Boolean) {
        dataStore.edit { it[KEY_DEBUG_FAKE_AI] = useFake }
    }

    override fun observeLastNotifiedDate(): Flow<LocalDate?> =
        dataStore.data.map { prefs ->
            prefs[KEY_LAST_NOTIFIED_DATE]?.let(LocalDate::parse)
        }

    override suspend fun setLastNotifiedDate(date: LocalDate?) {
        dataStore.edit { prefs ->
            if (date == null) {
                prefs.remove(KEY_LAST_NOTIFIED_DATE)
            } else {
                prefs[KEY_LAST_NOTIFIED_DATE] = date.toString()
            }
        }
    }

    override fun observeCoarseCityId(): Flow<String?> =
        dataStore.data.map { it[KEY_COARSE_CITY_ID] }

    override suspend fun setCoarseCityId(id: String?) {
        dataStore.edit { prefs ->
            if (id.isNullOrBlank()) {
                prefs.remove(KEY_COARSE_CITY_ID)
            } else {
                prefs[KEY_COARSE_CITY_ID] = id
            }
        }
    }

    private companion object {
        const val PREFS_FILE = "shutterup_prefs.preferences_pb"
        const val DEFAULT_NOTIFY_HOUR = 9
        const val DEFAULT_NOTIFY_MINUTE = 0
        val KEY_NOTIFY_HOUR = intPreferencesKey("notify_hour")
        val KEY_NOTIFY_MINUTE = intPreferencesKey("notify_minute")
        val KEY_PRECISE_TIMING = booleanPreferencesKey("precise_timing")
        val KEY_THEME_FOCUS = stringPreferencesKey("theme_focus")
        val KEY_SERIES_ENABLED = booleanPreferencesKey("series_enabled")
        val KEY_PAUSED = booleanPreferencesKey("paused")
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val KEY_DEBUG_FAKE_AI = booleanPreferencesKey("debug_fake_ai")
        val KEY_LAST_NOTIFIED_DATE = stringPreferencesKey("last_notified_date")
        val KEY_COARSE_CITY_ID = stringPreferencesKey("coarse_city_id")
    }
}
