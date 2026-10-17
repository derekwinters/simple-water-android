package com.derekwinters.water.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Requirement IDs refer to docs/spec/logging.md. */
class LoggingTest {
    private val newYork = ZoneId.of("America/New_York")

    // LOG-002: a day is local midnight to local midnight.
    @Test
    fun dayWindowIsLocalMidnightToMidnight() {
        val date = LocalDate.of(2026, 10, 11)
        val start = DayWindow.startMillis(date, newYork)
        val end = DayWindow.endMillis(date, newYork)
        assertEquals(LocalDateTime.of(2026, 10, 11, 0, 0).atZone(newYork).toInstant().toEpochMilli(), start)
        assertEquals(24 * 60 * 60 * 1000L, end - start)
    }

    // LOG-002 invariant: a DST day is 23 or 25 hours, not "start + 24 h".
    @Test
    fun daylightSavingDaysAreNot24Hours() {
        val spring = LocalDate.of(2026, 3, 8)
        assertEquals(23 * 3_600_000L, DayWindow.endMillis(spring, newYork) - DayWindow.startMillis(spring, newYork))
        val autumn = LocalDate.of(2026, 11, 1)
        assertEquals(25 * 3_600_000L, DayWindow.endMillis(autumn, newYork) - DayWindow.startMillis(autumn, newYork))
    }

    // LOG-002: an entry belongs to the local date of its instant.
    @Test
    fun dateOfAnInstant() {
        val justBeforeMidnight = LocalDateTime.of(2026, 10, 11, 23, 59, 59).atZone(newYork).toInstant().toEpochMilli()
        assertEquals(LocalDate.of(2026, 10, 11), DayWindow.dateOf(justBeforeMidnight, newYork))
        assertEquals(LocalDate.of(2026, 10, 12), DayWindow.dateOf(justBeforeMidnight + 1000, newYork))
    }

    // LOG-001, LOG-006: an entry is 1–5,000 ml, entered in the volume unit.
    @Test
    fun entryAmountFromInput() {
        assertEquals(330.0, EntryRules.amountFromInput("330", VolumeUnit.ML)!!, 0.0)
        assertEquals(1.0, EntryRules.amountFromInput("1", VolumeUnit.ML)!!, 0.0)
        assertEquals(5000.0, EntryRules.amountFromInput("5000", VolumeUnit.ML)!!, 0.0)
        assertNull(EntryRules.amountFromInput("0", VolumeUnit.ML))
        assertNull(EntryRules.amountFromInput("5001", VolumeUnit.ML))
        assertNull(EntryRules.amountFromInput("", VolumeUnit.ML))
        assertEquals(354.882, EntryRules.amountFromInput("12", VolumeUnit.FL_OZ)!!, 1e-9) // GOAL-008: unrounded
        assertNull(EntryRules.amountFromInput("170", VolumeUnit.FL_OZ)) // 5,027 ml
    }

    // LOG-004: defaults 250 and 500 ml; 1–4 sizes of 10–2,000 ml; the first is the default.
    @Test
    fun drinkSizeDefaultsAndDecoding() {
        assertEquals(listOf(250.0, 500.0), DrinkSizes.DEFAULT)
        assertEquals(listOf(250.0, 500.0), DrinkSizes.decode(null))
        assertEquals(listOf(250.0, 500.0), DrinkSizes.decode(""))
        assertEquals(listOf(330.0, 750.0), DrinkSizes.decode(DrinkSizes.encode(listOf(330.0, 750.0))))
        // Invalid stored values are dropped; nothing valid left falls back to the defaults.
        assertEquals(listOf(330.0), DrinkSizes.decode("330,5,abc"))
        assertEquals(listOf(250.0, 500.0), DrinkSizes.decode("5,9999"))
        assertEquals(listOf(100.0, 200.0, 300.0, 400.0), DrinkSizes.decode("100,200,300,400,500"))
        // Sizes are decimal ml and survive a round trip unchanged.
        val ounces = listOf(8 * Units.ML_PER_FL_OZ, 16 * Units.ML_PER_FL_OZ)
        assertEquals(ounces, DrinkSizes.decode(DrinkSizes.encode(ounces)))
    }

    // LOG-009: blank fields are dropped; any invalid field or no size at all is rejected.
    @Test
    fun drinkSizesFromInputs() {
        assertNull(DrinkSizes.fromInputs(listOf("", "", "", ""), VolumeUnit.ML))
        assertNull(DrinkSizes.fromInputs(listOf("300", "5"), VolumeUnit.ML))
        assertNull(DrinkSizes.fromInputs(listOf("300", "2001"), VolumeUnit.ML))
        assertEquals(listOf(300.0, 600.0), DrinkSizes.fromInputs(listOf("300", "", "600", " "), VolumeUnit.ML))
        // GOAL-008: a custom "8" entered in fl oz is stored as 236.588 ml.
        val ounces = DrinkSizes.fromInputs(listOf("8", "16"), VolumeUnit.FL_OZ)!!
        assertEquals(236.588, ounces[0], 1e-9)
        assertEquals(473.176, ounces[1], 1e-9)
        assertTrue(DrinkSizes.isValid(10.0))
        assertFalse(DrinkSizes.isValid(9.99))
    }

    // LOG-004: defaults follow the unit while the sizes have not been customised.
    @Test
    fun quickAddDefaultsFollowTheUnit() {
        assertEquals(listOf(250.0, 500.0), DrinkSizes.quickAdd(DrinkSizes.DEFAULT, VolumeUnit.ML))
        val ounces = DrinkSizes.quickAdd(DrinkSizes.DEFAULT, VolumeUnit.FL_OZ)
        assertEquals(listOf("8 fl oz", "16 fl oz"), ounces.map { Units.formatVolume(it, VolumeUnit.FL_OZ) })
        assertEquals(8 * Units.ML_PER_FL_OZ, ounces[0], 1e-9)
        assertEquals(16 * Units.ML_PER_FL_OZ, ounces[1], 1e-9)
        // Customised sizes are kept, in either unit.
        val custom = listOf(330.0, 750.0)
        assertEquals(custom, DrinkSizes.quickAdd(custom, VolumeUnit.ML))
        assertEquals(listOf("11 fl oz", "25 fl oz"), DrinkSizes.quickAdd(custom, VolumeUnit.FL_OZ).map { Units.formatVolume(it, VolumeUnit.FL_OZ) })
    }

    // The issue this fixes: in fl oz, two "8 fl oz" quick-adds total 16 fl oz, not 17.
    @Test
    fun twoEightOunceQuickAddsTotalSixteen() {
        val eight = DrinkSizes.quickAdd(DrinkSizes.DEFAULT, VolumeUnit.FL_OZ).first()
        assertEquals("16 fl oz", Units.formatVolume(eight + eight, VolumeUnit.FL_OZ))
        assertEquals("800 fl oz", Units.formatVolume(List(100) { eight }.sum(), VolumeUnit.FL_OZ))
    }

    // LOG-005 invariant: a quick-add logs exactly the amount its label shows.
    @Test
    fun quickAddLogsWhatItsLabelShows() {
        // A size saved in ml, shown rounded in fl oz, logs the shown whole fl oz.
        assertEquals(listOf(11 * Units.ML_PER_FL_OZ), DrinkSizes.quickAdd(listOf(330.0), VolumeUnit.FL_OZ))
        // A size saved from fl oz, shown rounded in ml, logs the shown whole ml.
        assertEquals(listOf(237.0), DrinkSizes.quickAdd(listOf(8 * Units.ML_PER_FL_OZ), VolumeUnit.ML))
        for (unit in VolumeUnit.entries) {
            var size = DrinkSizes.MIN_ML.toDouble()
            while (size <= DrinkSizes.MAX_ML) {
                val logged = DrinkSizes.quickAdd(listOf(size), unit).single()
                val shown = Units.mlToDisplay(logged, unit)
                assertEquals(Units.mlToDisplay(size, unit), shown)
                assertEquals("$size in $unit", Units.displayToMl(shown, unit), logged, 0.0)
                size += 0.7
            }
        }
    }
}
