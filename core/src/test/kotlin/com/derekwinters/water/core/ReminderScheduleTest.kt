package com.derekwinters.water.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/** Requirement IDs refer to docs/spec/reminders.md. */
class ReminderScheduleTest {
    private val defaults = ReminderSettings.DEFAULT

    // Sunday 11 October 2026.
    private fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 10, day, hour, minute)

    // REM-002: on, every day, 08:00–20:00, every 60 minutes.
    @Test
    fun defaults() {
        assertTrue(defaults.enabled)
        assertEquals(DayOfWeek.entries.toSet(), defaults.days)
        assertEquals(8 * 60, defaults.startMinute)
        assertEquals(20 * 60, defaults.endMinute)
        assertEquals(60, defaults.intervalMinutes)
        assertEquals(listOf(30, 60, 90, 120), ReminderSettings.INTERVALS)
    }

    // REM-004: start, every interval, end inclusive when on the grid.
    @Test
    fun slotsIncludeTheEndWhenOnTheGrid() {
        val slots = ReminderSchedule.slotsFor(defaults.copy(intervalMinutes = 90))
        assertEquals(LocalTime.of(8, 0), slots.first())
        assertEquals(LocalTime.of(20, 0), slots.last())
        assertEquals(9, slots.size)
        val shorter = ReminderSchedule.slotsFor(defaults.copy(endMinute = 19 * 60, intervalMinutes = 90))
        assertEquals(LocalTime.of(18, 30), shorter.last())
        assertEquals(13, ReminderSchedule.slotsFor(defaults).size)
    }

    // REM-003: start == end gives one reminder a day.
    @Test
    fun equalStartAndEndIsOneSlot() {
        assertEquals(listOf(LocalTime.of(12, 0)), ReminderSchedule.slotsFor(defaults.copy(startMinute = 720, endMinute = 720)))
    }

    // REM-006: the first slot strictly after now.
    @Test
    fun nextIsStrictlyAfterNow() {
        assertEquals(at(11, 11), ReminderSchedule.next(at(11, 10), defaults))
        assertEquals(at(11, 11), ReminderSchedule.next(at(11, 10, 30), defaults))
        assertEquals(at(11, 8), ReminderSchedule.next(at(11, 6), defaults))
        // After the last slot, the first slot of the next day.
        assertEquals(at(12, 8), ReminderSchedule.next(at(11, 20), defaults))
    }

    // REM-005: only active days; none active or reminders off means no alarm.
    @Test
    fun onlyActiveDays() {
        val weekdays = defaults.copy(days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
        // Sunday 21:00 → Monday 08:00.
        assertEquals(at(12, 8), ReminderSchedule.next(at(11, 21), weekdays))
        // Saturday 10 Oct 09:00 → Monday.
        assertEquals(at(12, 8), ReminderSchedule.next(at(10, 9), weekdays))
        assertNull(ReminderSchedule.next(at(11, 9), defaults.copy(days = emptySet())))
        assertNull(ReminderSchedule.next(at(11, 9), defaults.copy(enabled = false)))
    }

    // REM-006: one active day a week is still found (at most 8 days ahead).
    @Test
    fun oneDayAWeek() {
        val sundays = defaults.copy(days = setOf(DayOfWeek.SUNDAY))
        assertEquals(LocalDateTime.of(2026, 10, 18, 8, 0), ReminderSchedule.next(at(11, 21), sundays))
    }

    // REM-003: start past end moves end; end before start moves start; 30-minute steps 00:00–23:30.
    @Test
    fun startAndEndStayOrdered() {
        val later = ReminderSchedule.withStart(defaults, 21 * 60)
        assertEquals(21 * 60, later.startMinute)
        assertEquals(21 * 60, later.endMinute)
        val earlier = ReminderSchedule.withEnd(defaults, 7 * 60)
        assertEquals(7 * 60, earlier.startMinute)
        assertEquals(7 * 60, earlier.endMinute)
        assertEquals(0, ReminderSchedule.withStart(defaults, -30).startMinute)
        assertEquals(23 * 60 + 30, ReminderSchedule.withEnd(defaults, 24 * 60).endMinute)
    }

    // Days are stored as a bit mask, Monday = bit 0.
    @Test
    fun dayMaskRoundTrip() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)
        assertEquals(0b1000001, ReminderSchedule.daysToMask(days))
        assertEquals(days, ReminderSchedule.maskToDays(0b1000001))
        assertEquals(DayOfWeek.entries.toSet(), ReminderSchedule.maskToDays(ReminderSchedule.daysToMask(DayOfWeek.entries.toSet())))
    }

    // REM-010, REM-011: when a delivered alarm becomes a notification.
    @Test
    fun deliveryPolicy() {
        fun notify(
            handled: Boolean = false,
            enabled: Boolean = true,
            dayActive: Boolean = true,
            lateMillis: Long = 0,
            total: Int = 1000,
            goal: Int = 2450,
        ) = ReminderPolicy.shouldNotify(handled, enabled, dayActive, lateMillis, total, goal)

        assertTrue(notify())
        assertFalse(notify(handled = true))
        assertFalse(notify(enabled = false))
        assertFalse(notify(dayActive = false))
        assertTrue(notify(lateMillis = 30 * 60 * 1000L))
        assertFalse(notify(lateMillis = 30 * 60 * 1000L + 1))
        assertFalse(notify(total = 2450))
        assertFalse(notify(total = 3000))
        assertTrue(notify(total = 2449))
    }

    // REM-012: notification text.
    @Test
    fun notificationText() {
        assertEquals("Time for some water", ReminderText.TITLE)
        assertEquals("1,250 of 2,450 ml today · 1,200 ml to go", ReminderText.body(1250, 2450, VolumeUnit.ML))
        assertEquals("0 of 83 fl oz today · 83 fl oz to go", ReminderText.body(0, 2450, VolumeUnit.FL_OZ))
        assertEquals("Log 250 ml", ReminderText.logAction(250, VolumeUnit.ML))
        assertEquals("Log 8 fl oz", ReminderText.logAction(250, VolumeUnit.FL_OZ))
    }
}
