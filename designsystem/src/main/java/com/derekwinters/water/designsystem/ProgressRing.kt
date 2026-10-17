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
import kotlin.math.cos
import kotlin.math.sin

/** `DS-017`'s drawn diameter for the home screen's ring. */
private val RingSize: Dp = 220.dp

/** `DS-026`: the width of a tick on the outer ring; `RingBitmap` (`DS-027`) draws the same. */
internal val TickWidth: Dp = 2.dp

/**
 * `ProgressRing` (`DS-017`): a `chip` track with a clockwise `accent` arc from 12 o'clock, round
 * caps, stroke `spacing.md`, filled to [fraction] clamped to 0–1, and a centred [content] slot.
 * An empty fraction draws the track only (a round cap on a zero-length arc would draw a dot).
 *
 * With an [outerFraction] (`DS-026`) a thinner ring is drawn just outside, sharing the centre: a
 * `line` track of stroke `spacing.xs`, `spacing.xs` away from the inner ring, with a clockwise
 * `ideal` arc from 12 o'clock filled to [outerFraction] clamped to 0–1, and a short radial `dim`
 * tick of length `spacing.xs` just outside it at each of [outerTicks] (fractions of the circle,
 * 0 at 12 o'clock). The inner ring keeps its size; the component grows to make room. Without an
 * [outerFraction] the component is exactly `DS-017`.
 */
@Composable
fun ProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    outerFraction: Float? = null,
    outerTicks: List<Float> = emptyList(),
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val strokeWidth = spacing.md
    val outerStrokeWidth = spacing.xs
    val outerGap = spacing.xs
    val tickLength = spacing.xs
    val clamped = fraction.coerceIn(0f, 1f)
    val outerClamped = outerFraction?.coerceIn(0f, 1f)
    // DS-026: the room the outer ring and its ticks take on each side of the inner ring.
    val outerRoom = if (outerClamped == null) 0.dp else outerGap + outerStrokeWidth + tickLength
    Box(
        modifier = modifier
            .size(RingSize + outerRoom * 2)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val room = outerRoom.toPx()
            val inset = room + stroke / 2
            val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
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
                    color = colors.accent,
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            if (outerClamped != null) {
                val outerStroke = outerStrokeWidth.toPx()
                // The outer ring's centre line: past the inner ring's outer edge and the gap.
                val radius = size.minDimension / 2 - room + outerGap.toPx() + outerStroke / 2
                val outerTopLeft = Offset(center.x - radius, center.y - radius)
                val outerSize = Size(radius * 2, radius * 2)
                drawArc(
                    color = colors.line,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = outerTopLeft,
                    size = outerSize,
                    style = Stroke(width = outerStroke),
                )
                if (outerClamped > 0f) {
                    drawArc(
                        color = colors.ideal,
                        startAngle = -90f,
                        sweepAngle = 360f * outerClamped,
                        useCenter = false,
                        topLeft = outerTopLeft,
                        size = outerSize,
                        style = Stroke(width = outerStroke, cap = StrokeCap.Round),
                    )
                }
                val tickFrom = radius + outerStroke / 2
                val tickTo = tickFrom + tickLength.toPx()
                outerTicks.forEach { tick ->
                    val angle = Math.toRadians(360.0 * tick - 90.0)
                    val dx = cos(angle).toFloat()
                    val dy = sin(angle).toFloat()
                    drawLine(
                        color = colors.dim,
                        start = Offset(center.x + dx * tickFrom, center.y + dy * tickFrom),
                        end = Offset(center.x + dx * tickTo, center.y + dy * tickTo),
                        strokeWidth = TickWidth.toPx(),
                    )
                }
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
            ProgressRing(fraction = 0.6f, outerFraction = 4f / 12, outerTicks = List(12) { it / 12f }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NumericText(text = "1,470", style = AppTheme.numericTypography.large)
                    AppText(text = "of 2,450 ml", role = TextRole.Meta)
                }
            }
        }
    }
}
