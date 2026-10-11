package com.derekwinters.water.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.derekwinters.water.WaterApp
import com.derekwinters.water.core.ReminderPolicy
import com.derekwinters.water.core.ReminderSchedule
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps exactly one reminder alarm armed, for the next reminder time (REM-006), following
 * simple-stretch-android's scheduler: one PendingIntent request code, so arming again replaces the
 * previous alarm; exact when allowed, inexact otherwise (REM-007); and the armed trigger time is
 * remembered so a reschedule keeps a just-due occurrence that has not been handled yet (REM-009).
 */
object ReminderScheduler {
    private const val TAG = "ReminderScheduler"
    const val ACTION_FIRE = "com.derekwinters.water.action.FIRE_REMINDER"
    const val EXTRA_TRIGGER_AT = "triggerAt"

    private const val REQUEST_CODE = 1
    private const val PREFS = "reminder_scheduler"
    private const val KEY_PENDING_AT = "pending_at"

    private val mutex = Mutex()

    /** REM-007, REM-015. */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    /** REM-005, REM-006, REM-008, REM-009. */
    suspend fun rescheduleAll(context: Context) = mutex.withLock {
        val app = context.applicationContext as WaterApp
        val settings = app.settingsStore.current()
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val zone = ZoneId.systemDefault()
        val nowMillis = System.currentTimeMillis()

        // REM-009: an alarm that came due moments ago and has not been handled is kept.
        val pendingAt = prefs.getLong(KEY_PENDING_AT, -1L)
        val baseMillis = if (pendingAt in 0..nowMillis && nowMillis - pendingAt < ReminderPolicy.GRACE_MILLIS) {
            pendingAt - 1
        } else {
            nowMillis
        }
        val base = LocalDateTime.ofInstant(Instant.ofEpochMilli(baseMillis), zone)

        // REM-005: nothing is armed before first run is done, or while reminders are off.
        val next = if (settings.onboarded) ReminderSchedule.next(base, settings.reminders) else null
        if (next == null) {
            cancelAlarm(app)
            prefs.edit().remove(KEY_PENDING_AT).apply()
            return@withLock
        }
        val triggerAt = next.atZone(zone).toInstant().toEpochMilli()
        setAlarm(app, triggerAt)
        prefs.edit().putLong(KEY_PENDING_AT, triggerAt).apply()
    }

    /** Called by [AlarmReceiver] once an occurrence has been handled: arm the next one. */
    suspend fun onAlarmHandled(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PENDING_AT)
            .commit()
        rescheduleAll(context)
    }

    private fun pendingIntent(context: Context, triggerAt: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_TRIGGER_AT, triggerAt)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun setAlarm(context: Context, triggerAt: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        // Cancel first: FLAG_UPDATE_CURRENT on an immutable PendingIntent does not reliably
        // refresh the extras of an alarm that is already registered.
        val stale = pendingIntent(context, triggerAt)
        alarmManager.cancel(stale)
        stale.cancel()
        val pi = pendingIntent(context, triggerAt)
        try {
            if (canScheduleExact(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm refused; falling back to inexact", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context, 0L)
        alarmManager.cancel(pi)
        pi.cancel()
    }
}
