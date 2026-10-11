package com.derekwinters.water.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.derekwinters.water.core.DrinkSizes
import com.derekwinters.water.core.GoalMath
import com.derekwinters.water.core.Profile
import com.derekwinters.water.core.ReminderSchedule
import com.derekwinters.water.core.ReminderSettings
import com.derekwinters.water.core.VolumeUnit
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Everything the user configures. Volumes are ml, weight is kg. */
data class UserSettings(
    /** GOAL-010: true once first run has been completed or skipped. */
    val onboarded: Boolean = false,
    val profile: Profile = Profile.DEFAULT,
    /** GOAL-006: null when the recommendation is in use. */
    val customGoalMl: Int? = null,
    val volumeUnit: VolumeUnit = VolumeUnit.ML,
    val drinkSizes: List<Int> = DrinkSizes.DEFAULT,
    val reminders: ReminderSettings = ReminderSettings.DEFAULT,
) {
    /** GOAL-004, GOAL-005: computed, never stored. */
    val recommendedGoalMl: Int get() = GoalMath.recommendedGoalMl(profile)

    /** GOAL-007. */
    val goalMl: Int get() = GoalMath.effectiveGoalMl(profile, customGoalMl)

    /** LOG-004: the first quick-add size. */
    val defaultSizeMl: Int get() = drinkSizes.firstOrNull() ?: DrinkSizes.DEFAULT.first()
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Stores [UserSettings] in Jetpack DataStore (APP-003). */
class SettingsStore(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val settings: Flow<UserSettings> = store.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toSettings() }

    suspend fun current(): UserSettings = settings.first()

    /** Applies [transform] atomically and returns the settings before and after. */
    suspend fun update(transform: (UserSettings) -> UserSettings): Pair<UserSettings, UserSettings> {
        var before = UserSettings()
        var after = UserSettings()
        store.edit { prefs ->
            before = prefs.toSettings()
            after = transform(before)
            prefs.write(after)
        }
        return before to after
    }

    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val WEIGHT_KG = doublePreferencesKey("weight_kg")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val ACTIVITY = stringPreferencesKey("activity")
        val CUSTOM_GOAL_ML = intPreferencesKey("custom_goal_ml")
        val VOLUME_UNIT = stringPreferencesKey("volume_unit")
        val DRINK_SIZES = stringPreferencesKey("drink_sizes")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_DAYS = intPreferencesKey("reminder_days")
        val REMINDER_START = intPreferencesKey("reminder_start_minute")
        val REMINDER_END = intPreferencesKey("reminder_end_minute")
        val REMINDER_INTERVAL = intPreferencesKey("reminder_interval_minutes")
    }

    private fun Preferences.toSettings(): UserSettings {
        val defaults = UserSettings()
        val reminderDefaults = ReminderSettings.DEFAULT
        val interval = this[Keys.REMINDER_INTERVAL]?.takeIf { it in ReminderSettings.INTERVALS }
            ?: reminderDefaults.intervalMinutes
        return UserSettings(
            onboarded = this[Keys.ONBOARDED] ?: false,
            profile = Profile(
                weightKg = this[Keys.WEIGHT_KG] ?: defaults.profile.weightKg,
                activity = enumOr(this[Keys.ACTIVITY], defaults.profile.activity),
                weightUnit = enumOr(this[Keys.WEIGHT_UNIT], defaults.profile.weightUnit),
            ),
            customGoalMl = this[Keys.CUSTOM_GOAL_ML]?.takeIf { GoalMath.isValidGoalMl(it) },
            volumeUnit = enumOr(this[Keys.VOLUME_UNIT], defaults.volumeUnit),
            drinkSizes = DrinkSizes.decode(this[Keys.DRINK_SIZES]),
            reminders = ReminderSettings(
                enabled = this[Keys.REMINDERS_ENABLED] ?: reminderDefaults.enabled,
                days = this[Keys.REMINDER_DAYS]?.let { ReminderSchedule.maskToDays(it) } ?: reminderDefaults.days,
                startMinute = this[Keys.REMINDER_START] ?: reminderDefaults.startMinute,
                endMinute = this[Keys.REMINDER_END] ?: reminderDefaults.endMinute,
                intervalMinutes = interval,
            ),
        )
    }

    private fun MutablePreferences.write(settings: UserSettings) {
        this[Keys.ONBOARDED] = settings.onboarded
        this[Keys.WEIGHT_KG] = settings.profile.weightKg
        this[Keys.WEIGHT_UNIT] = settings.profile.weightUnit.name
        this[Keys.ACTIVITY] = settings.profile.activity.name
        val custom = settings.customGoalMl
        if (custom == null) remove(Keys.CUSTOM_GOAL_ML) else this[Keys.CUSTOM_GOAL_ML] = custom
        this[Keys.VOLUME_UNIT] = settings.volumeUnit.name
        this[Keys.DRINK_SIZES] = DrinkSizes.encode(settings.drinkSizes)
        this[Keys.REMINDERS_ENABLED] = settings.reminders.enabled
        this[Keys.REMINDER_DAYS] = ReminderSchedule.daysToMask(settings.reminders.days)
        this[Keys.REMINDER_START] = settings.reminders.startMinute
        this[Keys.REMINDER_END] = settings.reminders.endMinute
        this[Keys.REMINDER_INTERVAL] = settings.reminders.intervalMinutes
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback
}
