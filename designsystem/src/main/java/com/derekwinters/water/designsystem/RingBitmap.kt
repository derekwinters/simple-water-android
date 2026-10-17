package com.derekwinters.water.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * `RingBitmap` (`DS-027`): `ProgressRing`'s rings (`DS-017`, `DS-026`) drawn to a square bitmap,
 * for a surface that cannot run Compose UI — the home screen widget (docs/spec/screens.md UI-015).
 *
 * The same tokens as `ProgressRing`: a `chip` track with an `accent` arc for [fraction]; with an
 * [outerFraction], a `line` track with an `ideal` arc and a `dim` tick at each of [outerTicks]
 * (fractions of the circle, 0 at 12 o'clock). Arcs run clockwise from 12 o'clock with round caps,
 * clamped to 0–1, and an empty arc draws the track only. Strokes, gap and ticks keep their dp
 * sizes; [RingGeometry] places them so the rings fill the square. A [centre] drawable is drawn
 * across the inner ring's opening.
 */
object RingBitmap {
    fun render(
        sidePx: Int,
        density: Float,
        fraction: Float,
        outerFraction: Float?,
        outerTicks: List<Float>,
        centre: Drawable?,
        colors: DesignSystemColors = DesignSystemColors.Default,
        spacing: Spacing = Spacing.Default,
    ): Bitmap {
        val side = sidePx.coerceAtLeast(1)
        val geometry = RingGeometry.of(
            sidePx = side.toFloat(),
            density = density,
            innerStrokeDp = spacing.md.value,
            outerStrokeDp = spacing.xs.value,
            outerGapDp = spacing.xs.value,
            tickLengthDp = spacing.xs.value,
            tickWidthDp = TickWidth.value,
        )
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val middle = side / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

        fun ring(radius: Float, stroke: Float, track: Color, arc: Color, filled: Float) {
            paint.strokeWidth = stroke
            paint.strokeCap = Paint.Cap.BUTT
            paint.color = track.toArgb()
            canvas.drawCircle(middle, middle, radius, paint)
            val clamped = filled.coerceIn(0f, 1f)
            if (clamped > 0f) {
                paint.strokeCap = Paint.Cap.ROUND
                paint.color = arc.toArgb()
                val bounds = RectF(middle - radius, middle - radius, middle + radius, middle + radius)
                canvas.drawArc(bounds, -90f, 360f * clamped, false, paint)
            }
        }

        ring(geometry.innerRadius, geometry.innerStroke, colors.chip, colors.accent, fraction)
        if (outerFraction != null) {
            ring(geometry.outerRadius, geometry.outerStroke, colors.line, colors.ideal, outerFraction)
            paint.strokeWidth = geometry.tickWidth
            paint.strokeCap = Paint.Cap.BUTT
            paint.color = colors.dim.toArgb()
            outerTicks.forEach { tick ->
                val angle = Math.toRadians(360.0 * tick - 90.0)
                val dx = cos(angle).toFloat()
                val dy = sin(angle).toFloat()
                canvas.drawLine(
                    middle + dx * geometry.tickFrom,
                    middle + dy * geometry.tickFrom,
                    middle + dx * geometry.tickTo,
                    middle + dy * geometry.tickTo,
                    paint,
                )
            }
        }
        if (centre != null && geometry.openingRadius > 0f) {
            val radius = geometry.openingRadius
            centre.setBounds(
                (middle - radius).roundToInt(),
                (middle - radius).roundToInt(),
                (middle + radius).roundToInt(),
                (middle + radius).roundToInt(),
            )
            centre.draw(canvas)
        }
        return bitmap
    }
}
