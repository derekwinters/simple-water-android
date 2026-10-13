package com.derekwinters.water.core

import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign

/**
 * REM-017, REM-018: how much should have been drunk by a time of day to stay on track, and how
 * far today's total is from it. Both are unrounded ml; round with [Pace.rounded] for display.
 */
data class Pace(val expectedMl: Double, val differenceMl: Double) {
    companion object {
        private const val ROUND_TO_ML = 10

        /**
         * REM-017: `goal × (time − start) / (end − start)`, clamped to 0…goal (invariant: never
         * negative, never above the goal). Null when start equals end: no ramp, no pace.
         */
        fun at(time: LocalTime, totalMl: Int, goalMl: Int, window: ReminderSettings): Pace? {
            val start = window.startMinute * 60.0
            val end = window.endMinute * 60.0
            if (end <= start) return null
            val goal = goalMl.coerceAtLeast(0).toDouble()
            val progress = ((time.toSecondOfDay() - start) / (end - start)).coerceIn(0.0, 1.0)
            val expected = (goal * progress).coerceIn(0.0, goal)
            return Pace(expectedMl = expected, differenceMl = totalMl - expected)
        }

        /**
         * REM-018: [ml] in display units of [unit] — the nearest 10 ml, or the nearest whole fl oz —
         * with an exact half rounding away from zero, so behind and ahead round alike.
         */
        fun rounded(ml: Double, unit: VolumeUnit): Int {
            val magnitude = when (unit) {
                VolumeUnit.ML -> floor(abs(ml) / ROUND_TO_ML + 0.5).toInt() * ROUND_TO_ML
                VolumeUnit.FL_OZ -> floor(abs(ml) / Units.ML_PER_FL_OZ + 0.5).toInt()
            }
            return if (magnitude == 0) 0 else magnitude * sign(ml).toInt()
        }
    }
}

/** REM-019, UI-013: the pace wording. */
object PaceText {
    /**
     * REM-019: the reminder's pace sentence, or null when the plain REM-012 text should be used —
     * start equals end, or the expected amount rounds to 0 (the start-time slot).
     */
    fun notificationBody(time: LocalTime, totalMl: Int, goalMl: Int, window: ReminderSettings, unit: VolumeUnit): String? {
        val pace = Pace.at(time, totalMl, goalMl, window) ?: return null
        val expected = Pace.rounded(pace.expectedMl, unit)
        if (expected == 0) return null
        val standing = standing(pace, unit)
        return "Ideally you'd have had ${Units.formatDisplay(expected, unit)} by now to stay on track. You're $standing."
    }

    /**
     * UI-013: "On pace: 1,000 ml by now · 250 ml behind", only while [time] is strictly inside the
     * window and the goal is not met; null otherwise. The window applies whether or not reminders
     * are switched on.
     */
    fun homeLine(time: LocalTime, totalMl: Int, goalMl: Int, window: ReminderSettings, unit: VolumeUnit): String? {
        if (totalMl >= goalMl) return null
        val second = time.toSecondOfDay()
        if (second <= window.startMinute * 60 || second >= window.endMinute * 60) return null
        val pace = Pace.at(time, totalMl, goalMl, window) ?: return null
        val expected = Units.formatDisplay(Pace.rounded(pace.expectedMl, unit), unit)
        val standing = standing(pace, unit)
        return "On pace: $expected by now · $standing"
    }

    /** REM-018: "250 ml behind", "200 ml ahead", or "right on track" when it rounds to 0. */
    private fun standing(pace: Pace, unit: VolumeUnit): String {
        val diff = Pace.rounded(pace.differenceMl, unit)
        if (diff == 0) return "right on track"
        return "${Units.formatDisplay(abs(diff), unit)} ${if (diff < 0) "behind" else "ahead"}"
    }
}
