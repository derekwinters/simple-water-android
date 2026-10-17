package com.derekwinters.water.core

import java.time.LocalDate
import java.time.ZoneId

/** GOAL-012: the effective goal recorded against a date. */
data class GoalRecord(val epochDay: Long, val goalMl: Int)

/** A logged amount, as history needs it. */
data class LoggedAmount(val loggedAtMillis: Long, val amountMl: Double)

/** HIST-003: one day of the week view. [totalMl] is unrounded decimal ml (GOAL-008). */
data class DaySummary(val date: LocalDate, val totalMl: Double, val goalMl: Int) {
    val met: Boolean get() = totalMl >= goalMl
    val fraction: Float get() = GoalMath.fraction(totalMl, goalMl)
}

/** HIST-004: seven days, how many met their goal, and the average daily total (unrounded). */
data class WeekSummary(val days: List<DaySummary>) {
    val metCount: Int get() = days.count { it.met }
    val averageMl: Double get() = if (days.isEmpty()) 0.0 else days.sumOf { it.totalMl } / days.size
}

/** The week view (docs/spec/history.md). */
object History {
    const val DAYS = 7

    /**
     * HIST-002: the latest record on or before [date]; before any record, the earliest; with no
     * records, [currentGoalMl]. HIST-006: never the current goal when a record applies.
     */
    fun goalOn(date: LocalDate, records: List<GoalRecord>, currentGoalMl: Int): Int {
        if (records.isEmpty()) return currentGoalMl
        val day = date.toEpochDay()
        val sorted = records.sortedBy { it.epochDay }
        return (sorted.lastOrNull { it.epochDay <= day } ?: sorted.first()).goalMl
    }

    /** HIST-001, HIST-005: the seven days ending on [endDate], never past [today]. */
    fun week(
        endDate: LocalDate,
        today: LocalDate,
        totals: Map<LocalDate, Double>,
        records: List<GoalRecord>,
        currentGoalMl: Int,
    ): WeekSummary {
        val end = if (endDate.isAfter(today)) today else endDate
        val days = (DAYS - 1 downTo 0).map { back ->
            val date = end.minusDays(back.toLong())
            DaySummary(date, totals[date] ?: 0.0, goalOn(date, records, currentGoalMl))
        }
        return WeekSummary(days)
    }

    /** LOG-002: totals per local date, summed unrounded (GOAL-008 invariant). */
    fun totalsByDay(entries: List<LoggedAmount>, zone: ZoneId): Map<LocalDate, Double> =
        entries.groupBy { DayWindow.dateOf(it.loggedAtMillis, zone) }
            .mapValues { (_, list) -> list.sumOf { it.amountMl } }

    fun canGoForward(endDate: LocalDate, today: LocalDate): Boolean = endDate.isBefore(today)

    fun previousEnd(endDate: LocalDate): LocalDate = endDate.minusDays(DAYS.toLong())

    fun nextEnd(endDate: LocalDate, today: LocalDate): LocalDate {
        val next = endDate.plusDays(DAYS.toLong())
        return if (next.isAfter(today)) today else next
    }
}
