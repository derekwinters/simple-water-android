package com.derekwinters.water.core

import java.time.ZonedDateTime
import kotlin.math.floor
import kotlin.math.roundToInt

/** UI-021, UI-023: what the "Text in cup" setting writes in the widget's cup. */
enum class CupTextMode(val label: String) {
    NONE("None"),
    PERCENT("Percent"),
    AMOUNT("Amount"),
}

/** UI-023: the two widget settings and their defaults. */
data class WidgetSettings(
    val text: CupTextMode = CupTextMode.NONE,
    val idealLine: Boolean = true,
)

/** UI-021, UI-022: what is written in the cup. */
sealed interface CupLabel {
    /** Nothing: the text is set to None. */
    data object None : CupLabel

    /** UI-022: the goal is met. */
    data object Check : CupLabel

    /** UI-021: a number, with its unit under it when there is one ("1.1" over "L"; "45%" alone). */
    data class Value(val number: String, val unit: String?) : CupLabel
}

/**
 * UI-015, UI-020–022: what the widget draws. [water] is the water's height as a fraction of full
 * (0–1); [ideal] is the ideal line's, or null when it is hidden; [goalMet] is home's "Goal met".
 */
data class CupState(val water: Float, val ideal: Float?, val goalMet: Boolean, val label: CupLabel) {
    /** UI-018: the form the widget stores between refreshes; [decode] reads it back unchanged. */
    fun encode(): String {
        val (kind, number, unit) = when (label) {
            CupLabel.None -> Triple("N", "", "")
            CupLabel.Check -> Triple("C", "", "")
            is CupLabel.Value -> Triple("V", label.number, label.unit ?: NO_UNIT)
        }
        return listOf(water.toString(), ideal?.toString().orEmpty(), goalMet.toString(), kind, number, unit)
            .joinToString(SEPARATOR)
    }

    companion object {
        private const val SEPARATOR = ";"
        private const val NO_UNIT = "-"

        /** The cup [encode] wrote, or null for anything else. */
        fun decode(text: String?): CupState? {
            val fields = text?.split(SEPARATOR) ?: return null
            if (fields.size != 6) return null
            val water = fields[0].toFloatOrNull() ?: return null
            val ideal = if (fields[1].isEmpty()) null else fields[1].toFloatOrNull() ?: return null
            val goalMet = fields[2].toBooleanStrictOrNull() ?: return null
            val label = when (fields[3]) {
                "N" -> CupLabel.None
                "C" -> CupLabel.Check
                "V" -> CupLabel.Value(fields[4], fields[5].takeUnless { it == NO_UNIT })
                else -> return null
            }
            return CupState(water, ideal, goalMet, label)
        }
    }
}

/**
 * The home screen widget's cup (docs/spec/screens.md UI-015, UI-020–023). Invariant: the water,
 * the ideal line and goal met come from home's own values — `Rings` and `GoalMath.remainingMl` —
 * never a second calculation.
 */
object WidgetCup {
    /** Totals a hair under a whole step (decimal-ml fl oz sums, LOG-012) still count as that step. */
    private const val STEP_TOLERANCE = 1e-6

    /** UI-015, UI-020–022: the cup for [totalMl] against [goalMl], with the ideal amount [pace]. */
    fun of(totalMl: Double, goalMl: Int, pace: PaceState?, unit: VolumeUnit, settings: WidgetSettings): CupState {
        val rings = Rings.of(totalMl, goalMl, pace)
        // Invariant: home's own test for "Goal met" (UI-004).
        val goalMet = GoalMath.remainingMl(totalMl, goalMl) == 0.0
        val label = when {
            settings.text == CupTextMode.NONE -> CupLabel.None
            goalMet -> CupLabel.Check
            settings.text == CupTextMode.PERCENT -> percent(totalMl, goalMl)
            else -> amount(totalMl, goalMl, unit)
        }
        return CupState(
            water = rings.fraction,
            ideal = rings.outerFraction.takeIf { settings.idealLine },
            goalMet = goalMet,
            label = label,
        )
    }

    /** UI-015: the cup at [now] from [entries], counting only [now]'s local date (LOG-002). */
    fun today(
        entries: List<LoggedAmount>,
        now: ZonedDateTime,
        goalMl: Int,
        reminders: ReminderSettings,
        unit: VolumeUnit,
        settings: WidgetSettings,
    ): CupState = of(Rings.totalToday(entries, now), goalMl, Pace.today(reminders, now.toLocalDateTime(), goalMl), unit, settings)

    /**
     * UI-021: [value] rounded down to whole steps, and — the goal not being met — at most one
     * step below [goalSteps], the goal as shown in the same steps.
     */
    private fun stepsBelowGoal(value: Double, goalSteps: Int): Int =
        minOf(floor(value + STEP_TOLERANCE).toInt(), goalSteps - 1).coerceAtLeast(0)

    /** UI-021: "45%", rounded down, never 100% before the goal. */
    private fun percent(totalMl: Double, goalMl: Int): CupLabel.Value =
        CupLabel.Value("${stepsBelowGoal(totalMl / goalMl * 100, 100)}%", null)

    /** UI-021: whole fl oz, or litres to one decimal for ml users, rounded down. */
    private fun amount(totalMl: Double, goalMl: Int, unit: VolumeUnit): CupLabel.Value = when (unit) {
        VolumeUnit.FL_OZ -> {
            // The goal as home shows it (GOAL-009).
            val goal = Units.mlToDisplay(goalMl, VolumeUnit.FL_OZ)
            CupLabel.Value(stepsBelowGoal(totalMl / Units.ML_PER_FL_OZ, goal).toString(), VolumeUnit.FL_OZ.label)
        }
        VolumeUnit.ML -> {
            val goalTenths = (goalMl / ML_PER_TENTH_LITRE).roundToInt()
            val tenths = stepsBelowGoal(totalMl / ML_PER_TENTH_LITRE, goalTenths)
            CupLabel.Value("${tenths / 10}.${tenths % 10}", LITRE)
        }
    }

    private const val ML_PER_TENTH_LITRE = 100.0
    private const val LITRE = "L"
}
