package com.derekwinters.water.designsystem

/**
 * `DS-027`: where `RingBitmap` draws in a square of [sidePx]: every value is in pixels, measured
 * from the square's centre. Radii are stroke centre lines, as `Canvas.drawCircle` takes them.
 *
 * The rings fill the square: the outer ring's ticks reach its edge, then come the outer ring, the
 * gap and the inner ring (`DS-017`, `DS-026`). The outer ring's room is always kept, so the inner
 * ring is the same size whether or not the outer ring is drawn. Plain arithmetic, no Android types,
 * so it is tested on the JVM.
 */
data class RingGeometry(
    val innerRadius: Float,
    val innerStroke: Float,
    val outerRadius: Float,
    val outerStroke: Float,
    val tickFrom: Float,
    val tickTo: Float,
    val tickWidth: Float,
    /** The radius of the circle inside the inner stroke, where a centre drawable goes. */
    val openingRadius: Float,
) {
    companion object {
        fun of(
            sidePx: Float,
            density: Float,
            innerStrokeDp: Float,
            outerStrokeDp: Float,
            outerGapDp: Float,
            tickLengthDp: Float,
            tickWidthDp: Float,
        ): RingGeometry {
            val innerStroke = innerStrokeDp * density
            val outerStroke = outerStrokeDp * density
            val tickTo = sidePx / 2
            val tickFrom = tickTo - tickLengthDp * density
            val outerRadius = tickFrom - outerStroke / 2
            val innerOutsideEdge = outerRadius - outerStroke / 2 - outerGapDp * density
            val innerRadius = (innerOutsideEdge - innerStroke / 2).coerceAtLeast(0f)
            return RingGeometry(
                innerRadius = innerRadius,
                innerStroke = innerStroke,
                outerRadius = outerRadius,
                outerStroke = outerStroke,
                tickFrom = tickFrom,
                tickTo = tickTo,
                tickWidth = tickWidthDp * density,
                openingRadius = (innerRadius - innerStroke / 2).coerceAtLeast(0f),
            )
        }
    }
}
