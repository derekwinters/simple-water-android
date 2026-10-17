package com.derekwinters.water.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * LOG-002: a day is local midnight to local midnight in the zone. The end is the next date's
 * start, never "start + 24 hours", so daylight-saving days come out 23 or 25 hours long.
 */
object DayWindow {
    fun startMillis(date: LocalDate, zone: ZoneId): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    /** Exclusive end of [date]. */
    fun endMillis(date: LocalDate, zone: ZoneId): Long = startMillis(date.plusDays(1), zone)

    fun dateOf(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}

/** LOG-001, LOG-006: one drink entry's amount. */
object EntryRules {
    const val MIN_ML = 1
    const val MAX_ML = 5000

    fun isValid(ml: Double): Boolean = ml >= MIN_ML && ml <= MAX_ML

    /** The amount typed in [unit], in decimal ml (GOAL-008), or null when it is not 1–5,000 ml. */
    fun amountFromInput(text: String, unit: VolumeUnit): Double? {
        val value = text.trim().toIntOrNull() ?: return null
        return Units.displayToMl(value, unit).takeIf { isValid(it) }
    }
}

/** LOG-004, LOG-009: the quick-add drink sizes, in decimal ml. The first is the default size. */
object DrinkSizes {
    /** The stored defaults; while the stored sizes equal these they have not been customised. */
    val DEFAULT: List<Double> = listOf(250.0, 500.0)

    /** LOG-004: the defaults shown (and logged) while the unit is fl oz and nothing is customised. */
    val DEFAULT_FL_OZ: List<Int> = listOf(8, 16)
    const val MAX_COUNT = 4
    const val MIN_ML = 10
    const val MAX_ML = 2000

    fun isValid(ml: Double): Boolean = ml >= MIN_ML && ml <= MAX_ML

    fun encode(sizes: List<Double>): String = sizes.joinToString(",")

    /** Stored sizes; invalid values are dropped, and nothing valid left means the defaults. */
    fun decode(text: String?): List<Double> {
        val sizes = text.orEmpty()
            .split(',')
            .mapNotNull { it.trim().toDoubleOrNull() }
            .filter { isValid(it) }
            .take(MAX_COUNT)
        return sizes.ifEmpty { DEFAULT }
    }

    /**
     * LOG-009: sizes typed in [unit]. Blank fields are dropped; any other field that is not a
     * valid size, or no size at all, gives null.
     */
    fun fromInputs(inputs: List<String>, unit: VolumeUnit): List<Double>? {
        val filled = inputs.map { it.trim() }.filter { it.isNotEmpty() }
        if (filled.isEmpty() || filled.size > MAX_COUNT) return null
        val sizes = filled.map { text ->
            val value = text.toIntOrNull() ?: return null
            Units.displayToMl(value, unit).takeIf { isValid(it) } ?: return null
        }
        return sizes
    }
    /**
     * LOG-004, LOG-009: the sizes as shown in [unit]. While [stored] is still [DEFAULT] (not
     * customised) the defaults follow the unit: 8 and 16 fl oz in fl oz.
     */
    fun forUnit(stored: List<Double>, unit: VolumeUnit): List<Double> = when {
        stored != DEFAULT -> stored
        unit == VolumeUnit.FL_OZ -> DEFAULT_FL_OZ.map { Units.displayToMl(it, unit) }
        else -> DEFAULT
    }

    /**
     * LOG-004, LOG-005: the amounts the quick-add buttons log in [unit], in decimal ml.
     * LOG-005 invariant: each is exactly what its label shows — the size rounded to a whole
     * number in [unit] and converted back exactly — so the label and the entry never disagree.
     */
    fun quickAdd(stored: List<Double>, unit: VolumeUnit): List<Double> =
        forUnit(stored, unit).map { Units.displayToMl(Units.mlToDisplay(it, unit), unit) }
}
