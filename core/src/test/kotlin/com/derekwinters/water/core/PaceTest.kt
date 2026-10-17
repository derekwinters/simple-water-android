package com.derekwinters.water.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/** Requirement IDs refer to docs/spec/logging.md (LOG-013–015) and docs/spec/screens.md (UI-013). */
class PaceTest {
    private val goal = 2450

    // 08:00–20:00 every 60 minutes: thirteen reminder times, twelve intervals.
    private val defaults = ReminderSettings.DEFAULT

    // Sunday 11 October 2026.
    private fun at(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 10, 11, hour, minute)

    private fun state(now: LocalDateTime, settings: ReminderSettings = defaults, goalMl: Int = goal): PaceState =
        requireNotNull(Pace.today(settings, now, goalMl)) { "expected an ideal amount at $now" }

    private val hhmm: (LocalTime) -> String = { "%d:%02d".format(it.hour, it.minute) }

    // LOG-013: 0 before the first reminder time.
    @Test
    fun idealIsZeroBeforeTheFirstReminderTime() {
        val pace = state(at(7, 59))
        assertEquals(0.0, pace.idealMl, 0.0)
        assertEquals(0f, pace.idealFraction, 0f)
        assertNull(pace.lastPassed)
        assertEquals(LocalTime.of(8, 0), pace.firstTime)
    }

    // LOG-013: k = 0 at t0 itself, so still 0.
    @Test
    fun idealIsZeroAtTheFirstReminderTime() {
        val pace = state(at(8, 0))
        assertEquals(0.0, pace.idealMl, 0.0)
        assertEquals(LocalTime.of(8, 0), pace.lastPassed)
    }

    // LOG-013: goal × k / n once tk has passed, steady until the next reminder time.
    @Test
    fun idealStepsAtReminderTimesAndHoldsBetween() {
        assertEquals(2450.0 / 12, state(at(9, 0)).idealMl, 1e-9)
        assertEquals(2450.0 / 12, state(at(9, 59)).idealMl, 1e-9)
        assertEquals(2450.0 * 4 / 12, state(at(12, 0)).idealMl, 1e-9)
        assertEquals(2450.0 * 4 / 12, state(at(12, 30)).idealMl, 1e-9)
        assertEquals(LocalTime.of(12, 0), state(at(12, 30)).lastPassed)
        assertEquals(4f / 12, state(at(12, 30)).idealFraction, 1e-6f)
    }

    // LOG-013: equals the goal from tn on, never above it.
    @Test
    fun idealIsTheGoalFromTheLastReminderTimeOn() {
        assertEquals(2450.0, state(at(20, 0)).idealMl, 0.0)
        assertEquals(2450.0, state(at(23, 59)).idealMl, 0.0)
        assertEquals(1f, state(at(23, 59)).idealFraction, 0f)
    }

    // LOG-013, REM-004: the last reminder time is the last on the grid, not the end setting.
    @Test
    fun idealFollowsTheGridWhenTheEndIsOffIt() {
        // 08:00–19:00 every 90 min → 08:00, 09:30, …, 18:30: n = 7.
        val settings = defaults.copy(endMinute = 19 * 60, intervalMinutes = 90)
        assertEquals(2450.0 / 7, state(at(9, 30), settings).idealMl, 1e-9)
        assertEquals(2450.0, state(at(18, 30), settings).idealMl, 0.0)
    }

    // LOG-013 invariant: never interpolated, never below 0 or above the goal, through the whole day.
    @Test
    fun idealOnlyChangesAtReminderTimes() {
        val slots = ReminderSchedule.slotsFor(defaults).toSet()
        var previous = state(at(0, 0)).idealMl
        for (minute in 1 until 24 * 60) {
            val ideal = state(at(minute / 60, minute % 60)).idealMl
            if (ideal != previous) {
                assertTrue("changed at minute $minute", LocalTime.of(minute / 60, minute % 60) in slots)
            }
            assertTrue("out of range at minute $minute", ideal in 0.0..2450.0)
            previous = ideal
        }
    }

    // UI-013: a tick at each reminder time, as a fraction of the circle (t0 at 12 o'clock; tn is t0).
    @Test
    fun ticksMarkEachReminderTime() {
        val ticks = state(at(12)).tickFractions
        assertEquals(12, ticks.size)
        assertEquals(0f, ticks.first(), 0f)
        assertEquals(11f / 12, ticks.last(), 1e-6f)
    }

    // LOG-014: hidden when reminders are off.
    @Test
    fun noIdealWhenRemindersAreOff() {
        assertNull(Pace.today(defaults.copy(enabled = false), at(12), goal))
    }

    // LOG-014: hidden when today is not an active reminder day.
    @Test
    fun noIdealOnAnInactiveDay() {
        val weekdays = defaults.copy(days = DayOfWeek.entries.toSet() - DayOfWeek.SUNDAY)
        assertNull(Pace.today(weekdays, at(12), goal))
        assertNotNull(Pace.today(weekdays, at(12).plusDays(1), goal))
    }

    // LOG-014: hidden with fewer than two reminder times.
    @Test
    fun noIdealWithASingleReminderTime() {
        assertNull(Pace.today(defaults.copy(startMinute = 12 * 60, endMinute = 12 * 60), at(13), goal))
        // 08:00–08:30 every 60 minutes → only 08:00.
        assertNull(Pace.today(defaults.copy(endMinute = 8 * 60 + 30), at(13), goal))
        // 08:00–09:00 every 60 minutes → two times, one interval.
        assertEquals(2450.0, state(at(9), defaults.copy(endMinute = 9 * 60)).idealMl, 0.0)
    }

    // LOG-015: behind, ahead, rounded to the nearest 10 ml.
    @Test
    fun behindAndAheadInMl() {
        // Ideal 816.67 at 12:00.
        assertEquals("220 ml behind", Pace.status(600.0, 2450.0 * 4 / 12, VolumeUnit.ML))
        assertEquals("370 ml ahead", Pace.status(1186.67, 2450.0 * 4 / 12, VolumeUnit.ML))
    }

    // LOG-015: "On track" when the size rounds to 0; an exact half rounds up.
    @Test
    fun onTrackWhenTheDifferenceRoundsToZero() {
        assertEquals("On track", Pace.status(816.67, 2450.0 * 4 / 12, VolumeUnit.ML))
        assertEquals("On track", Pace.status(500.0, 504.9, VolumeUnit.ML))
        assertEquals("On track", Pace.status(504.9, 500.0, VolumeUnit.ML))
        assertEquals("10 ml behind", Pace.status(500.0, 505.0, VolumeUnit.ML))
        assertEquals("10 ml ahead", Pace.status(505.0, 500.0, VolumeUnit.ML))
    }

    // LOG-015: in fl oz, to the nearest whole fl oz.
    @Test
    fun behindAndAheadInFlOz() {
        val ideal = 2450.0 * 4 / 12 // 27.6 fl oz; 216.67 ml behind is 7.3 fl oz
        assertEquals("7 fl oz behind", Pace.status(600.0, ideal, VolumeUnit.FL_OZ))
        assertEquals("On track", Pace.status(ideal - 14.0, ideal, VolumeUnit.FL_OZ))
        assertEquals("1 fl oz ahead", Pace.status(ideal + 15.0, ideal, VolumeUnit.FL_OZ))
    }

    // LOG-015: the ideal amount is shown with the same rounding.
    @Test
    fun idealAmountRounding() {
        assertEquals("820 ml", Pace.formatAmount(816.67, VolumeUnit.ML))
        assertEquals("2,450 ml", Pace.formatAmount(2450.0, VolumeUnit.ML))
        assertEquals("10 ml", Pace.formatAmount(5.0, VolumeUnit.ML))
        assertEquals("0 ml", Pace.formatAmount(0.0, VolumeUnit.ML))
        assertEquals("28 fl oz", Pace.formatAmount(816.67, VolumeUnit.FL_OZ))
    }

    // UI-013: the two lines after the first reminder time; "to go" keeps LOG-003's rounding.
    @Test
    fun linesAfterTheFirstReminderTime() {
        val text = Pace.text(state(at(12, 30)), totalMl = 600.0, unit = VolumeUnit.ML, formatTime = hhmm)
        assertEquals(PaceText("220 ml behind", "Ideal by 12:00: 820 ml · 1,850 ml to go"), text)
        val odd = Pace.text(state(at(12, 30)), totalMl = 605.0, unit = VolumeUnit.ML, formatTime = hhmm)
        assertEquals("Ideal by 12:00: 820 ml · 1,845 ml to go", odd?.detail)
    }

    // UI-013: before the first reminder time.
    @Test
    fun linesBeforeTheFirstReminderTime() {
        val text = Pace.text(state(at(7)), totalMl = 0.0, unit = VolumeUnit.ML, formatTime = hhmm)
        assertEquals(PaceText("Ideal starts at 8:00", "2,450 ml to go"), text)
    }

    // UI-013: in fl oz.
    @Test
    fun linesInFlOz() {
        val text = Pace.text(state(at(12)), totalMl = 600.0, unit = VolumeUnit.FL_OZ, formatTime = hhmm)
        assertEquals(PaceText("7 fl oz behind", "Ideal by 12:00: 28 fl oz · 63 fl oz to go"), text)
    }

    // UI-013: once the goal is met, no pace lines ("Goal met" alone, UI-004).
    @Test
    fun noLinesOnceTheGoalIsMet() {
        assertNull(Pace.text(state(at(12)), totalMl = 2450.0, unit = VolumeUnit.ML, formatTime = hhmm))
        assertNull(Pace.text(state(at(7)), totalMl = 3000.0, unit = VolumeUnit.ML, formatTime = hhmm))
    }
}
