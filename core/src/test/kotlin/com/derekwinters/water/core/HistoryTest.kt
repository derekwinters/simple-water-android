package com.derekwinters.water.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Requirement IDs refer to docs/spec/history.md. */
class HistoryTest {
    private val today = LocalDate.of(2026, 10, 11)
    private fun day(d: Int) = LocalDate.of(2026, 10, d)
    private fun record(d: Int, goal: Int) = GoalRecord(day(d).toEpochDay(), goal)

    // HIST-002: the goal in force is the latest record on or before the day.
    @Test
    fun goalOnADay() {
        val records = listOf(record(3, 2000), record(8, 2500))
        assertEquals(2000, History.goalOn(day(3), records, currentGoalMl = 3000))
        assertEquals(2000, History.goalOn(day(7), records, currentGoalMl = 3000))
        assertEquals(2500, History.goalOn(day(8), records, currentGoalMl = 3000))
        assertEquals(2500, History.goalOn(day(11), records, currentGoalMl = 3000))
        // Before any record: the earliest record.
        assertEquals(2000, History.goalOn(day(1), records, currentGoalMl = 3000))
        // No records at all: the current goal.
        assertEquals(3000, History.goalOn(day(1), emptyList(), currentGoalMl = 3000))
    }

    // HIST-006 invariant: today's goal change does not rewrite last week.
    @Test
    fun changingTheGoalTodayKeepsPastDays() {
        val records = listOf(record(1, 2000), record(11, 4000))
        val week = History.week(endDate = today, today = today, totals = mapOf(day(10) to 2100.0), records = records, currentGoalMl = 4000)
        val tenth = week.days.single { it.date == day(10) }
        assertEquals(2000, tenth.goalMl)
        assertTrue(tenth.met)
        assertEquals(4000, week.days.last().goalMl)
    }

    // HIST-001, HIST-004: seven days ending on the chosen day, met count and average.
    @Test
    fun weekSummary() {
        val totals = mapOf(day(5) to 2500.0, day(6) to 1000.0, day(11) to 2450.0, day(4) to 9999.0)
        val week = History.week(endDate = today, today = today, totals = totals, records = emptyList(), currentGoalMl = 2450)
        assertEquals((5..11).map { day(it) }, week.days.map { it.date })
        assertEquals(listOf(2500.0, 1000.0, 0.0, 0.0, 0.0, 0.0, 2450.0), week.days.map { it.totalMl })
        assertEquals(2, week.metCount)
        assertEquals(850.0, week.averageMl, 1e-9) // 5950 / 7 = 850
        assertEquals(1f, week.days.first().fraction, 0.0001f)
        assertEquals(1000f / 2450f, week.days[1].fraction, 0.0001f)
    }

    // HIST-001, HIST-005: navigating never extends past today.
    @Test
    fun windowNeverPassesToday() {
        val week = History.week(endDate = today.plusDays(3), today = today, totals = emptyMap(), records = emptyList(), currentGoalMl = 2000)
        assertEquals(today, week.days.last().date)
        assertFalse(History.canGoForward(today, today))
        assertTrue(History.canGoForward(today.minusDays(7), today))
        assertEquals(today.minusDays(7), History.previousEnd(today))
        assertEquals(today, History.nextEnd(today.minusDays(7), today))
        assertEquals(today, History.nextEnd(today.minusDays(3), today))
    }

    // HIST-002 with LOG-002: totals grouped by local date.
    @Test
    fun totalsByLocalDate() {
        val zone = ZoneId.of("Europe/London")
        fun millis(d: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 10, d, h, m).atZone(zone).toInstant().toEpochMilli()
        val entries = listOf(
            LoggedAmount(millis(10, 23, 59), 250.0),
            LoggedAmount(millis(11, 0, 0), 500.0),
            LoggedAmount(millis(11, 9), 250.0),
        )
        assertEquals(mapOf(day(10) to 250.0, day(11) to 750.0), History.totalsByDay(entries, zone))
    }

    // HIST-004 with the GOAL-008 invariant: totals and the average sum decimal ml, rounded once.
    @Test
    fun totalsAndAverageAreUnrounded() {
        val zone = ZoneId.of("Europe/London")
        val eight = Units.displayToMl(8, VolumeUnit.FL_OZ)
        val noon = LocalDateTime.of(2026, 10, 11, 12, 0).atZone(zone).toInstant().toEpochMilli()
        val entries = List(3) { LoggedAmount(noon + it, eight) }
        val totals = History.totalsByDay(entries, zone)
        assertEquals(3 * eight, totals.getValue(day(11)), 1e-9)
        assertEquals("24 fl oz", Units.formatVolume(totals.getValue(day(11)), VolumeUnit.FL_OZ))
        assertEquals("710 ml", Units.formatVolume(totals.getValue(day(11)), VolumeUnit.ML)) // 709.764, not 3 × 237
        val week = History.week(endDate = today, today = today, totals = totals, records = emptyList(), currentGoalMl = 2450)
        assertEquals(3 * eight / 7, week.averageMl, 1e-9)
    }
}
