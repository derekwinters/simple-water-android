package com.derekwinters.water.core

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** GOAL-012: the effective goal recorded against a date. */
data class GoalRecord(val epochDay: Long, val goalMl: Int)

/** A logged amount, as history needs it. */
data class LoggedAmount(val loggedAtMillis: Long, val amountMl: Int)

/** HIST-003: one day of the week view. */
data class DaySummary(val date: LocalDate, val totalMl: Int, val goalMl: Int) {
    val met: Boolean get() = totalMl >= goalMl
    val fraction: Float get() = GoalMath.fraction(totalMl, goalMl)
}

/** HIST-004: seven days, how many met their goal, and the average daily total. */
data class WeekSummary(val days: List<DaySummary>) {
    val metCount: Int get() = days.count { it.met }
    val averageMl: Int get() = if (days.isEmpty()) 0 else (days.sumOf { it.totalMl }.toDouble() / days.size).roundToInt()
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
        totals: Map<LocalDate, Int>,
        records: List<GoalRecord>,
        currentGoalMl: Int,
    ): WeekSummary {
        val end = if (endDate.isAfter(today)) today else endDate
        val days = (DAYS - 1 downTo 0).map { back ->
            val date = end.minusDays(back.toLong())
            DaySummary(date, totals[date] ?: 0, goalOn(date, records, currentGoalMl))
        }
        return WeekSummary(days)
    }

    /** LOG-002: totals per local date. */
    fun totalsByDay(entries: List<LoggedAmount>, zone: ZoneId): Map<LocalDate, Int> =
        entries.groupBy { DayWindow.dateOf(it.loggedAtMillis, zone) }
            .mapValues { (_, list) -> list.sumOf { it.amountMl } }

    fun canGoForward(endDate: LocalDate, today: LocalDate): Boolean = endDate.isBefore(today)

    fun previousEnd(endDate: LocalDate): LocalDate = endDate.minusDays(DAYS.toLong())

    fun nextEnd(endDate: LocalDate, today: LocalDate): LocalDate {
        val next = endDate.plusDays(DAYS.toLong())
        return if (next.isAfter(today)) today else next
    }
}
