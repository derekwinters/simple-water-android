package com.derekwinters.water.core

import java.util.Locale
import kotlin.math.roundToInt

/** The volume unit used for display and entry (GOAL-008). Volumes are stored in decimal ml. */
enum class VolumeUnit(val label: String) {
    ML("ml"),
    FL_OZ("fl oz"),
}

/** The unit a weight is entered and shown in (GOAL-001). Weights are stored in kg. */
enum class WeightUnit(val label: String) {
    KG("kg"),
    LB("lb"),
}

/** Unit conversion and formatting (GOAL-001, GOAL-008, GOAL-009). */
object Units {
    /** One US fluid ounce in millilitres. */
    const val ML_PER_FL_OZ = 29.5735

    /** One pound in kilograms (exact by definition). */
    const val KG_PER_LB = 0.45359237

    /**
     * GOAL-008: decimal ml shown in [unit], rounded to a whole ml or a whole fl oz. This is the only
     * place a volume is rounded: totals are summed unrounded and pass through here once.
     */
    fun mlToDisplay(ml: Double, unit: VolumeUnit): Int = when (unit) {
        VolumeUnit.ML -> ml.roundToInt()
        VolumeUnit.FL_OZ -> (ml / ML_PER_FL_OZ).roundToInt()
    }

    fun mlToDisplay(ml: Int, unit: VolumeUnit): Int = mlToDisplay(ml.toDouble(), unit)

    /** GOAL-008: a whole number entered in [unit], converted exactly (unrounded) to ml. */
    fun displayToMl(value: Int, unit: VolumeUnit): Double = when (unit) {
        VolumeUnit.ML -> value.toDouble()
        VolumeUnit.FL_OZ -> value * ML_PER_FL_OZ
    }

    /** GOAL-009: "2,450 ml" or "83 fl oz". */
    fun formatVolume(ml: Double, unit: VolumeUnit): String = "${formatNumber(ml, unit)} ${unit.label}"

    fun formatVolume(ml: Int, unit: VolumeUnit): String = formatVolume(ml.toDouble(), unit)

    /** The number part of [formatVolume], without the unit: "2,450". */
    fun formatNumber(ml: Double, unit: VolumeUnit): String = group(mlToDisplay(ml, unit))

    fun formatNumber(ml: Int, unit: VolumeUnit): String = formatNumber(ml.toDouble(), unit)

    fun kgToDisplay(kg: Double, unit: WeightUnit): Int = when (unit) {
        WeightUnit.KG -> kg.roundToInt()
        WeightUnit.LB -> (kg / KG_PER_LB).roundToInt()
    }

    fun displayToKg(value: Int, unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> value.toDouble()
        WeightUnit.LB -> value * KG_PER_LB
    }

    fun formatWeight(kg: Double, unit: WeightUnit): String = "${kgToDisplay(kg, unit)} ${unit.label}"

    private fun group(n: Int): String = String.format(Locale.US, "%,d", n)
}
