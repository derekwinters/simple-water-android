package com.derekwinters.water.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.derekwinters.water.core.Box
import com.derekwinters.water.core.CupGeometry
import com.derekwinters.water.core.CupLabel
import com.derekwinters.water.core.CupState
import com.derekwinters.water.core.PathOp
import com.derekwinters.water.designsystem.DesignSystemColors
import kotlin.math.ceil

/**
 * Draws the widget's cup (docs/spec/screens.md UI-015, UI-016, UI-020–022) to a bitmap, since a
 * widget cannot draw paths. Every shape, size and position is [CupGeometry]'s, in icon units; this
 * only scales them to pixels and paints them: the water and cup in the brand colours (BRAND-056),
 * the ideal line and the text in the design system's `fg`, their outline in `bg`.
 */
object CupBitmap {
    /** The size the number is measured at; [CupGeometry.fitText] scales it to the cup. */
    private const val REFERENCE_SIZE = 100f

    /**
     * The cup for a [cardWidthPx] × [cardHeightPx] card less [paddingPx] each side (UI-015). The
     * bitmap is the cup's bounds at that scale; the caller centres it on the card.
     */
    fun render(
        cardWidthPx: Float,
        cardHeightPx: Float,
        paddingPx: Float,
        state: CupState,
        typeface: Typeface?,
        colors: DesignSystemColors = DesignSystemColors.Default,
    ): Bitmap {
        val bounds = CupGeometry.BOUNDS
        val scale = CupGeometry.placement(cardWidthPx.toDouble(), cardHeightPx.toDouble(), paddingPx.toDouble()).scale
        val width = ceil(bounds.width * scale).toInt().coerceAtLeast(1)
        val height = ceil(bounds.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // From here on the canvas is in icon units.
        canvas.scale(scale.toFloat(), scale.toFloat())
        canvas.translate(-bounds.left.toFloat(), -bounds.top.toFloat())

        drawWater(canvas, state.water)
        state.ideal?.let { drawIdealLine(canvas, it, colors) }
        drawCup(canvas)
        when (val label = state.label) {
            CupLabel.None -> Unit
            CupLabel.Check -> drawCheck(canvas, colors)
            is CupLabel.Value -> drawValue(canvas, label, typeface, colors)
        }
        return bitmap
    }

    /** BRAND-056: BRAND-012's water at the level, clipped at the bottom. */
    private fun drawWater(canvas: Canvas, fraction: Float) {
        val water = CupGeometry.waterPath(fraction.toDouble()) ?: return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL_AND_STROKE
            color = CupGeometry.BLUE.toInt()
            strokeWidth = CupGeometry.WATER_STROKE.toFloat()
            strokeJoin = Paint.Join.ROUND
        }
        canvas.save()
        canvas.clipRect(
            CupGeometry.BOUNDS.left.toFloat(),
            CupGeometry.BOUNDS.top.toFloat(),
            CupGeometry.BOUNDS.right.toFloat(),
            CupGeometry.WATER_CLIP_BOTTOM.toFloat(),
        )
        canvas.drawPath(water.toPath(), paint)
        canvas.restore()
    }

    /** UI-020: the dashed `fg` line at 75% opacity. */
    private fun drawIdealLine(canvas: Canvas, fraction: Float, colors: DesignSystemColors) {
        val line = CupGeometry.idealLine(fraction.toDouble())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = colors.fg.copy(alpha = CupGeometry.IDEAL_OPACITY.toFloat()).toArgb()
            strokeWidth = CupGeometry.IDEAL_STROKE.toFloat()
            strokeCap = Paint.Cap.ROUND
            pathEffect = DashPathEffect(CupGeometry.IDEAL_DASH.map { it.toFloat() }.toFloatArray(), 0f)
        }
        val y = line.y.toFloat()
        canvas.drawPath(Path().apply { moveTo(line.x1.toFloat(), y); lineTo(line.x2.toFloat(), y) }, paint)
    }

    /** BRAND-013's outline in `grey`. */
    private fun drawCup(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = CupGeometry.GREY.toInt()
            strokeWidth = CupGeometry.CUP_STROKE.toFloat()
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(CupGeometry.cupPath.toPath(), paint)
    }

    /** UI-021: the number, and the unit under it, fitted to the text box. */
    private fun drawValue(canvas: Canvas, label: CupLabel.Value, typeface: Typeface?, colors: DesignSystemColors) {
        val number = textPaint(typeface, REFERENCE_SIZE)
        val unitSize = (REFERENCE_SIZE * CupGeometry.UNIT_SIZE).toFloat()
        val unitBaseline = (REFERENCE_SIZE * CupGeometry.UNIT_BASELINE).toFloat()
        val unit = textPaint(typeface, unitSize)
        // Each line's box: its advance across, its font's ascent to descent down (as the mockups measured).
        fun lineBox(paint: Paint, text: String, baseline: Float): Box {
            val half = paint.measureText(text) / 2.0
            val metrics = paint.fontMetrics
            return Box(-half, (baseline + metrics.ascent).toDouble(), half, (baseline + metrics.descent).toDouble())
        }
        var block = lineBox(number, label.number, 0f)
        label.unit?.let { block = block.union(lineBox(unit, it, unitBaseline)) }
        val fit = CupGeometry.fitText(block)

        canvas.save()
        canvas.translate(fit.dx.toFloat(), fit.dy.toFloat())
        canvas.scale(fit.scale.toFloat(), fit.scale.toFloat())
        val outline = colors.bg.copy(alpha = CupGeometry.OUTLINE_OPACITY.toFloat()).toArgb()
        val lines = listOfNotNull(
            Triple(number, label.number, 0f),
            label.unit?.let { Triple(unit, it, unitBaseline) },
        )
        // The outline first, so the fill covers its inner half.
        for ((paint, text, baseline) in lines) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = CupGeometry.TEXT_OUTLINE.toFloat()
            paint.strokeJoin = Paint.Join.ROUND
            paint.color = outline
            canvas.drawText(text, 0f, baseline, paint)
        }
        for ((paint, text, baseline) in lines) {
            paint.style = Paint.Style.FILL
            paint.color = colors.fg.toArgb()
            canvas.drawText(text, 0f, baseline, paint)
        }
        canvas.restore()
    }

    /** UI-022: the check in place of the text, fitted to the box at 75% of its height. */
    private fun drawCheck(canvas: Canvas, colors: DesignSystemColors) {
        val fit = CupGeometry.fitText(CupGeometry.checkBox, CupGeometry.CHECK_HEIGHT_SHARE)
        val path = Path()
        CupGeometry.CHECK_POINTS.forEachIndexed { i, (x, y) ->
            if (i == 0) path.moveTo(x.toFloat(), y.toFloat()) else path.lineTo(x.toFloat(), y.toFloat())
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.save()
        canvas.translate(fit.dx.toFloat(), fit.dy.toFloat())
        canvas.scale(fit.scale.toFloat(), fit.scale.toFloat())
        paint.color = colors.bg.copy(alpha = CupGeometry.OUTLINE_OPACITY.toFloat()).toArgb()
        paint.strokeWidth = CupGeometry.CHECK_OUTLINE.toFloat()
        canvas.drawPath(path, paint)
        paint.color = colors.fg.toArgb()
        paint.strokeWidth = CupGeometry.CHECK_STROKE.toFloat()
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun textPaint(typeface: Typeface?, size: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = size
        textAlign = Paint.Align.CENTER
    }

    private fun List<PathOp>.toPath(): Path = Path().also { path ->
        forEach { op ->
            when (op) {
                is PathOp.MoveTo -> path.moveTo(op.x.toFloat(), op.y.toFloat())
                is PathOp.LineTo -> path.lineTo(op.x.toFloat(), op.y.toFloat())
                is PathOp.QuadTo -> path.quadTo(op.cx.toFloat(), op.cy.toFloat(), op.x.toFloat(), op.y.toFloat())
                PathOp.Close -> path.close()
            }
        }
    }
}
