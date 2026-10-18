package com.derekwinters.water.core

import java.util.Locale

/** A rectangle, y downwards: icon units in [CupGeometry], or whatever units its caller measures in. */
data class Box(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    val width: Double get() = right - left
    val height: Double get() = bottom - top
    val centreX: Double get() = (left + right) / 2
    val centreY: Double get() = (top + bottom) / 2

    fun union(other: Box): Box =
        Box(minOf(left, other.left), minOf(top, other.top), maxOf(right, other.right), maxOf(bottom, other.bottom))
}

/** One command of a path in absolute coordinates (BRAND's "coordinates are absolute and baked"). */
sealed interface PathOp {
    data class MoveTo(val x: Double, val y: Double) : PathOp
    data class LineTo(val x: Double, val y: Double) : PathOp
    data class QuadTo(val cx: Double, val cy: Double, val x: Double, val y: Double) : PathOp
    data object Close : PathOp
}

/** The path as SVG path data, for comparing with the master icon (BRAND-017). */
fun List<PathOp>.toSvg(): String = joinToString(" ") { op ->
    fun n(v: Double) = String.format(Locale.US, "%.6f", v).trimEnd('0').trimEnd('.')
    when (op) {
        is PathOp.MoveTo -> "M${n(op.x)} ${n(op.y)}"
        is PathOp.LineTo -> "L${n(op.x)} ${n(op.y)}"
        is PathOp.QuadTo -> "Q${n(op.cx)} ${n(op.cy)} ${n(op.x)} ${n(op.y)}"
        PathOp.Close -> "Z"
    }
}

/** UI-020: a horizontal line from [x1] to [x2] at height [y], in icon units. */
data class Line(val x1: Double, val x2: Double, val y: Double)

/** UI-015: icon unit (x, y) lands at (left + (x − BOUNDS.left) × scale, top + (y − BOUNDS.top) × scale). */
data class Placement(val scale: Double, val left: Double, val top: Double)

/** UI-021, UI-022: a measured point (x, y) is drawn at icon units ([x], [y]). */
data class TextFit(val scale: Double, val dx: Double, val dy: Double) {
    fun x(x: Double): Double = x * scale + dx
    fun y(y: Double): Double = y * scale + dy
}

/**
 * The home screen widget's cup (docs/spec/brand.md BRAND-056; docs/spec/screens.md UI-015, UI-016,
 * UI-020–022): the master icon's cup and water with the water at any level, the ideal line, and
 * where the text and the check go. Everything is in icon units (BRAND-001); `CupBitmap` scales it
 * to the widget. Invariant: the path data and colours are the master icon's (BRAND-017), never an
 * approximation; at three-quarters the water is BRAND-012 exactly.
 */
object CupGeometry {
    /** BRAND-002 `blue`, the water, as ARGB. */
    const val BLUE = 0xFF1361A7L

    /** BRAND-002 `grey`, the cup, as ARGB. */
    const val GREY = 0xFF818589L

    /** BRAND-013. */
    const val CUP_STROKE = 4.2

    /** BRAND-012. */
    const val WATER_STROKE = 1.26

    /** BRAND-012's bottom edge and its bottom corners. */
    private const val WATER_BOTTOM = 72.48
    private const val WATER_BOTTOM_LEFT = 44.424
    private const val WATER_BOTTOM_RIGHT = 63.576

    /** BRAND-012's sides move out 0.1 unit for every unit up. */
    private const val SIDE_SLOPE = 0.1

    /** BRAND-012's top at three-quarters is 45.6, so full is 26.88 / 0.75 above the bottom. */
    private const val FULL_HEIGHT = (WATER_BOTTOM - 45.6) / 0.75

    /** BRAND-012: the half-waves' control points sit this far above and below the top line. */
    private const val WAVE_CONTROL = 2.52

    /** BRAND-056: a low wave is clipped here, so it never dips below the bottom. */
    const val WATER_CLIP_BOTTOM = WATER_BOTTOM + WATER_STROKE / 2

    /** UI-020. */
    const val IDEAL_STROKE = 0.9
    val IDEAL_DASH = listOf(2.2, 1.6)
    const val IDEAL_OPACITY = 0.75
    private const val IDEAL_OVERHANG = 0.6

    /** BRAND-056: the cup's outline with its stroke; UI-015 scales this box to the card. */
    val BOUNDS = Box(37.2 - CUP_STROKE / 2, 33.84 - CUP_STROKE / 2, 70.8 + CUP_STROKE / 2, 76.68 + CUP_STROKE / 2)

    /** BRAND-013, exactly. */
    val cupPath: List<PathOp> = listOf(
        PathOp.MoveTo(37.2, 33.84),
        PathOp.LineTo(41.064, 73.32),
        PathOp.QuadTo(41.4, 76.68, 44.76, 76.68),
        PathOp.LineTo(63.24, 76.68),
        PathOp.QuadTo(66.6, 76.68, 66.936, 73.32),
        PathOp.LineTo(70.8, 33.84),
    )

    /** UI-015: the water's top line at [fraction] of full, clamped to 0–1. */
    fun waterTop(fraction: Double): Double = WATER_BOTTOM - FULL_HEIGHT * fraction.coerceIn(0.0, 1.0)

    /** BRAND-056: the water's left and right edges at height [y], continuing BRAND-012's sides. */
    fun waterEdges(y: Double): Pair<Double, Double> {
        val out = (WATER_BOTTOM - y) * SIDE_SLOPE
        return (WATER_BOTTOM_LEFT - out) to (WATER_BOTTOM_RIGHT + out)
    }

    /** BRAND-056: the water at [fraction] of full, or null when empty (nothing is drawn). */
    fun waterPath(fraction: Double): List<PathOp>? {
        if (fraction <= 0.0) return null
        val top = waterTop(fraction)
        val (left, right) = waterEdges(top)
        val half = (right - left) / 4
        val waves = (0 until 4).map { i ->
            PathOp.QuadTo(
                cx = left + half * (i + 0.5),
                cy = top + if (i % 2 == 0) -WAVE_CONTROL else WAVE_CONTROL,
                x = left + half * (i + 1),
                y = top,
            )
        }
        return listOf(PathOp.MoveTo(left, top)) + waves + listOf(
            PathOp.LineTo(WATER_BOTTOM_RIGHT, WATER_BOTTOM),
            PathOp.LineTo(WATER_BOTTOM_LEFT, WATER_BOTTOM),
            PathOp.Close,
        )
    }

    /** UI-020: the ideal line at [fraction] of full, 0.6 past the water's edges on each side. */
    fun idealLine(fraction: Double): Line {
        val y = waterTop(fraction)
        val (left, right) = waterEdges(y)
        return Line(left - IDEAL_OVERHANG, right + IDEAL_OVERHANG, y)
    }

    /** UI-015: the cup as large as fits in a [width] × [height] card less [padding] each side, centred. */
    fun placement(width: Double, height: Double, padding: Double): Placement {
        val scale = minOf((width - 2 * padding) / BOUNDS.width, (height - 2 * padding) / BOUNDS.height)
            .coerceAtLeast(0.0)
        return Placement(scale, (width - BOUNDS.width * scale) / 2, (height - BOUNDS.height * scale) / 2)
    }

    /** UI-021: the text's centre: half the full water height. */
    private const val TEXT_CENTRE_Y = WATER_BOTTOM - FULL_HEIGHT / 2

    /** UI-021: padding from the water's edges, measured this far below the centre. */
    private const val TEXT_PADDING = 2.6
    private const val TEXT_MEASURED_BELOW = 6.0
    private const val TEXT_HEIGHT = 17.5

    /** UI-021: the box the text is fitted to, in icon units. */
    val TEXT_BOX: Box = run {
        val (left, right) = waterEdges(TEXT_CENTRE_Y + TEXT_MEASURED_BELOW)
        val width = right - left - 2 * TEXT_PADDING
        Box(54 - width / 2, TEXT_CENTRE_Y - TEXT_HEIGHT / 2, 54 + width / 2, TEXT_CENTRE_Y + TEXT_HEIGHT / 2)
    }

    /** UI-021: the unit's size, and its baseline below the number's, as fractions of the number's size. */
    const val UNIT_SIZE = 0.42
    const val UNIT_BASELINE = 0.46

    /** UI-021: the text's `bg` outline: its opacity, and its stroke where the number's size is 100. */
    const val OUTLINE_OPACITY = 0.35
    const val TEXT_OUTLINE = 12.0

    /** UI-022: the check, in a frame where the number's size is 100. */
    val CHECK_POINTS = listOf(-30.0 to 2.0, -10.0 to 22.0, 30.0 to -22.0)
    const val CHECK_STROKE = 13.0
    const val CHECK_OUTLINE = 20.0

    /** UI-022: the check's share of the text box's height. */
    const val CHECK_HEIGHT_SHARE = 0.75

    /** UI-022: the box [CHECK_POINTS] span, fitted like text. */
    val checkBox: Box = Box(
        CHECK_POINTS.minOf { it.first },
        CHECK_POINTS.minOf { it.second },
        CHECK_POINTS.maxOf { it.first },
        CHECK_POINTS.maxOf { it.second },
    )

    /**
     * UI-021, UI-022: scales a measured [block] (the number and unit together, or the check) as
     * large as fits [TEXT_BOX], at [heightShare] of its height, and centres it there.
     */
    fun fitText(block: Box, heightShare: Double = 1.0): TextFit {
        val scale = if (block.width <= 0 || block.height <= 0) {
            0.0
        } else {
            minOf(TEXT_BOX.width / block.width, TEXT_BOX.height * heightShare / block.height)
        }
        return TextFit(scale, TEXT_BOX.centreX - block.centreX * scale, TEXT_BOX.centreY - block.centreY * scale)
    }
}
