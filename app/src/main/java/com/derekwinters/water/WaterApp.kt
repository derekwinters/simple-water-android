package com.derekwinters.water

import android.app.Application
import com.derekwinters.water.data.AppDatabase
import com.derekwinters.water.data.SettingsStore
import com.derekwinters.water.data.WaterRepository
import com.derekwinters.water.reminders.Notifications
import com.derekwinters.water.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WaterApp : Application() {
    /** Process-wide scope for short background work (writes, rescheduling, receiver work). */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    val repository: WaterRepository by lazy { WaterRepository(this, database, settingsStore) }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
        // REM-008: the alarm reflects the stored settings whenever the process starts.
        appScope.launch { ReminderScheduler.rescheduleAll(this@WaterApp) }
    }
}
