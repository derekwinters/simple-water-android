package com.derekwinters.water.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview

/**
 * `ChoiceChips` (`DS-018`): equal-width chips for choosing one ([multiSelect] false, radio
 * semantics) or several ([multiSelect] true, checkbox semantics) of a few short [options], laid
 * out [columns] per row. Selected chips use the `work` fill with `bg` ink, others `chip` with `fg`.
 * Each chip has a 48dp minimum touch target and reports its selected state; [descriptions], when
 * given, replace a chip's label for accessibility (for example "Monday" for "M").
 *
 * The component reports taps only ([onToggle] with the option's index); the caller owns the
 * selection, per the design system's value-control invariant.
 */
@Composable
fun ChoiceChips(
    options: List<String>,
    isSelected: (Int) -> Boolean,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
    multiSelect: Boolean = false,
    columns: Int = options.size,
    descriptions: List<String>? = null,
) {
    val spacing = AppTheme.spacing
    val perRow = columns.coerceAtLeast(1)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        options.indices.chunked(perRow).forEach { rowIndices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                rowIndices.forEach { index ->
                    Chip(
                        label = options[index],
                        description = descriptions?.getOrNull(index) ?: options[index],
                        selected = isSelected(index),
                        multiSelect = multiSelect,
                        onClick = { onToggle(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keep a short last row's chips the same width as the full rows above it.
                repeat(perRow - rowIndices.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    description: String,
    selected: Boolean,
    multiSelect: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val selection = if (multiSelect) {
        Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
    } else {
        Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    }
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(spacing.sm))
            .background(if (selected) colors.work else colors.chip)
            .semantics { contentDescription = description }
            .then(selection)
            .padding(horizontal = spacing.xs, vertical = spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = label,
            role = if (selected) TextRole.BodyStrong else TextRole.Body,
            color = if (selected) colors.bg else colors.fg,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChoiceChipsPreview() {
    AppTheme {
        Surface(color = AppTheme.colors.bg) {
            var selected by remember { mutableIntStateOf(1) }
            ChoiceChips(
                options = listOf("Sedentary", "Lightly active", "Active", "Very active"),
                isSelected = { it == selected },
                onToggle = { selected = it },
                columns = 2,
            )
        }
    }
}

