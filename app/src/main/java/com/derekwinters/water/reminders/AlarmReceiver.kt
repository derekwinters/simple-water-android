package com.derekwinters.water.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.derekwinters.water.WaterApp
import com.derekwinters.water.core.DayWindow
import com.derekwinters.water.core.ReminderPolicy
import java.time.ZoneId
import kotlinx.coroutines.launch

/** Receives the reminder alarm, decides whether to notify (REM-010), and arms the next alarm. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_FIRE) return
        val triggerAt = intent.getLongExtra(ReminderScheduler.EXTRA_TRIGGER_AT, System.currentTimeMillis())
        val app = context.applicationContext as WaterApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                handle(app, triggerAt)
            } finally {
                try {
                    // REM-011: armed whether or not a notification was shown.
                    ReminderScheduler.onAlarmHandled(app)
                } finally {
                    pending.finish()
                }
            }
        }
    }

    private suspend fun handle(app: WaterApp, triggerAt: Long) {
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // REM-010 invariant: the same occurrence can be delivered twice after a reschedule.
        val alreadyHandled = prefs.getLong(KEY_FIRED_AT, -1L) == triggerAt
        if (!alreadyHandled) prefs.edit().putLong(KEY_FIRED_AT, triggerAt).apply()

        val settings = app.settingsStore.current()
        val zone = ZoneId.systemDefault()
        val date = DayWindow.dateOf(triggerAt, zone)
        val total = app.repository.totalOn(date, zone)
        val show = ReminderPolicy.shouldNotify(
            alreadyHandled = alreadyHandled,
            enabled = settings.onboarded && settings.reminders.enabled,
            dayActive = date.dayOfWeek in settings.reminders.days,
            lateByMillis = System.currentTimeMillis() - triggerAt,
            totalMl = total,
            goalMl = settings.goalMl,
        )
        if (show) {
            Notifications.showReminder(app, total, settings.goalMl, settings.volumeUnit, settings.defaultSizeMl)
        }
    }

    private companion object {
        const val PREFS = "alarm_receiver"
        const val KEY_FIRED_AT = "fired_at"
    }
}

/** REM-008: re-arms the alarm after reboot, clock or time-zone changes, an update, or a permission change. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val app = context.applicationContext as WaterApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                ReminderScheduler.rescheduleAll(app)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED, // "android.intent.action.TIME_SET"
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
