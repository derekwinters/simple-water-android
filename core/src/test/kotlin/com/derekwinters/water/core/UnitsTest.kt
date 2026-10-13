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
    }

    // GOAL-008: a value entered in fl oz becomes the nearest whole ml.
    @Test
    fun displayToMl() {
        assertEquals(250, Units.displayToMl(250, VolumeUnit.ML))
        assertEquals(237, Units.displayToMl(8, VolumeUnit.FL_OZ)) // 236.6
        assertEquals(473, Units.displayToMl(16, VolumeUnit.FL_OZ)) // 473.2
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

    // GOAL-009: a value already in display units, formatted with its unit.
    @Test
    fun formatDisplay() {
        assertEquals("1,000 ml", Units.formatDisplay(1000, VolumeUnit.ML))
        assertEquals("34 fl oz", Units.formatDisplay(34, VolumeUnit.FL_OZ))
    }
}
