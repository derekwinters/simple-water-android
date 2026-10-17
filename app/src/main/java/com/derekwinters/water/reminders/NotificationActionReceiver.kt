package com.derekwinters.water.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.derekwinters.water.WaterApp
import com.derekwinters.water.core.EntryRules
import kotlinx.coroutines.launch

/** REM-012: the notification's "Log <default size>" action — logs without opening the app. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_LOG) return
        Notifications.cancelReminder(context)
        // Decimal ml (GOAL-008); a notification posted before the upgrade still carries an Int.
        @Suppress("DEPRECATION")
        val amount = (intent.extras?.get(EXTRA_AMOUNT_ML) as? Number)?.toDouble() ?: return
        if (!EntryRules.isValid(amount)) return
        val app = context.applicationContext as WaterApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                app.repository.addEntry(amount)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_LOG = "com.derekwinters.water.action.LOG_DRINK"
        const val EXTRA_AMOUNT_ML = "amountMl"
    }
}
