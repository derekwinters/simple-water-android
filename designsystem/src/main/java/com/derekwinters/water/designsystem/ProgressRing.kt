package com.derekwinters.water.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** `DS-017`'s drawn diameter for the home screen's ring. */
private val RingSize: Dp = 220.dp

/**
 * `ProgressRing` (`DS-017`): a `chip` track with a clockwise `work` arc from 12 o'clock, round
 * caps, stroke `spacing.md`, filled to [fraction] clamped to 0–1, and a centred [content] slot.
 * An empty fraction draws the track only (a round cap on a zero-length arc would draw a dot).
 */
@Composable
fun ProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val strokeWidth = AppTheme.spacing.md
    val clamped = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .size(RingSize)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = colors.chip,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (clamped > 0f) {
                drawArc(
                    color = colors.work,
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressRingPreview() {
    AppTheme {
        Surface(color = AppTheme.colors.bg) {
            ProgressRing(fraction = 0.6f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NumericText(text = "1,470", style = AppTheme.numericTypography.large)
                    AppText(text = "of 2,450 ml", role = TextRole.Meta)
                }
            }
        }
    }
}
