package com.derekwinters.water.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Wide enough for five digits in `numeric.stat`. */
private val FieldMinWidth: Dp = 96.dp

/** DS-024: at most this many digits. */
private const val MAX_DIGITS = 5

/**
 * `NumberField` (`DS-024`): a single-line whole-number field — `chip` fill, rounded corners, the
 * value in `numeric.stat`, an optional unit [suffix] in `dim`, an `accent` cursor and the number
 * keyboard. Only digits are accepted, at most five; whether the number is valid is the caller's
 * decision. [contentDescription] names the field for accessibility.
 */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val description = contentDescription
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(spacing.sm))
            .background(colors.chip)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.widthIn(min = FieldMinWidth)) {
            BasicTextField(
                value = value,
                onValueChange = { typed ->
                    val digits = typed.filter { it.isDigit() }.take(MAX_DIGITS)
                    if (digits != value) onValueChange(digits)
                },
                modifier = Modifier.semantics { this.contentDescription = description },
                textStyle = AppTheme.numericTypography.stat.copy(color = colors.fg),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                cursorBrush = SolidColor(colors.accent),
            )
        }
        if (suffix != null) {
            AppText(
                text = suffix,
                role = TextRole.Meta,
                modifier = Modifier.padding(start = spacing.sm),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NumberFieldPreview() {
    AppTheme {
        Surface(color = AppTheme.colors.bg) {
            var text by remember { mutableStateOf("70") }
            NumberField(value = text, onValueChange = { text = it }, contentDescription = "Weight", suffix = "kg")
        }
    }
}
