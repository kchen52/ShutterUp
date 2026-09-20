package app.shutterup.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PreferencesDataStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun defaults() = runTest {
        val repo = newRepo()
        assertEquals(LocalTime.of(9, 0), repo.observeNotifyTime().first())
        assertFalse(repo.observePreciseTiming().first())
        assertNull(repo.observeThemeFocus().first())
        assertFalse(repo.observeSeriesEnabled().first())
        assertFalse(repo.observePaused().first())
        assertFalse(repo.observeOnboardingComplete().first())
        assertFalse(repo.observeDebugUseFakeAi().first())
    }

    @Test
    fun roundTripEveryKeyIncludingClearingThemeFocus() = runTest {
        val repo = newRepo()

        repo.setNotifyTime(LocalTime.of(17, 45))
        assertEquals(LocalTime.of(17, 45), repo.observeNotifyTime().first())

        repo.setPreciseTiming(true)
        assertTrue(repo.observePreciseTiming().first())
        repo.setPreciseTiming(false)
        assertFalse(repo.observePreciseTiming().first())

        repo.setThemeFocus("my dog")
        assertEquals("my dog", repo.observeThemeFocus().first())
        repo.setThemeFocus("")
        assertNull(repo.observeThemeFocus().first())
        repo.setThemeFocus("architecture")
        assertEquals("architecture", repo.observeThemeFocus().first())
        repo.setThemeFocus(null)
        assertNull(repo.observeThemeFocus().first())

        assertFalse(repo.observeSeriesEnabled().first())
        repo.setSeriesEnabled(true)
        assertTrue(repo.observeSeriesEnabled().first())
        repo.setSeriesEnabled(false)
        assertFalse(repo.observeSeriesEnabled().first())

        repo.setPaused(true)
        assertTrue(repo.observePaused().first())

        repo.setOnboardingComplete(true)
        assertTrue(repo.observeOnboardingComplete().first())

        repo.setDebugUseFakeAi(true)
        assertTrue(repo.observeDebugUseFakeAi().first())
        repo.setDebugUseFakeAi(false)
        assertFalse(repo.observeDebugUseFakeAi().first())
    }

    @Test
    fun lastNotifiedDateRoundTripIncludingNull() = runTest {
        val repo = newRepo()
        assertNull(repo.observeLastNotifiedDate().first())

        val date = LocalDate.of(2024, 6, 15)
        repo.setLastNotifiedDate(date)
        assertEquals(date, repo.observeLastNotifiedDate().first())

        repo.setLastNotifiedDate(null)
        assertNull(repo.observeLastNotifiedDate().first())
    }

    private fun TestScope.newRepo(): PreferencesDataStore {
        val file = tempFolder.newFile("shutterup_prefs.preferences_pb")
        file.delete()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )
        return PreferencesDataStore(dataStore)
    }
}
