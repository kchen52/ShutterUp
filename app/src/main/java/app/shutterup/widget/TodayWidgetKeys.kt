package app.shutterup.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Preference keys written via [androidx.glance.appwidget.state.updateAppWidgetState]. */
internal object TodayWidgetKeys {
    val dateIso = stringPreferencesKey("date_iso")
    val weekday = stringPreferencesKey("weekday")
    val theme = stringPreferencesKey("theme")
    val title = stringPreferencesKey("title")
    val oneLiner = stringPreferencesKey("one_liner")
    val constraint = stringPreferencesKey("constraint")
    val streakDays = intPreferencesKey("streak_days")
    val paused = booleanPreferencesKey("paused")
    val completed = booleanPreferencesKey("completed")
    val thumbPath = stringPreferencesKey("thumb_path")
}
