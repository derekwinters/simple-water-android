package com.derekwinters.water.core

import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * LOG-013: today's ideal amount at one moment. [times] are today's reminder times `t0 … tn`
 * (REM-004), at least two of them (LOG-014); [passed] is `k`, the index of the last one at or
 * before now, or -1 before `t0`.
 */
data class PaceState(val goalMl: Int, val times: List<LocalTime>, val passed: Int) {
    init {
        require(times.size >= 2) { "LOG-014: an ideal amount needs at least two reminder times" }
        require(passed in -1..times.lastIndex)
    }

    private val intervals: Int get() = times.size - 1

    /**
     * LOG-013: `goal × k / n`; 0 before `t0`. LOG-013 invariant: it changes only when `k` does,
     * so only at a reminder time, and `0 ≤ k ≤ n` keeps it within 0 and the goal.
     */
    val idealMl: Double get() = if (passed <= 0) 0.0 else goalMl.toDouble() * passed / intervals

    /** UI-013: how full the outer ring is, 0–1. */
    val idealFraction: Float get() = if (passed <= 0) 0f else passed.toFloat() / intervals

    /** UI-013: the last reminder time that has passed ("Ideal by 12:00"), or null before `t0`. */
    val lastPassed: LocalTime? get() = times.getOrNull(passed)

    /** UI-013: "Ideal starts at 8:00". */
    val firstTime: LocalTime get() = times.first()

    /** UI-013: a tick at each reminder time, as a fraction of the circle; `tn` falls on `t0`. */
    val tickFractions: List<Float> get() = (0 until intervals).map { it.toFloat() / intervals }
}

/** UI-013: the main line ("220 ml behind") over the smaller line ("Ideal by 12:00: …"). */
data class PaceText(val main: String, val detail: String)

/** The ideal amount and the behind/ahead text (docs/spec/logging.md LOG-013–015, UI-013). */
object Pace {
    /** LOG-015: the ideal amount and the difference are shown to the nearest 10 ml. */
    const val ROUND_TO_ML = 10

    /**
     * LOG-013, LOG-014: the ideal amount at [now], or null when there is none — reminders off,
     * today not an active day, or fewer than two reminder times today.
     */
    fun today(settings: ReminderSettings, now: LocalDateTime, goalMl: Int): PaceState? {
        if (!settings.enabled || now.dayOfWeek !in settings.days) return null
        val times = ReminderSchedule.slotsFor(settings)
        if (times.size < 2) return null
        val time = now.toLocalTime()
        return PaceState(goalMl, times, times.indexOfLast { !it.isAfter(time) })
    }

    /** LOG-015: [ml] (not negative) to the nearest 10 ml, an exact half up, or the nearest whole fl oz. */
    private fun displayAmount(ml: Double, unit: VolumeUnit): Int = when (unit) {
        VolumeUnit.ML -> (ml / ROUND_TO_ML).roundToInt() * ROUND_TO_ML
        VolumeUnit.FL_OZ -> Units.mlToDisplay(ml, unit)
    }

    /** LOG-015, GOAL-009: "820 ml" or "28 fl oz". */
    fun formatAmount(ml: Double, unit: VolumeUnit): String =
        Units.formatVolume(Units.displayToMl(displayAmount(ml, unit), unit), unit)

    /**
     * LOG-015: "220 ml behind", "370 ml ahead" or "On track". The difference is taken unrounded
     * (GOAL-008 invariant) and only its size is rounded, so behind and ahead round alike.
     */
    fun status(totalMl: Double, idealMl: Double, unit: VolumeUnit): String {
        val difference = totalMl - idealMl
        val size = displayAmount(abs(difference), unit)
        return when {
            size == 0 -> "On track"
            difference < 0 -> "${Units.formatVolume(Units.displayToMl(size, unit), unit)} behind"
            else -> "${Units.formatVolume(Units.displayToMl(size, unit), unit)} ahead"
        }
    }

    /**
     * UI-013: the two lines under the rings, or null once the goal is met ("Goal met" alone,
     * UI-004). "<remaining> to go" keeps LOG-003's rounding (LOG-015). [formatTime] formats a
     * reminder time for display (UI-009).
     */
    fun text(state: PaceState, totalMl: Double, unit: VolumeUnit, formatTime: (LocalTime) -> String): PaceText? {
        val remaining = GoalMath.remainingMl(totalMl, state.goalMl)
        if (remaining == 0.0) return null
        val toGo = "${Units.formatVolume(remaining, unit)} to go"
        val last = state.lastPassed ?: return PaceText("Ideal starts at ${formatTime(state.firstTime)}", toGo)
        return PaceText(
            main = status(totalMl, state.idealMl, unit),
            detail = "Ideal by ${formatTime(last)}: ${formatAmount(state.idealMl, unit)} · $toGo",
        )
    }
}
