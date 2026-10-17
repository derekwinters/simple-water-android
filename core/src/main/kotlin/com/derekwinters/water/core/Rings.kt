package com.derekwinters.water.core

import java.time.ZonedDateTime

/**
 * UI-013, UI-015: what the two rings show. [fraction] is the inner ring (LOG-003), 0–1;
 * [outerFraction] is the outer ring (LOG-013), or null while there is no ideal amount (LOG-014),
 * when [outerTicks] is empty too.
 */
data class RingState(val fraction: Float, val outerFraction: Float?, val outerTicks: List<Float>) {
    /** UI-018: the form the widget stores between refreshes; [decode] reads it back unchanged. */
    fun encode(): String = listOf(
        fraction.toString(),
        outerFraction?.toString().orEmpty(),
        outerTicks.joinToString(TICK_SEPARATOR),
    ).joinToString(FIELD_SEPARATOR)

    companion object {
        private const val FIELD_SEPARATOR = ";"
        private const val TICK_SEPARATOR = ","

        /** The rings [encode] wrote, or null for anything else. */
        fun decode(text: String?): RingState? {
            val fields = text?.split(FIELD_SEPARATOR) ?: return null
            if (fields.size != 3) return null
            val fraction = fields[0].toFloatOrNull() ?: return null
            val outer = if (fields[1].isEmpty()) null else fields[1].toFloatOrNull() ?: return null
            val ticks = if (fields[2].isEmpty()) {
                emptyList()
            } else {
                fields[2].split(TICK_SEPARATOR).map { it.toFloatOrNull() ?: return null }
            }
            return RingState(fraction, outer, ticks)
        }
    }
}

/**
 * The rings of the home screen's ring card and of the home screen widget (docs/spec/screens.md
 * UI-013, UI-015). Invariant: both surfaces take their rings from here, never a second calculation.
 */
object Rings {
    /** LOG-003, UI-013: [totalMl] against [goalMl], and the ideal amount when there is one. */
    fun of(totalMl: Double, goalMl: Int, pace: PaceState?): RingState = RingState(
        fraction = GoalMath.fraction(totalMl, goalMl),
        outerFraction = pace?.idealFraction,
        outerTicks = pace?.tickFractions.orEmpty(),
    )

    /**
     * UI-015: the rings at [now] from [entries]. Widget invariant: only the entries of [now]'s local
     * date count (LOG-002), so a previous day's drinks never show, whatever the caller passes.
     */
    fun today(entries: List<LoggedAmount>, now: ZonedDateTime, goalMl: Int, reminders: ReminderSettings): RingState {
        val date = now.toLocalDate()
        val total = entries.filter { DayWindow.dateOf(it.loggedAtMillis, now.zone) == date }.sumOf { it.amountMl }
        return of(total, goalMl, Pace.today(reminders, now.toLocalDateTime(), goalMl))
    }

    /** UI-018, LOG-002: the start of the local date after [now]'s, never "now + 24 hours". */
    fun nextMidnightMillis(now: ZonedDateTime): Long = DayWindow.endMillis(now.toLocalDate(), now.zone)
}
