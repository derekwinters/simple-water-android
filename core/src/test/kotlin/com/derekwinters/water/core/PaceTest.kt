package com.derekwinters.water.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/** Requirement IDs refer to docs/spec/reminders.md (REM-017–019) and docs/spec/screens.md (UI-013). */
class PaceTest {
    // 08:00–20:00, a 12-hour window.
    private val window = ReminderSettings.DEFAULT

    private fun t(hour: Int, minute: Int = 0, second: Int = 0) = LocalTime.of(hour, minute, second)

    // REM-017: linear ramp across the window.
    @Test
    fun expectedRampsLinearly() {
        assertEquals(0.0, Pace.at(t(8), 0, 2400, window)!!.expectedMl, 1e-9)
        assertEquals(1200.0, Pace.at(t(14), 0, 2400, window)!!.expectedMl, 1e-9)
        assertEquals(600.0, Pace.at(t(11), 0, 2400, window)!!.expectedMl, 1e-9)
        assertEquals(2400.0, Pace.at(t(20), 0, 2400, window)!!.expectedMl, 1e-9)
    }

    // REM-017 invariant: never negative, never above the goal.
    @Test
    fun expectedIsClampedToTheGoal() {
        assertEquals(0.0, Pace.at(t(6), 0, 2400, window)!!.expectedMl, 1e-9)
        assertEquals(0.0, Pace.at(t(0), 0, 2400, window)!!.expectedMl, 1e-9)
        assertEquals(2400.0, Pace.at(t(22), 0, 2400, window)!!.expectedMl, 1e-9)
        assertEquals(2400.0, Pace.at(t(23, 59, 59), 0, 2400, window)!!.expectedMl, 1e-9)
        for (minute in 0 until 24 * 60) {
            val e = Pace.at(t(minute / 60, minute % 60), 0, 2450, window.copy(startMinute = 7 * 60 + 30, endMinute = 9 * 60))!!.expectedMl
            assertTrue("$minute: $e", e in 0.0..2450.0)
        }
    }

    // REM-017: start == end means no ramp and no pace.
    @Test
    fun noPaceWhenStartEqualsEnd() {
        assertNull(Pace.at(t(12), 500, 2400, window.copy(startMinute = 720, endMinute = 720)))
        assertNull(PaceText.notificationBody(t(13), 500, 2400, window.copy(startMinute = 720, endMinute = 720), VolumeUnit.ML))
        assertNull(PaceText.homeLine(t(12), 500, 2400, window.copy(startMinute = 720, endMinute = 720), VolumeUnit.ML))
    }

    // REM-018: difference is total minus the unrounded expected amount.
    @Test
    fun differenceIsTotalMinusExpected() {
        assertEquals(-450.0, Pace.at(t(11), 150, 2400, window)!!.differenceMl, 1e-9)
        assertEquals(200.0, Pace.at(t(11), 800, 2400, window)!!.differenceMl, 1e-9)
    }

    // REM-018: nearest 10 ml, or whole fl oz; a half rounds away from zero.
    @Test
    fun roundingInBothUnits() {
        assertEquals(1000, Pace.rounded(1004.9, VolumeUnit.ML))
        assertEquals(1010, Pace.rounded(1005.0, VolumeUnit.ML))
        assertEquals(-250, Pace.rounded(-254.0, VolumeUnit.ML))
        assertEquals(-30, Pace.rounded(-25.0, VolumeUnit.ML))
        assertEquals(30, Pace.rounded(25.0, VolumeUnit.ML))
        assertEquals(0, Pace.rounded(-4.9, VolumeUnit.ML))
        assertEquals(34, Pace.rounded(1000.0, VolumeUnit.FL_OZ)) // 33.8
        assertEquals(-8, Pace.rounded(-250.0, VolumeUnit.FL_OZ)) // -8.45
        assertEquals(0, Pace.rounded(14.0, VolumeUnit.FL_OZ)) // 0.47
        assertEquals(-1, Pace.rounded(-Units.ML_PER_FL_OZ / 2, VolumeUnit.FL_OZ))
    }

    // REM-019: the pace sentence in both units, behind, ahead and on track.
    @Test
    fun notificationBody() {
        // 13:00 is 5/12 of 08:00–20:00, so 1,000 ml of a 2,400 ml goal.
        assertEquals(
            "Ideally you'd have had 1,000 ml by now to stay on track. You're 250 ml behind.",
            PaceText.notificationBody(t(13), 750, 2400, window, VolumeUnit.ML),
        )
        assertEquals(
            "Ideally you'd have had 1,000 ml by now to stay on track. You're 200 ml ahead.",
            PaceText.notificationBody(t(13), 1200, 2400, window, VolumeUnit.ML),
        )
        assertEquals(
            "Ideally you'd have had 1,000 ml by now to stay on track. You're right on track.",
            PaceText.notificationBody(t(13), 996, 2400, window, VolumeUnit.ML),
        )
        assertEquals(
            "Ideally you'd have had 34 fl oz by now to stay on track. You're 8 fl oz behind.",
            PaceText.notificationBody(t(13), 750, 2400, window, VolumeUnit.FL_OZ),
        )
        assertEquals(
            "Ideally you'd have had 34 fl oz by now to stay on track. You're right on track.",
            PaceText.notificationBody(t(13), 990, 2400, window, VolumeUnit.FL_OZ),
        )
    }

    // REM-019: the start-time slot (expected rounds to 0) keeps the plain text.
    @Test
    fun startSlotHasNoPaceSentence() {
        assertNull(PaceText.notificationBody(t(8), 0, 2400, window, VolumeUnit.ML))
        assertNull(PaceText.notificationBody(t(7), 0, 2400, window, VolumeUnit.ML))
        // 14 ml expected rounds to 0 fl oz.
        assertNull(PaceText.notificationBody(t(8, 4, 12), 0, 2400, window, VolumeUnit.FL_OZ))
    }

    // UI-013: the home line, only strictly inside the window and below the goal.
    @Test
    fun homeLine() {
        assertEquals("On pace: 1,000 ml by now · 250 ml behind", PaceText.homeLine(t(13), 750, 2400, window, VolumeUnit.ML))
        assertEquals("On pace: 1,000 ml by now · 200 ml ahead", PaceText.homeLine(t(13), 1200, 2400, window, VolumeUnit.ML))
        assertEquals("On pace: 1,000 ml by now · right on track", PaceText.homeLine(t(13), 1000, 2400, window, VolumeUnit.ML))
        assertEquals("On pace: 34 fl oz by now · 8 fl oz behind", PaceText.homeLine(t(13), 750, 2400, window, VolumeUnit.FL_OZ))
        assertNull(PaceText.homeLine(t(8), 0, 2400, window, VolumeUnit.ML))
        assertNull(PaceText.homeLine(t(7, 59), 0, 2400, window, VolumeUnit.ML))
        assertNull(PaceText.homeLine(t(20), 0, 2400, window, VolumeUnit.ML))
        assertNull(PaceText.homeLine(t(21), 0, 2400, window, VolumeUnit.ML))
        assertNull(PaceText.homeLine(t(13), 2400, 2400, window, VolumeUnit.ML))
        assertNull(PaceText.homeLine(t(13), 3000, 2400, window, VolumeUnit.ML))
        // UI-013: uses the window whether or not reminders are on.
        assertEquals(
            "On pace: 1,000 ml by now · 250 ml behind",
            PaceText.homeLine(t(13), 750, 2400, window.copy(enabled = false), VolumeUnit.ML),
        )
    }
}
