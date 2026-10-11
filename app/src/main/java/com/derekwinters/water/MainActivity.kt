package com.derekwinters.water

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.derekwinters.water.designsystem.AppTheme
import com.derekwinters.water.reminders.Notifications
import com.derekwinters.water.reminders.ReminderScheduler
import com.derekwinters.water.ui.WaterNavHost
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** Incremented each time a notification asks for the home screen (REM-012, UI-001). */
    private var openHomeRequests by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        // DS-023: edge-to-edge with light icons on both bars, whatever the system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        val app = application as WaterApp
        setContent {
            AppTheme {
                WaterNavHost(app = app, openHomeRequests = openHomeRequests)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action != ACTION_OPEN_HOME) return
        NotificationManagerCompat.from(this).cancel(Notifications.REMINDER_ID)
        openHomeRequests++
    }

    override fun onResume() {
        super.onResume()
        // REM-008: the exact-alarm permission may have changed while the app was away.
        lifecycleScope.launch { ReminderScheduler.rescheduleAll(this@MainActivity) }
    }

    companion object {
        const val ACTION_OPEN_HOME = "com.derekwinters.water.action.OPEN_HOME"
    }
}
