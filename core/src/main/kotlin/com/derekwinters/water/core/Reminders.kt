package com.derekwinters.water.core

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/** REM-001, REM-002: reminder settings. Times are minutes after local midnight. */
data class ReminderSettings(
    val enabled: Boolean = true,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val startMinute: Int = 8 * 60,
    val endMinute: Int = 20 * 60,
    val intervalMinutes: Int = 60,
) {
    companion object {
        val DEFAULT = ReminderSettings()
        val INTERVALS: List<Int> = listOf(30, 60, 90, 120)
        const val TIME_STEP_MINUTES = 30
        const val LAST_MINUTE = 23 * 60 + 30
    }
}

/** When reminders fire (docs/spec/reminders.md). */
object ReminderSchedule {
    /** REM-006: how far ahead the next reminder is looked for. */
    const val MAX_DAYS_AHEAD = 8

    /** REM-004: start, then every interval, up to and including the end. */
    fun slotsFor(settings: ReminderSettings): List<LocalTime> {
        val step = settings.intervalMinutes.coerceAtLeast(1)
        val start = settings.startMinute.coerceIn(0, ReminderSettings.LAST_MINUTE)
        val end = settings.endMinute.coerceIn(start, ReminderSettings.LAST_MINUTE)
        return (start..end step step).map { LocalTime.of(it / 60, it % 60) }
    }

    /**
     * REM-005, REM-006: the first reminder strictly after [now] on an active day, or null when
     * reminders are off, no day is active, or nothing falls within [MAX_DAYS_AHEAD] days.
     */
    fun next(now: LocalDateTime, settings: ReminderSettings): LocalDateTime? {
        if (!settings.enabled || settings.days.isEmpty()) return null
        val slots = slotsFor(settings)
        var date = now.toLocalDate()
        repeat(MAX_DAYS_AHEAD + 1) {
            if (date.dayOfWeek in settings.days) {
                for (slot in slots) {
                    val candidate = LocalDateTime.of(date, slot)
                    if (candidate.isAfter(now)) return candidate
                }
            }
            date = date.plusDays(1)
        }
        return null
    }

    /** REM-003: moving the start past the end moves the end with it. */
    fun withStart(settings: ReminderSettings, minute: Int): ReminderSettings {
        val start = minute.coerceIn(0, ReminderSettings.LAST_MINUTE)
        return settings.copy(startMinute = start, endMinute = maxOf(settings.endMinute, start))
    }

    /** REM-003: moving the end before the start moves the start with it. */
    fun withEnd(settings: ReminderSettings, minute: Int): ReminderSettings {
        val end = minute.coerceIn(0, ReminderSettings.LAST_MINUTE)
        return settings.copy(endMinute = end, startMinute = minOf(settings.startMinute, end))
    }

    /** Bit 0 = Monday … bit 6 = Sunday. */
    fun daysToMask(days: Set<DayOfWeek>): Int = days.fold(0) { acc, day -> acc or (1 shl (day.value - 1)) }

    fun maskToDays(mask: Int): Set<DayOfWeek> = DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()
}

/** REM-010, REM-011: whether a delivered alarm becomes a notification. */
object ReminderPolicy {
    /** REM-010: an alarm delivered later than this is dropped. */
    const val LATE_LIMIT_MILLIS = 30 * 60 * 1000L

    /** REM-009: a just-due alarm not yet handled within this window is kept by a reschedule. */
    const val GRACE_MILLIS = 30 * 60 * 1000L

    /**
     * A met goal ([totalMl] ≥ [goalMl]) suppresses only the notification; the caller still arms
     * the next alarm (REM-011 invariant).
     */
    fun shouldNotify(
        alreadyHandled: Boolean,
        enabled: Boolean,
        dayActive: Boolean,
        lateByMillis: Long,
        totalMl: Double,
        goalMl: Int,
    ): Boolean = !alreadyHandled &&
        enabled &&
        dayActive &&
        lateByMillis <= LATE_LIMIT_MILLIS &&
        totalMl < goalMl
}

/** REM-012: the reminder notification's text. */
object ReminderText {
    const val TITLE = "Time for some water"

    fun body(totalMl: Double, goalMl: Int, unit: VolumeUnit): String =
        "${Units.formatNumber(totalMl, unit)} of ${Units.formatVolume(goalMl, unit)} today · " +
            "${Units.formatVolume(GoalMath.remainingMl(totalMl, goalMl), unit)} to go"

    fun logAction(ml: Double, unit: VolumeUnit): String = "Log ${Units.formatVolume(ml, unit)}"
}
