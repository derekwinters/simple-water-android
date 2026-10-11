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
        assertEquals(330, EntryRules.amountFromInput("330", VolumeUnit.ML))
        assertEquals(1, EntryRules.amountFromInput("1", VolumeUnit.ML))
        assertEquals(5000, EntryRules.amountFromInput("5000", VolumeUnit.ML))
        assertNull(EntryRules.amountFromInput("0", VolumeUnit.ML))
        assertNull(EntryRules.amountFromInput("5001", VolumeUnit.ML))
        assertNull(EntryRules.amountFromInput("", VolumeUnit.ML))
        assertEquals(355, EntryRules.amountFromInput("12", VolumeUnit.FL_OZ))
        assertNull(EntryRules.amountFromInput("170", VolumeUnit.FL_OZ)) // 5,027 ml
    }

    // LOG-004: defaults 250 and 500 ml; 1–4 sizes of 10–2,000 ml; the first is the default.
    @Test
    fun drinkSizeDefaultsAndDecoding() {
        assertEquals(listOf(250, 500), DrinkSizes.DEFAULT)
        assertEquals(listOf(250, 500), DrinkSizes.decode(null))
        assertEquals(listOf(250, 500), DrinkSizes.decode(""))
        assertEquals(listOf(330, 750), DrinkSizes.decode(DrinkSizes.encode(listOf(330, 750))))
        // Invalid stored values are dropped; nothing valid left falls back to the defaults.
        assertEquals(listOf(330), DrinkSizes.decode("330,5,abc"))
        assertEquals(listOf(250, 500), DrinkSizes.decode("5,9999"))
        assertEquals(listOf(100, 200, 300, 400), DrinkSizes.decode("100,200,300,400,500"))
    }

    // LOG-009: blank fields are dropped; any invalid field or no size at all is rejected.
    @Test
    fun drinkSizesFromInputs() {
        assertEquals(listOf(300, 600), DrinkSizes.fromInputs(listOf("300", "", "600", " "), VolumeUnit.ML))
        assertNull(DrinkSizes.fromInputs(listOf("", "", "", ""), VolumeUnit.ML))
        assertNull(DrinkSizes.fromInputs(listOf("300", "5"), VolumeUnit.ML))
        assertNull(DrinkSizes.fromInputs(listOf("300", "2001"), VolumeUnit.ML))
        assertEquals(listOf(237, 473), DrinkSizes.fromInputs(listOf("8", "16"), VolumeUnit.FL_OZ))
        assertTrue(DrinkSizes.isValid(10))
        assertFalse(DrinkSizes.isValid(9))
    }
}
