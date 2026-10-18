package com.derekwinters.water.core

import java.time.ZonedDateTime

/**
 * UI-013: what the two rings show. [fraction] is the inner ring (LOG-003), 0–1;
 * [outerFraction] is the outer ring (LOG-013), or null while there is no ideal amount (LOG-014),
 * when [outerTicks] is empty too.
 */
data class RingState(val fraction: Float, val outerFraction: Float?, val outerTicks: List<Float>)

/**
 * The rings of the home screen's ring card (docs/spec/screens.md UI-013), whose fractions the home
 * screen widget's water and ideal line also take (UI-015, UI-020). Invariant: both surfaces take
 * them from here, never a second calculation.
 */
object Rings {
    /** LOG-003, UI-013: [totalMl] against [goalMl], and the ideal amount when there is one. */
    fun of(totalMl: Double, goalMl: Int, pace: PaceState?): RingState = RingState(
        fraction = GoalMath.fraction(totalMl, goalMl),
        outerFraction = pace?.idealFraction,
        outerTicks = pace?.tickFractions.orEmpty(),
    )

    /**
     * UI-013: the rings at [now] from [entries]. Widget invariant: only the entries of [now]'s local
     * date count (LOG-002), so a previous day's drinks never show, whatever the caller passes.
     */
    fun today(entries: List<LoggedAmount>, now: ZonedDateTime, goalMl: Int, reminders: ReminderSettings): RingState =
        of(totalToday(entries, now), goalMl, Pace.today(reminders, now.toLocalDateTime(), goalMl))

    /** LOG-002, LOG-003: the total of the [entries] on [now]'s local date, unrounded. */
    fun totalToday(entries: List<LoggedAmount>, now: ZonedDateTime): Double {
        val date = now.toLocalDate()
        return entries.filter { DayWindow.dateOf(it.loggedAtMillis, now.zone) == date }.sumOf { it.amountMl }
    }

    /** UI-018, LOG-002: the start of the local date after [now]'s, never "now + 24 hours". */
    fun nextMidnightMillis(now: ZonedDateTime): Long = DayWindow.endMillis(now.toLocalDate(), now.zone)
}
