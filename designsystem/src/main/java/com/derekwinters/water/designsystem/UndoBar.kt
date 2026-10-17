package com.derekwinters.water.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview

/**
 * `UndoBar` (`DS-025`): a full-width `chip` bar with a [message] and one `accent` text action, shown
 * in a list layout's bottom slot after something was removed (LOG-008). How long it stays is the
 * caller's decision.
 */
@Composable
fun UndoBar(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.sm))
            .background(colors.chip)
            .padding(start = spacing.lg, end = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(text = message, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction) {
            Text(text = actionLabel, color = colors.accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UndoBarPreview() {
    AppTheme {
        Surface(color = AppTheme.colors.bg) {
            UndoBar(message = "Removed 250 ml", actionLabel = "Undo", onAction = {})
        }
    }
}
