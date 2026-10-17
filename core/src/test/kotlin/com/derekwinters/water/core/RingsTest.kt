package com.derekwinters.water.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Requirement IDs refer to docs/spec/screens.md (UI-013, UI-015, UI-018 and the widget
 * invariants) and docs/spec/logging.md (LOG-002, LOG-003, LOG-013, LOG-014).
 */
class RingsTest {
    private val zone = ZoneId.of("Europe/London")
    private val goal = 2450

    // 08:00–20:00 every 60 minutes: thirteen reminder times, twelve intervals.
    private val reminders = ReminderSettings.DEFAULT

    // Sunday 11 October 2026.
    private fun at(hour: Int, minute: Int = 0, day: Int = 11): ZonedDateTime =
        ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone)

    private fun entry(time: ZonedDateTime, ml: Double) = LoggedAmount(time.toInstant().toEpochMilli(), ml)

    /** The rings worked out from home's building blocks: LOG-003's fraction and the ideal amount's. */
    private fun home(totalMl: Double, now: ZonedDateTime, settings: ReminderSettings = reminders): RingState {
        val pace = Pace.today(settings, now.toLocalDateTime(), goal)
        return RingState(GoalMath.fraction(totalMl, goal), pace?.idealFraction, pace?.tickFractions.orEmpty())
    }

    // LOG-003, UI-013: the inner ring is the total against the goal; the outer ring is the ideal amount.
    @Test
    fun ringsAreTheTotalAgainstTheGoalAndTheIdealAmount() {
        val pace = Pace.today(reminders, LocalDateTime.of(2026, 10, 11, 12, 30), goal)!!
        val rings = Rings.of(1225.0, goal, pace)
        assertEquals(0.5f, rings.fraction, 1e-6f)
        assertEquals(4f / 12, rings.outerFraction!!, 1e-6f)
        assertEquals(pace.tickFractions, rings.outerTicks)
    }

    // Widget invariant: the widget's rings equal home's for the same day, total, goal and time.
    @Test
    fun widgetRingsEqualHomeRingsAtTheSameMoment() {
        val entries = listOf(entry(at(9, 15), 250.0), entry(at(11, 40), 500.0), entry(at(12, 10), 236.588))
        for (now in listOf(at(7, 0), at(8, 0), at(12, 30), at(19, 59), at(20, 0), at(23, 59))) {
            val total = entries.filter { it.loggedAtMillis <= now.toInstant().toEpochMilli() }
            assertEquals(
                "at $now",
                home(total.sumOf { it.amountMl }, now),
                Rings.today(total, now, goal, reminders),
            )
        }
    }

    // UI-015, LOG-013: before the first reminder time the outer ring is shown, empty.
    @Test
    fun beforeTheFirstReminderTimeTheOuterRingIsEmpty() {
        val rings = Rings.today(listOf(entry(at(6, 30), 300.0)), at(7, 59), goal, reminders)
        assertEquals(0f, rings.outerFraction!!, 0f)
        assertEquals(12, rings.outerTicks.size)
        assertEquals(300f / goal, rings.fraction, 1e-6f)
    }

    // UI-015, LOG-003: at or above the goal the inner ring is full, never more.
    @Test
    fun goalMetFillsTheInnerRing() {
        val entries = listOf(entry(at(9), 2000.0), entry(at(10), 1000.0))
        assertEquals(1f, Rings.today(entries, at(10, 30), goal, reminders).fraction, 0f)
        assertEquals(1f, Rings.today(entries.take(1) + entry(at(10), 450.0), at(10, 30), goal, reminders).fraction, 0f)
    }

    // Widget invariant: never a previous day's rings. Just after midnight both rings reset.
    @Test
    fun atMidnightBothRingsReset() {
        val yesterday = listOf(entry(at(9), 1500.0), entry(at(21), 900.0), entry(at(23, 59), 50.0))
        val late = Rings.today(yesterday, at(23, 59), goal, reminders)
        assertEquals(1f, late.outerFraction!!, 0f)
        assertEquals(2450f / goal, late.fraction, 1e-6f)

        val midnight = Rings.today(yesterday, at(0, 0, day = 12), goal, reminders)
        assertEquals(0f, midnight.fraction, 0f)
        assertEquals(0f, midnight.outerFraction!!, 0f)
    }

    // LOG-002: an entry belongs to the local date of its instant, also on a daylight-saving day.
    @Test
    fun onlyTheEntriesOfTheLocalDateCount() {
        // 25 October 2026: the clocks go back in London, so the day is 25 hours long.
        val dstDay = ZonedDateTime.of(2026, 10, 25, 23, 30, 0, 0, zone)
        val entries = listOf(
            entry(ZonedDateTime.of(2026, 10, 24, 23, 59, 0, 0, zone), 400.0),
            entry(ZonedDateTime.of(2026, 10, 25, 0, 30, 0, 0, zone), 245.0),
            entry(ZonedDateTime.of(2026, 10, 25, 1, 30, 0, 0, zone).withLaterOffsetAtOverlap(), 245.0),
            entry(ZonedDateTime.of(2026, 10, 26, 0, 0, 0, 0, zone), 1000.0),
        )
        assertEquals(490f / goal, Rings.today(entries, dstDay, goal, reminders).fraction, 1e-6f)
    }

    // LOG-014, UI-015: with no ideal amount the outer ring and its ticks are hidden.
    @Test
    fun theOuterRingIsHiddenWhenThereIsNoIdealAmount() {
        val entries = listOf(entry(at(9), 500.0))
        val hidden = listOf(
            "reminders off" to reminders.copy(enabled = false),
            "today not an active day" to reminders.copy(days = setOf(DayOfWeek.MONDAY)),
            "one reminder time" to reminders.copy(startMinute = 9 * 60, endMinute = 9 * 60),
            "interval longer than the span" to reminders.copy(startMinute = 9 * 60, endMinute = 10 * 60, intervalMinutes = 90),
        )
        for ((why, settings) in hidden) {
            val rings = Rings.today(entries, at(12), goal, settings)
            assertNull(why, rings.outerFraction)
            assertTrue(why, rings.outerTicks.isEmpty())
            assertEquals(why, 500f / goal, rings.fraction, 1e-6f)
            assertEquals(why, home(500.0, at(12), settings), rings)
        }
    }

    // UI-018: the midnight refresh is at the start of the next local date, never "now + 24 hours".
    @Test
    fun theNextRefreshAtMidnightIsTheStartOfTheNextLocalDate() {
        assertEquals(
            ZonedDateTime.of(2026, 10, 12, 0, 0, 0, 0, zone).toInstant().toEpochMilli(),
            Rings.nextMidnightMillis(at(18, 45)),
        )
        // The day the clocks go back: 25 hours from the start of 25 October.
        val start = ZonedDateTime.of(2026, 10, 25, 0, 0, 0, 0, zone)
        assertEquals(
            start.toInstant().toEpochMilli() + 25 * 3_600_000L,
            Rings.nextMidnightMillis(start),
        )
        assertEquals(DayWindow.endMillis(LocalDate.of(2026, 10, 25), zone), Rings.nextMidnightMillis(start))
    }

    // UI-018: the rings a refresh stores read back unchanged.
    @Test
    fun ringsSurviveEncodingForTheWidget() {
        val shown = Rings.today(listOf(entry(at(9), 700.0)), at(12, 30), goal, reminders)
        assertEquals(shown, RingState.decode(shown.encode()))
        val hidden = Rings.today(listOf(entry(at(9), 700.0)), at(12, 30), goal, reminders.copy(enabled = false))
        assertEquals(hidden, RingState.decode(hidden.encode()))
        assertNull(RingState.decode("not rings"))
        assertNull(RingState.decode(null))
    }
}
