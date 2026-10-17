package com.derekwinters.water.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Requirement IDs refer to docs/spec/goal.md. */
class GoalMathTest {

    // GOAL-004: weight_kg × 35 ml plus the activity allowance, rounded to the nearest 50 ml.
    @Test
    fun recommendedGoalForEachActivityLevel() {
        assertEquals(2450, GoalMath.recommendedGoalMl(70.0, ActivityLevel.SEDENTARY))
        assertEquals(2800, GoalMath.recommendedGoalMl(70.0, ActivityLevel.LIGHTLY_ACTIVE))
        assertEquals(3150, GoalMath.recommendedGoalMl(70.0, ActivityLevel.ACTIVE))
        assertEquals(3500, GoalMath.recommendedGoalMl(70.0, ActivityLevel.VERY_ACTIVE))
    }

    // GOAL-002: the four levels, in order, with 0 / 350 / 700 / 1050 ml allowances.
    @Test
    fun activityLevelsInOrder() {
        assertEquals(
            listOf("Sedentary", "Lightly active", "Active", "Very active"),
            ActivityLevel.entries.map { it.label },
        )
        assertEquals(listOf(0, 350, 700, 1050), ActivityLevel.entries.map { it.allowanceMl })
    }

    // GOAL-004: rounding to the nearest 50 ml, an exact half rounding up.
    @Test
    fun roundsToNearestFifty() {
        assertEquals(2850, GoalMath.recommendedGoalMl(81.0, ActivityLevel.SEDENTARY)) // 2835
        assertEquals(2800, GoalMath.recommendedGoalMl(80.5, ActivityLevel.SEDENTARY)) // 2817.5
        assertEquals(2300, GoalMath.recommendedGoalMl(65.0, ActivityLevel.SEDENTARY)) // 2275, half
    }

    // GOAL-001, GOAL-004: a weight entered in pounds is converted to kilograms first.
    @Test
    fun poundsAreConvertedToKilograms() {
        val kg = Units.displayToKg(154, WeightUnit.LB)
        assertEquals(69.853, kg, 0.001)
        assertEquals(2450, GoalMath.recommendedGoalMl(kg, ActivityLevel.SEDENTARY))
    }

    // GOAL-001: valid weights are 20–300 kg or 44–661 lb, inclusive, in the entered unit.
    @Test
    fun weightValidation() {
        assertTrue(GoalMath.isValidWeight(20, WeightUnit.KG))
        assertTrue(GoalMath.isValidWeight(300, WeightUnit.KG))
        assertFalse(GoalMath.isValidWeight(19, WeightUnit.KG))
        assertFalse(GoalMath.isValidWeight(301, WeightUnit.KG))
        assertTrue(GoalMath.isValidWeight(44, WeightUnit.LB))
        assertTrue(GoalMath.isValidWeight(661, WeightUnit.LB))
        assertFalse(GoalMath.isValidWeight(43, WeightUnit.LB))
        assertFalse(GoalMath.isValidWeight(662, WeightUnit.LB))
    }

    // GOAL-003: the default profile is 70 kg, Sedentary, in kg.
    @Test
    fun defaultProfile() {
        assertEquals(Profile(70.0, ActivityLevel.SEDENTARY, WeightUnit.KG), Profile.DEFAULT)
        assertEquals(2450, GoalMath.recommendedGoalMl(Profile.DEFAULT))
    }

    // GOAL-006, GOAL-007: the effective goal is custom ?: recommended.
    @Test
    fun effectiveGoalPrefersCustom() {
        val profile = Profile(70.0, ActivityLevel.SEDENTARY, WeightUnit.KG)
        assertEquals(2450, GoalMath.effectiveGoalMl(profile, customGoalMl = null))
        assertEquals(3000, GoalMath.effectiveGoalMl(profile, customGoalMl = 3000))
        // GOAL-007 invariant: a profile change does not move a custom goal.
        val heavier = profile.copy(weightKg = 90.0)
        assertEquals(3000, GoalMath.effectiveGoalMl(heavier, customGoalMl = 3000))
        assertEquals(3150, GoalMath.effectiveGoalMl(heavier, customGoalMl = null))
    }

    // GOAL-006: a custom goal is 500–10,000 ml, entered in the volume unit.
    @Test
    fun customGoalValidation() {
        assertEquals(500, GoalMath.customGoalFromInput("500", VolumeUnit.ML))
        assertEquals(10_000, GoalMath.customGoalFromInput("10000", VolumeUnit.ML))
        assertEquals(null, GoalMath.customGoalFromInput("499", VolumeUnit.ML))
        assertEquals(null, GoalMath.customGoalFromInput("10001", VolumeUnit.ML))
        assertEquals(null, GoalMath.customGoalFromInput("", VolumeUnit.ML))
        assertEquals(null, GoalMath.customGoalFromInput("abc", VolumeUnit.ML))
        assertEquals(2366, GoalMath.customGoalFromInput("80", VolumeUnit.FL_OZ)) // 80 × 29.5735
        assertEquals(null, GoalMath.customGoalFromInput("16", VolumeUnit.FL_OZ)) // 473 ml
    }

    // LOG-003: progress fraction and remaining volume.
    @Test
    fun progress() {
        assertEquals(0.5f, GoalMath.fraction(1225.0, 2450), 0.0001f)
        assertEquals(1f, GoalMath.fraction(3000.0, 2450), 0.0001f)
        assertEquals(0f, GoalMath.fraction(0.0, 2450), 0.0001f)
        assertEquals(1225.0, GoalMath.remainingMl(1225.0, 2450), 0.0)
        assertEquals(0.0, GoalMath.remainingMl(3000.0, 2450), 0.0)
        // GOAL-008 invariant: what is left is computed from the unrounded total.
        val eight = Units.displayToMl(8, VolumeUnit.FL_OZ)
        assertEquals("67 fl oz to go", Units.formatVolume(GoalMath.remainingMl(2 * eight, 2450), VolumeUnit.FL_OZ) + " to go")
    }
}
