package com.derekwinters.water.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Every component of this design system on one list-layout screen (`DS-095` in Interval Trainer),
 * kept as a `@Preview` for review. Not a screenshot target and not a test fixture here (`DS-091`).
 */
@Composable
fun ComponentGallery(modifier: Modifier = Modifier) {
    val spacing = AppTheme.spacing
    ListLayout(
        title = "Gallery",
        modifier = modifier,
        onBack = {},
        trailingAction = ScreenHeaderAction.Icon(
            icon = Icons.Filled.Settings,
            contentDescription = "Settings",
            onClick = {},
        ),
        bottomSlot = {
            UndoBar(message = "Removed 250 ml", actionLabel = "Undo", onAction = {})
        },
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                ProgressRing(fraction = 0.6f) {
                    NumericText(text = "1,470", style = AppTheme.numericTypography.large)
                }
                AppText(text = "Section", role = TextRole.Section)
                AppText(text = "Title", role = TextRole.Title)
                AppText(text = "Body text", role = TextRole.Body)
                AppText(text = "Meta text", role = TextRole.Meta)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    PrimaryButton(text = "+ 250 ml", onClick = {})
                    SecondaryButton(text = "Custom…", onClick = {})
                }
                DestructiveButton(text = "Delete", onClick = {})
                StatusIconButton(icon = Icons.Filled.Delete, contentDescription = "Delete entry", onClick = {})
                ChoiceChips(
                    options = listOf("M", "T", "W", "T", "F", "S", "S"),
                    isSelected = { it < 5 },
                    onToggle = {},
                    multiSelect = true,
                )
                ValueStepper(
                    label = "08:00",
                    onDecrement = {},
                    onIncrement = {},
                    decrementDescription = "Earlier",
                    incrementDescription = "Later",
                )
                CountStepper(count = 4, onCountChange = {})
                NumberField(value = "250", onValueChange = {}, contentDescription = "Amount", suffix = "ml")
                Toggle(checked = true, onCheckedChange = {}, contentDescription = "Reminders")
                StockListItem(headline = "Reminders", supportingText = "Every 60 min, 08:00–20:00")
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun ComponentGalleryPreview() {
    AppTheme {
        ComponentGallery()
    }
}
