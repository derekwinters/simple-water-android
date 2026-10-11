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

    fun isValid(ml: Int): Boolean = ml in MIN_ML..MAX_ML

    /** The amount typed in [unit], in ml, or null when it is not 1–5,000 ml. */
    fun amountFromInput(text: String, unit: VolumeUnit): Int? {
        val value = text.trim().toIntOrNull() ?: return null
        return Units.displayToMl(value, unit).takeIf { isValid(it) }
    }
}

/** LOG-004, LOG-009: the quick-add drink sizes, in ml. The first is the default size. */
object DrinkSizes {
    val DEFAULT: List<Int> = listOf(250, 500)
    const val MAX_COUNT = 4
    const val MIN_ML = 10
    const val MAX_ML = 2000

    fun isValid(ml: Int): Boolean = ml in MIN_ML..MAX_ML

    fun encode(sizes: List<Int>): String = sizes.joinToString(",")

    /** Stored sizes; invalid values are dropped, and nothing valid left means the defaults. */
    fun decode(text: String?): List<Int> {
        val sizes = text.orEmpty()
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { isValid(it) }
            .take(MAX_COUNT)
        return sizes.ifEmpty { DEFAULT }
    }

    /**
     * LOG-009: sizes typed in [unit]. Blank fields are dropped; any other field that is not a
     * valid size, or no size at all, gives null.
     */
    fun fromInputs(inputs: List<String>, unit: VolumeUnit): List<Int>? {
        val filled = inputs.map { it.trim() }.filter { it.isNotEmpty() }
        if (filled.isEmpty() || filled.size > MAX_COUNT) return null
        val sizes = filled.map { text ->
            val value = text.toIntOrNull() ?: return null
            Units.displayToMl(value, unit).takeIf { isValid(it) } ?: return null
        }
        return sizes
    }
}
