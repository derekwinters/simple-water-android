package com.derekwinters.water.designsystem

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * The five text roles (`DS-034`) for everything that is not a numeric value (`DS-030`–`033`). The
 * sizes are the ones Interval Trainer's screens write inline (16sp bold names, 13sp dim meta),
 * named once here so `:app` never writes a font size of its own.
 */
enum class TextRole {
    Title,
    Body,
    BodyStrong,
    Meta,
    Section,
}

/** The style a [TextRole] renders in, before any colour override. */
@Composable
fun textStyleFor(role: TextRole): TextStyle {
    val colors = AppTheme.colors
    return when (role) {
        TextRole.Title -> TextStyle(color = colors.fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        TextRole.Body -> TextStyle(color = colors.fg, fontSize = 16.sp)
        TextRole.BodyStrong -> TextStyle(color = colors.fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        TextRole.Meta -> TextStyle(color = colors.dim, fontSize = 13.sp)
        TextRole.Section -> TextStyle(
            color = colors.dim,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
    }
}

/**
 * `AppText` (`DS-034`): text in one of the [TextRole]s. [color] overrides the role's colour with
 * a token (for example `work` for a "Goal met" line); null keeps the role's own.
 */
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    role: TextRole = TextRole.Body,
    color: Color? = null,
    textAlign: TextAlign? = null,
) {
    val base = textStyleFor(role)
    val style = base.copy(
        color = color ?: base.color,
        textAlign = textAlign ?: TextAlign.Unspecified,
    )
    BasicText(text = text, modifier = modifier, style = style)
}

/**
 * A numeric value (`DS-030`–`033`) in one of the [NumericTypography] roles, in [color] (default
 * `fg`).
 */
@Composable
fun NumericText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color? = null,
    textAlign: TextAlign? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(
            color = color ?: AppTheme.colors.fg,
            textAlign = textAlign ?: TextAlign.Unspecified,
        ),
    )
}
