package com.derekwinters.water.core

import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Requirement IDs refer to docs/spec/screens.md (UI-015, UI-018, UI-020, UI-021, UI-022, UI-023
 * and the widget invariants) and docs/spec/logging.md (LOG-002, LOG-003, LOG-013, LOG-014).
 */
class WidgetCupTest {
    private val zone = ZoneId.of("Europe/London")
    private val goal = 2450

    // 08:00–20:00 every 60 minutes: thirteen reminder times, twelve intervals.
    private val reminders = ReminderSettings.DEFAULT

    private val percent = WidgetSettings(text = CupTextMode.PERCENT)
    private val amount = WidgetSettings(text = CupTextMode.AMOUNT)

    // Sunday 11 October 2026.
    private fun at(hour: Int, minute: Int = 0, day: Int = 11): ZonedDateTime =
        ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone)

    private fun entry(time: ZonedDateTime, ml: Double) = LoggedAmount(time.toInstant().toEpochMilli(), ml)

    private fun pace(now: ZonedDateTime, settings: ReminderSettings = reminders) =
        Pace.today(settings, now.toLocalDateTime(), goal)

    private fun cup(totalMl: Double, settings: WidgetSettings, unit: VolumeUnit = VolumeUnit.ML, goalMl: Int = goal) =
        WidgetCup.of(totalMl, goalMl, null, unit, settings)

    private fun value(state: CupState): CupLabel.Value = state.label as CupLabel.Value

    // UI-023: None by default, ideal line on, background off and cup fill on by default.
    @Test
    fun theDefaultsAreNoTextAndTheIdealLine() {
        assertEquals(CupTextMode.NONE, WidgetSettings().text)
        assertTrue(WidgetSettings().idealLine)
        assertFalse(WidgetSettings().background)
        assertTrue(WidgetSettings().cupFill)
        assertEquals(listOf("None", "Percent", "Amount"), CupTextMode.entries.map { it.label })
    }

    // Widget invariant, UI-015, UI-020: water and ideal line are home's ring fractions at the same moment.
    @Test
    fun waterAndIdealLineEqualHomesRingsAtTheSameMoment() {
        val entries = listOf(entry(at(9, 15), 250.0), entry(at(11, 40), 500.0), entry(at(12, 10), 236.588))
        for (now in listOf(at(7, 0), at(8, 0), at(12, 30), at(19, 59), at(20, 0), at(23, 59))) {
            val upToNow = entries.filter { it.loggedAtMillis <= now.toInstant().toEpochMilli() }
            val rings = Rings.today(upToNow, now, goal, reminders)
            val cup = WidgetCup.today(upToNow, now, goal, reminders, VolumeUnit.ML, WidgetSettings())
            assertEquals("at $now", rings.fraction, cup.water)
            assertEquals("at $now", rings.outerFraction, cup.ideal)
        }
    }

    // UI-015, LOG-003: empty, partway, at the goal and over it; the water is clamped to full.
    @Test
    fun theWaterIsTheTotalAgainstTheGoalClampedToFull() {
        assertEquals(0f, cup(0.0, percent).water, 0f)
        assertEquals(0.5f, cup(1225.0, percent).water, 1e-6f)
        assertEquals(1f, cup(2450.0, percent).water, 0f)
        assertEquals(1f, cup(3100.0, percent).water, 0f)
    }

    // UI-020, LOG-014: no ideal amount hides the line; so does the setting.
    @Test
    fun theIdealLineIsHiddenWithNoIdealAmountOrWhenTurnedOff() {
        val now = at(12, 30)
        assertEquals(4f / 12, WidgetCup.of(500.0, goal, pace(now), VolumeUnit.ML, WidgetSettings()).ideal!!, 1e-6f)
        assertNull(WidgetCup.of(500.0, goal, pace(now), VolumeUnit.ML, WidgetSettings(idealLine = false)).ideal)
        val none = listOf(
            reminders.copy(enabled = false),
            reminders.copy(days = setOf(DayOfWeek.MONDAY)),
            reminders.copy(startMinute = 9 * 60, endMinute = 9 * 60),
        )
        for (settings in none) {
            assertNull(WidgetCup.of(500.0, goal, pace(now, settings), VolumeUnit.ML, WidgetSettings()).ideal)
        }
        // Before the first reminder time the line is shown, at the bottom.
        assertEquals(0f, WidgetCup.of(0.0, goal, pace(at(7, 0)), VolumeUnit.ML, WidgetSettings()).ideal!!, 0f)
    }

    // UI-021: None writes nothing, before or after the goal (UI-022).
    @Test
    fun noneWritesNothing() {
        for (total in listOf(0.0, 1000.0, 2450.0, 4000.0)) {
            assertEquals(CupLabel.None, cup(total, WidgetSettings()).label)
        }
        assertTrue(cup(2450.0, WidgetSettings()).goalMet)
    }

    // UI-021: percent rounds down to a whole number with a % sign.
    @Test
    fun percentRoundsDown() {
        assertEquals(CupLabel.Value("0%", null), cup(0.0, percent).label)
        assertEquals(CupLabel.Value("45%", null), cup(1102.5, percent).label)
        assertEquals(CupLabel.Value("45%", null), cup(1126.0, percent).label) // 46.0% less 1 ml
        assertEquals(CupLabel.Value("99%", null), cup(0.996 * goal, percent).label)
        assertEquals(CupLabel.Value("99%", null), cup(goal - 0.001, percent).label)
    }

    // UI-021: fl oz rounds down to a whole number, with the unit under it.
    @Test
    fun fluidOuncesRoundDown() {
        assertEquals(CupLabel.Value("84", "fl oz"), value(cup(84.6 * Units.ML_PER_FL_OZ, amount, VolumeUnit.FL_OZ, goalMl = 3000)))
        assertEquals(CupLabel.Value("0", "fl oz"), value(cup(0.0, amount, VolumeUnit.FL_OZ)))
        // Three 1 fl oz quick-adds are stored as decimal ml (LOG-012) and still show 3.
        val three = Units.displayToMl(1, VolumeUnit.FL_OZ) + Units.displayToMl(1, VolumeUnit.FL_OZ) + Units.displayToMl(1, VolumeUnit.FL_OZ)
        assertEquals(CupLabel.Value("3", "fl oz"), value(cup(three, amount, VolumeUnit.FL_OZ)))
    }

    // UI-021: ml users see litres, rounded down to one decimal.
    @Test
    fun litresRoundDownToOneDecimal() {
        assertEquals(CupLabel.Value("0.8", "L"), value(cup(850.0, amount)))
        assertEquals(CupLabel.Value("1.1", "L"), value(cup(1100.0, amount)))
        assertEquals(CupLabel.Value("1.1", "L"), value(cup(1199.9, amount)))
        assertEquals(CupLabel.Value("0.0", "L"), value(cup(99.0, amount)))
        assertEquals(CupLabel.Value("2.3", "L"), value(cup(2399.0, amount)))
    }

    // UI-022, invariant: at the goal the text becomes a check and stays one over the goal.
    @Test
    fun atTheGoalTheTextBecomesACheckAndStays() {
        for (settings in listOf(percent, amount)) {
            for (unit in VolumeUnit.entries) {
                assertFalse(cup(goal - 0.001, settings, unit).goalMet)
                for (total in listOf(goal.toDouble(), goal + 0.001, 3200.0, 9000.0)) {
                    val state = cup(total, settings, unit)
                    assertTrue(state.goalMet)
                    assertEquals("$settings $unit $total", CupLabel.Check, state.label)
                }
            }
        }
    }

    // Invariant: the goal is met exactly when home says "Goal met" — no remaining amount.
    @Test
    fun goalMetIsHomesGoalMet() {
        for (total in listOf(0.0, 1000.0, 2449.999, 2450.0, 2450.001, 5000.0)) {
            assertEquals("$total", GoalMath.remainingMl(total, goal) == 0.0, cup(total, percent).goalMet)
        }
    }

    // Invariant: before the goal is met the number never shows the goal or 100%.
    @Test
    fun theNumberNeverShowsTheGoalEarly() {
        // fl oz: a 2,514 ml goal is "85 fl oz" on home; 85 fl oz of drinks is 2,513.7 ml, short of it.
        val custom = 2514
        assertEquals(85, Units.mlToDisplay(custom, VolumeUnit.FL_OZ))
        val eightyFive = 85 * Units.ML_PER_FL_OZ
        assertFalse(cup(eightyFive, amount, VolumeUnit.FL_OZ, goalMl = custom).goalMet)
        assertEquals(CupLabel.Value("84", "fl oz"), cup(eightyFive, amount, VolumeUnit.FL_OZ, goalMl = custom).label)
        // Litres: a 2,449 ml goal is 2.4 L to the nearest 0.1; 2,420 ml shows 2.3, not 2.4.
        assertEquals(CupLabel.Value("2.3", "L"), cup(2420.0, amount, goalMl = 2449).label)
        // Percent: never 100 before the goal, however close.
        for (short in listOf(0.5, 0.01, 1e-9)) {
            val label = cup(goal - short, percent).label as CupLabel.Value
            assertEquals("$short short", "99%", label.number)
        }
        // Sweep: below the goal, no text is the goal's own number in that unit.
        for (ml in 0 until goal step 7) {
            assertTrue(cup(ml.toDouble(), percent).label != CupLabel.Value("100%", null))
            assertTrue(cup(ml.toDouble(), amount, VolumeUnit.FL_OZ).label != CupLabel.Value("83", "fl oz"))
            assertTrue(cup(ml.toDouble(), amount).label != CupLabel.Value("2.5", "L"))
        }
    }

    // Widget invariant, LOG-002: just after midnight the cup is empty and the check is gone.
    @Test
    fun atMidnightTheCupEmpties() {
        val yesterday = listOf(entry(at(9), 1500.0), entry(at(21), 1200.0))
        val late = WidgetCup.today(yesterday, at(23, 59), goal, reminders, VolumeUnit.ML, percent)
        assertEquals(CupLabel.Check, late.label)
        val midnight = WidgetCup.today(yesterday, at(0, 0, day = 12), goal, reminders, VolumeUnit.ML, percent)
        assertEquals(0f, midnight.water, 0f)
        assertFalse(midnight.goalMet)
        assertEquals(CupLabel.Value("0%", null), midnight.label)
    }

    // UI-015, UI-023: the cup carries the background and cup fill settings to the widget as set.
    @Test
    fun theCupCarriesTheBackgroundAndCupFill() {
        val drawn = cup(500.0, WidgetSettings())
        assertFalse(drawn.background)
        assertTrue(drawn.cupFill)
        for (background in listOf(false, true)) {
            for (cupFill in listOf(false, true)) {
                val state = cup(500.0, WidgetSettings(background = background, cupFill = cupFill))
                assertEquals(background, state.background)
                assertEquals(cupFill, state.cupFill)
                // The settings change only how the cup is drawn, never the values in it.
                assertEquals(drawn.copy(background = background, cupFill = cupFill), state)
            }
        }
    }

    // UI-018: what a refresh stores reads back unchanged.
    @Test
    fun theCupSurvivesEncodingForTheWidget() {
        val states = listOf(
            cup(0.0, WidgetSettings()),
            cup(0.0, WidgetSettings(background = true, cupFill = false)),
            cup(1100.0, WidgetSettings(text = CupTextMode.PERCENT, background = true)),
            cup(1100.0, amount),
            cup(1100.0, amount, VolumeUnit.FL_OZ),
            cup(1100.0, percent),
            cup(3000.0, percent),
            WidgetCup.of(700.0, goal, pace(at(12, 30)), VolumeUnit.ML, amount),
        )
        for (state in states) assertEquals(state, CupState.decode(state.encode()))
        assertNull(CupState.decode("not a cup"))
        assertNull(CupState.decode(null))
    }
}
