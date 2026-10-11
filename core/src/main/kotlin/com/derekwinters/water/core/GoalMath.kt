package com.derekwinters.water.core

import kotlin.math.roundToInt

/** GOAL-002: the activity levels, in order, with their daily allowance (GOAL-004). */
enum class ActivityLevel(val label: String, val allowanceMl: Int) {
    SEDENTARY("Sedentary", 0),
    LIGHTLY_ACTIVE("Lightly active", 350),
    ACTIVE("Active", 700),
    VERY_ACTIVE("Very active", 1050),
}

/** GOAL-001: the profile. [weightKg] is stored in kg whatever [weightUnit] it was entered in. */
data class Profile(
    val weightKg: Double,
    val activity: ActivityLevel,
    val weightUnit: WeightUnit,
) {
    companion object {
        /** GOAL-003: used when first run is skipped. */
        val DEFAULT = Profile(weightKg = 70.0, activity = ActivityLevel.SEDENTARY, weightUnit = WeightUnit.KG)
    }
}

/** The daily goal (docs/spec/goal.md). */
object GoalMath {
    const val ML_PER_KG = 35
    const val ROUND_TO_ML = 50
    const val MIN_GOAL_ML = 500
    const val MAX_GOAL_ML = 10_000
    val WEIGHT_RANGE_KG = 20..300
    val WEIGHT_RANGE_LB = 44..661

    /**
     * GOAL-004: weight × 35 ml plus the activity allowance, rounded to the nearest 50 ml with an
     * exact half rounding up. GOAL-005: always computed from the profile, never stored.
     */
    fun recommendedGoalMl(weightKg: Double, activity: ActivityLevel): Int {
        val raw = weightKg * ML_PER_KG + activity.allowanceMl
        return (raw / ROUND_TO_ML).roundToInt() * ROUND_TO_ML
    }

    fun recommendedGoalMl(profile: Profile): Int = recommendedGoalMl(profile.weightKg, profile.activity)

    /** GOAL-001: a whole-number weight, validated in the unit it was entered in. */
    fun isValidWeight(value: Int, unit: WeightUnit): Boolean = when (unit) {
        WeightUnit.KG -> value in WEIGHT_RANGE_KG
        WeightUnit.LB -> value in WEIGHT_RANGE_LB
    }

    /** GOAL-007: custom ?: recommended. */
    fun effectiveGoalMl(profile: Profile, customGoalMl: Int?): Int = customGoalMl ?: recommendedGoalMl(profile)

    fun isValidGoalMl(ml: Int): Boolean = ml in MIN_GOAL_ML..MAX_GOAL_ML

    /** GOAL-006: a custom goal typed in [unit], or null when it is not 500–10,000 ml. */
    fun customGoalFromInput(text: String, unit: VolumeUnit): Int? {
        val value = text.trim().toIntOrNull() ?: return null
        val ml = Units.displayToMl(value, unit)
        return ml.takeIf { isValidGoalMl(it) }
    }

    /** LOG-003: how full the ring is, 0–1. */
    fun fraction(totalMl: Int, goalMl: Int): Float =
        if (goalMl <= 0) 0f else (totalMl.toFloat() / goalMl).coerceIn(0f, 1f)

    /** LOG-003: what is left to drink today, never negative. */
    fun remainingMl(totalMl: Int, goalMl: Int): Int = (goalMl - totalMl).coerceAtLeast(0)
}
