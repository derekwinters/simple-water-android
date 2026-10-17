package com.derekwinters.water.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.derekwinters.water.WaterApp
import com.derekwinters.water.core.RingState
import com.derekwinters.water.core.Rings
import com.derekwinters.water.reminders.ReminderScheduler
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Keeps every placed [WaterWidget] showing today's rings (docs/spec/screens.md UI-018). */
object WaterWidgetUpdater {
    private const val TAG = "WaterWidgetUpdater"
    const val ACTION_MIDNIGHT = "com.derekwinters.water.action.WIDGET_MIDNIGHT"
    private const val REQUEST_MIDNIGHT = 1

    internal val RINGS_KEY = stringPreferencesKey("rings")
    internal val AT_KEY = longPreferencesKey("rings_at_millis")

    /** Rings and the moment they were computed for. */
    data class Drawn(val atMillis: Long, val rings: RingState)

    /** Refreshes run one at a time, so rings computed earlier never overwrite later ones. */
    private val mutex = Mutex()

    /**
     * UI-015: the rings now. Widget invariants: the same `Rings` function as home, for the entries
     * of the local date of this moment, never a previous day's.
     */
    suspend fun current(app: WaterApp): Drawn {
        val settings = app.settingsStore.current()
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val entries = app.repository.entriesOn(now.toLocalDate(), zone).first().map { it.toLoggedAmount() }
        return Drawn(now.toInstant().toEpochMilli(), Rings.today(entries, now, settings.goalMl, settings.reminders))
    }

    /** Whichever is newer: the rings computed when the session started or the last refresh's. */
    fun newer(drawn: Drawn, storedRings: String?, storedAtMillis: Long?): RingState {
        val stored = RingState.decode(storedRings)
        return if (stored != null && storedAtMillis != null && storedAtMillis > drawn.atMillis) stored else drawn.rings
    }

    /**
     * UI-018: redraws every placed widget with the rings now, and keeps the midnight refresh armed
     * while any is placed. A failure is logged, never thrown into the write that triggered it.
     */
    suspend fun refreshAll(context: Context) {
        val app = context.applicationContext as WaterApp
        try {
            mutex.withLock {
                val ids = GlanceAppWidgetManager(app).getGlanceIds(WaterWidget::class.java)
                if (ids.isEmpty()) {
                    cancelMidnight(app)
                    return
                }
                val drawn = current(app)
                ids.forEach { id ->
                    updateAppWidgetState(app, id) { state ->
                        state[RINGS_KEY] = drawn.rings.encode()
                        state[AT_KEY] = drawn.atMillis
                    }
                    WaterWidget().update(app, id)
                }
                armMidnight(app)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Widget refresh failed", e)
        }
    }

    /** UI-018, LOG-002: the refresh at the next local midnight, exact when allowed (as REM-007). */
    fun armMidnight(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val at = Rings.nextMidnightMillis(ZonedDateTime.now(ZoneId.systemDefault()))
        val intent = midnightIntent(context)
        try {
            if (ReminderScheduler.canScheduleExact(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm refused; falling back to inexact", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    fun cancelMidnight(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(midnightIntent(context))
    }

    /** One request code and action, so arming again replaces the previous midnight alarm. */
    private fun midnightIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_MIDNIGHT,
        Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION_MIDNIGHT),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
