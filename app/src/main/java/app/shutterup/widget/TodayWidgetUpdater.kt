package app.shutterup.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import app.shutterup.domain.repository.DayPromptRepository
import app.shutterup.domain.repository.EntryRepository
import app.shutterup.domain.repository.GamificationRepository
import app.shutterup.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Pushes today's prompt / streak into Glance state and requests a redraw.
 * Called from [app.shutterup.work.DailyPromptWorker] and after achievement unlock.
 */
@Singleton
class TodayWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val days: DayPromptRepository,
    private val entries: EntryRepository,
    private val gamification: GamificationRepository,
    private val preferences: PreferencesRepository,
    private val clock: Clock,
    private val zone: ZoneId,
    @Named("appScope") private val scope: CoroutineScope,
) {
    fun refreshAsync() {
        scope.launch { runCatching { refresh() } }
    }

    suspend fun refresh() {
        val today = LocalDate.now(clock.withZone(zone))
        val prompt = days.getDay(today)
        val streak = gamification.observeStreak().first()
        val paused = preferences.observePaused().first()
        val thumb = entries.observeEntries(today).first().firstOrNull()?.thumbPath
        val snapshot = TodayWidgetState.from(today, prompt, streak.current, paused, thumb)
        val manager = GlanceAppWidgetManager(context)
        val ids = manager.getGlanceIds(TodayGlanceWidget::class.java)
        val widget = TodayGlanceWidget()
        for (id in ids) {
            updateAppWidgetState(context, id) { prefs ->
                writeState(prefs, snapshot)
            }
            widget.update(context, id)
        }
    }

    private fun writeState(prefs: MutablePreferences, snapshot: TodayWidgetState) {
        prefs[TodayWidgetKeys.dateIso] = snapshot.dateIso
        prefs[TodayWidgetKeys.weekday] = snapshot.weekday
        prefs[TodayWidgetKeys.theme] = snapshot.theme
        prefs[TodayWidgetKeys.title] = snapshot.title
        prefs[TodayWidgetKeys.oneLiner] = snapshot.oneLiner
        if (snapshot.constraint.isNullOrEmpty()) {
            prefs.remove(TodayWidgetKeys.constraint)
        } else {
            prefs[TodayWidgetKeys.constraint] = snapshot.constraint
        }
        prefs[TodayWidgetKeys.streakDays] = snapshot.streakDays
        prefs[TodayWidgetKeys.paused] = snapshot.paused
        prefs[TodayWidgetKeys.completed] = snapshot.completed
        if (snapshot.thumbPath.isNullOrEmpty()) {
            prefs.remove(TodayWidgetKeys.thumbPath)
        } else {
            prefs[TodayWidgetKeys.thumbPath] = snapshot.thumbPath
        }
    }
}
