package com.derekwinters.water.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** Requirement IDs refer to docs/spec/goal.md and docs/spec/logging.md. */
class UnitsTest {

    // GOAL-008: ml are stored; fl oz are shown rounded to a whole number.
    @Test
    fun mlToDisplay() {
        assertEquals(250, Units.mlToDisplay(250, VolumeUnit.ML))
        assertEquals(8, Units.mlToDisplay(250, VolumeUnit.FL_OZ)) // 8.45
        assertEquals(17, Units.mlToDisplay(500, VolumeUnit.FL_OZ)) // 16.9
        assertEquals(83, Units.mlToDisplay(2450, VolumeUnit.FL_OZ)) // 82.8
        // Decimal ml are shown as whole ml, and as whole fl oz.
        assertEquals(237, Units.mlToDisplay(236.588, VolumeUnit.ML))
        assertEquals(8, Units.mlToDisplay(236.588, VolumeUnit.FL_OZ))
    }

    // GOAL-008: a value entered in fl oz is converted exactly, unrounded.
    @Test
    fun displayToMl() {
        assertEquals(250.0, Units.displayToMl(250, VolumeUnit.ML), 0.0)
        assertEquals(236.588, Units.displayToMl(8, VolumeUnit.FL_OZ), 1e-9)
        assertEquals(473.176, Units.displayToMl(16, VolumeUnit.FL_OZ), 1e-9)
    }

    // GOAL-008 invariant: a total is the sum of stored amounts, rounded once for display.
    @Test
    fun totalsAreRoundedOnce() {
        val eight = Units.displayToMl(8, VolumeUnit.FL_OZ)
        assertEquals("16 fl oz", Units.formatVolume(List(2) { eight }.sum(), VolumeUnit.FL_OZ))
        assertEquals("800 fl oz", Units.formatVolume(List(100) { eight }.sum(), VolumeUnit.FL_OZ))
        // In ml the same two drinks are 473.176 ml: "473 ml", not 237 + 237.
        assertEquals("473 ml", Units.formatVolume(List(2) { eight }.sum(), VolumeUnit.ML))
    }

    // GOAL-009: thousands separator and unit label.
    @Test
    fun formatting() {
        assertEquals("2,450 ml", Units.formatVolume(2450, VolumeUnit.ML))
        assertEquals("250 ml", Units.formatVolume(250, VolumeUnit.ML))
        assertEquals("83 fl oz", Units.formatVolume(2450, VolumeUnit.FL_OZ))
        assertEquals("12,000", Units.formatNumber(12000, VolumeUnit.ML))
        assertEquals("0 ml", Units.formatVolume(0, VolumeUnit.ML))
    }

    // GOAL-001: weight shown in the chosen unit, rounded.
    @Test
    fun weight() {
        assertEquals(70, Units.kgToDisplay(70.0, WeightUnit.KG))
        assertEquals(154, Units.kgToDisplay(Units.displayToKg(154, WeightUnit.LB), WeightUnit.LB))
        assertEquals(154, Units.kgToDisplay(70.0, WeightUnit.LB)) // 154.3
        assertEquals("70 kg", Units.formatWeight(70.0, WeightUnit.KG))
        assertEquals("154 lb", Units.formatWeight(70.0, WeightUnit.LB))
    }
}
