package com.derekwinters.water.ui.firstrun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.derekwinters.water.WaterApp
import com.derekwinters.water.core.ActivityLevel
import com.derekwinters.water.core.GoalMath
import com.derekwinters.water.core.Profile
import com.derekwinters.water.core.Units
import com.derekwinters.water.core.VolumeUnit
import com.derekwinters.water.core.WeightUnit
import com.derekwinters.water.designsystem.AppText
import com.derekwinters.water.designsystem.AppTheme
import com.derekwinters.water.designsystem.ChoiceChips
import com.derekwinters.water.designsystem.FormLayout
import com.derekwinters.water.designsystem.NumberField
import com.derekwinters.water.designsystem.NumericText
import com.derekwinters.water.designsystem.SecondaryButton
import com.derekwinters.water.designsystem.TextRole
import kotlinx.coroutines.launch

/** GOAL-010: saves the profile (or the default one) and continues to home. */
@Composable
fun FirstRunRoute(app: WaterApp, onDone: () -> Unit) {
    FirstRunScreen(
        onStart = { profile ->
            app.appScope.launch {
                app.repository.completeFirstRun(profile)
            }
            onDone()
        },
        onSkip = {
            app.appScope.launch {
                app.repository.completeFirstRun(Profile.DEFAULT)
            }
            onDone()
        },
    )
}

/** UI-002: the first-run form. */
@Composable
fun FirstRunScreen(
    onStart: (Profile) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val defaults = Profile.DEFAULT
    var weightUnit by rememberSaveable { mutableStateOf(defaults.weightUnit) }
    var weightText by rememberSaveable { mutableStateOf(Units.kgToDisplay(defaults.weightKg, defaults.weightUnit).toString()) }
    var activity by rememberSaveable { mutableStateOf(defaults.activity) }

    val weightValue = weightText.toIntOrNull()
    // GOAL-011: Start is enabled only for a valid weight.
    val valid = weightValue != null && GoalMath.isValidWeight(weightValue, weightUnit)
    val profile = if (valid) Profile(Units.displayToKg(weightValue!!, weightUnit), activity, weightUnit) else null

    FormLayout(
        primaryActionText = "Start tracking",
        onPrimaryAction = { profile?.let(onStart) },
        modifier = modifier,
        headerTitle = "Welcome",
        primaryActionEnabled = profile != null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            AppText(
                text = "Simple Water suggests a daily goal from your weight and how active you are. " +
                    "You can change it, or set your own, at any time in settings.",
                textAlign = TextAlign.Center,
            )

            AppText(text = "WEIGHT", role = TextRole.Section)
            NumberField(
                value = weightText,
                onValueChange = { weightText = it },
                contentDescription = "Weight in ${weightUnit.label}",
                suffix = weightUnit.label,
            )
            ChoiceChips(
                options = WeightUnit.entries.map { it.label },
                isSelected = { WeightUnit.entries[it] == weightUnit },
                onToggle = { index ->
                    val chosen = WeightUnit.entries[index]
                    if (chosen != weightUnit) {
                        // Keep the same weight, shown in the new unit.
                        weightText.toIntOrNull()?.let { value ->
                            weightText = Units.kgToDisplay(Units.displayToKg(value, weightUnit), chosen).toString()
                        }
                        weightUnit = chosen
                    }
                },
            )

            AppText(text = "ACTIVITY", role = TextRole.Section)
            ChoiceChips(
                options = ActivityLevel.entries.map { it.label },
                isSelected = { ActivityLevel.entries[it] == activity },
                onToggle = { activity = ActivityLevel.entries[it] },
                columns = 2,
            )

            AppText(text = "RECOMMENDED DAILY GOAL", role = TextRole.Section)
            NumericText(
                text = profile?.let { Units.formatVolume(GoalMath.recommendedGoalMl(it), VolumeUnit.ML) } ?: "—",
                style = AppTheme.numericTypography.stat,
            )
            if (!valid) {
                val range = if (weightUnit == WeightUnit.KG) GoalMath.WEIGHT_RANGE_KG else GoalMath.WEIGHT_RANGE_LB
                AppText(
                    text = "Enter a weight from ${range.first} to ${range.last} ${weightUnit.label}.",
                    role = TextRole.Meta,
                )
            }

            SecondaryButton(text = "Skip — use 70 kg, Sedentary", onClick = onSkip)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FirstRunScreenPreview() {
    AppTheme {
        FirstRunScreen(onStart = {}, onSkip = {})
    }
}
