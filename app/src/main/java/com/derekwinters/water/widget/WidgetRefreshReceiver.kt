package com.derekwinters.water.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.derekwinters.water.WaterApp
import kotlinx.coroutines.launch

/** UI-018: the midnight alarm; redraws the widgets for the new day and arms the next midnight. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WaterWidgetUpdater.ACTION_MIDNIGHT) return
        val app = context.applicationContext as WaterApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                WaterWidgetUpdater.refreshAll(app)
            } finally {
                pending.finish()
            }
        }
    }
}
