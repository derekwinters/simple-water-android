package com.derekwinters.water.data

import android.content.Context
import com.derekwinters.water.core.DayWindow
import com.derekwinters.water.core.GoalRecord
import com.derekwinters.water.core.Profile
import com.derekwinters.water.reminders.Notifications
import com.derekwinters.water.reminders.ReminderScheduler
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The one place screens, receivers and the scheduler read and write data through. */
class WaterRepository(
    private val context: Context,
    database: AppDatabase,
    val settingsStore: SettingsStore,
) {
    private val entries = database.drinkEntryDao()
    private val goals = database.goalRecordDao()

    val settings: Flow<UserSettings> get() = settingsStore.settings

    /** LOG-002, LOG-007: the entries of the local dates [from]..[to] inclusive, newest first. */
    fun entriesBetween(from: LocalDate, to: LocalDate, zone: ZoneId): Flow<List<DrinkEntry>> =
        entries.observeBetween(DayWindow.startMillis(from, zone), DayWindow.endMillis(to, zone))

    fun entriesOn(date: LocalDate, zone: ZoneId): Flow<List<DrinkEntry>> = entriesBetween(date, date, zone)

    /** LOG-003. */
    suspend fun totalOn(date: LocalDate, zone: ZoneId): Double =
        entries.totalBetween(DayWindow.startMillis(date, zone), DayWindow.endMillis(date, zone))

    /** GOAL-012. */
    val goalRecords: Flow<List<GoalRecord>> = goals.observeAll().map { list -> list.map { it.toGoalRecord() } }

    /** LOG-005, LOG-006: logs [amountMl] now. LOG-011: dismisses the reminder once the goal is met. */
    suspend fun addEntry(amountMl: Double, atMillis: Long = System.currentTimeMillis()) {
        entries.insert(DrinkEntry(amountMl = amountMl, loggedAt = atMillis))
        dismissReminderIfGoalMet()
    }

    /** LOG-008: removes [entry]; [restoreEntry] puts it back unchanged. */
    suspend fun deleteEntry(entry: DrinkEntry) {
        entries.delete(entry)
    }

    suspend fun restoreEntry(entry: DrinkEntry) {
        entries.insert(entry)
        dismissReminderIfGoalMet()
    }

    /** GOAL-010: first run finished (or skipped, with [Profile.DEFAULT]). */
    suspend fun completeFirstRun(profile: Profile) {
        updateSettings { it.copy(onboarded = true, profile = profile) }
    }

    /**
     * Applies a settings change, records the effective goal against today when it changed or
     * first run just finished (GOAL-012), and re-arms the reminder alarm (REM-008).
     */
    suspend fun updateSettings(transform: (UserSettings) -> UserSettings) {
        val (before, after) = settingsStore.update(transform)
        if (after.onboarded && (!before.onboarded || before.goalMl != after.goalMl)) {
            goals.upsert(GoalRecordEntity(epochDay = LocalDate.now().toEpochDay(), goalMl = after.goalMl))
        }
        if (before.reminders != after.reminders || before.onboarded != after.onboarded) {
            ReminderScheduler.rescheduleAll(context)
        }
        if (before.goalMl != after.goalMl) dismissReminderIfGoalMet()
    }

    private suspend fun dismissReminderIfGoalMet() {
        val goal = settingsStore.current().goalMl
        if (totalOn(LocalDate.now(), ZoneId.systemDefault()) >= goal) Notifications.cancelReminder(context)
    }
}
