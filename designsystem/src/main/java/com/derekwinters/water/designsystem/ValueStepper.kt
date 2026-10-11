package com.derekwinters.water.designsystem

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview

/**
 * `ValueStepper` (`DS-019`): [CountStepper]'s `−`/`+` buttons (the same [StepperButton], 26dp
 * drawn, 48dp touch target) around a formatted [label] in `numeric.stat`, for a value that is not
 * a plain count — a time of day in 30-minute steps on the reminders screen. The caller owns the
 * value and the step: the stepper reports taps only, and each button can be disabled at the end
 * of the caller's range.
 */
@Composable
fun ValueStepper(
    label: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementDescription: String,
    incrementDescription: String,
    modifier: Modifier = Modifier,
    decrementEnabled: Boolean = true,
    incrementEnabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            label = "−",
            contentDescription = decrementDescription,
            enabled = decrementEnabled,
            onClick = onDecrement,
            background = colors.chip,
            ink = colors.fg,
        )
        NumericText(
            text = label,
            style = AppTheme.numericTypography.stat,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = spacing.sm),
        )
        StepperButton(
            label = "+",
            contentDescription = incrementDescription,
            enabled = incrementEnabled,
            onClick = onIncrement,
            background = colors.chip,
            ink = colors.fg,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ValueStepperPreview() {
    AppTheme {
        Surface(color = AppTheme.colors.bg) {
            var minute by remember { mutableIntStateOf(8 * 60) }
            ValueStepper(
                label = "%02d:%02d".format(minute / 60, minute % 60),
                onDecrement = { minute -= 30 },
                onIncrement = { minute += 30 },
                decrementDescription = "Earlier",
                incrementDescription = "Later",
            )
        }
    }
}
