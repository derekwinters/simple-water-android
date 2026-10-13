package com.derekwinters.water.core

import java.util.Locale
import kotlin.math.roundToInt

/** The volume unit used for display and entry (GOAL-008). Volumes are always stored in ml. */
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

    /** GOAL-008: ml shown in [unit]; fl oz rounded to a whole number. */
    fun mlToDisplay(ml: Int, unit: VolumeUnit): Int = when (unit) {
        VolumeUnit.ML -> ml
        VolumeUnit.FL_OZ -> (ml / ML_PER_FL_OZ).roundToInt()
    }

    /** GOAL-008: a whole number entered in [unit] converted to the nearest whole ml. */
    fun displayToMl(value: Int, unit: VolumeUnit): Int = when (unit) {
        VolumeUnit.ML -> value
        VolumeUnit.FL_OZ -> (value * ML_PER_FL_OZ).roundToInt()
    }

    /** GOAL-009: "2,450 ml" or "83 fl oz". */
    fun formatVolume(ml: Int, unit: VolumeUnit): String = "${formatNumber(ml, unit)} ${unit.label}"

    /** The number part of [formatVolume], without the unit: "2,450". */
    fun formatNumber(ml: Int, unit: VolumeUnit): String = group(mlToDisplay(ml, unit))

    /** GOAL-009: a value already in [unit]'s display units, e.g. 34 → "34 fl oz". */
    fun formatDisplay(value: Int, unit: VolumeUnit): String = "${group(value)} ${unit.label}"

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
