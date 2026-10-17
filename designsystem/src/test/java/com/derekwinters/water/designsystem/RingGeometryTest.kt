package com.derekwinters.water.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `DS-027`: where `RingBitmap` draws `DS-017`'s ring and `DS-026`'s outer ring in a square. The dp
 * sizes are the spec's (`spacing.md` inner stroke, `spacing.xs` outer stroke, gap and tick, 2dp
 * tick width), written out here rather than read from `Spacing`.
 */
class RingGeometryTest {
    private fun geometry(sidePx: Float, density: Float) = RingGeometry.of(
        sidePx = sidePx,
        density = density,
        innerStrokeDp = 12f,
        outerStrokeDp = 4f,
        outerGapDp = 4f,
        tickLengthDp = 4f,
        tickWidthDp = 2f,
    )

    /** DS-027: the ticks reach the square's edge; outer ring, gap and inner ring follow inwards. */
    @Test
    fun `the rings fill the square from the ticks inwards`() {
        val g = geometry(sidePx = 200f, density = 2f)
        assertEquals(100f, g.tickTo, 1e-4f)
        assertEquals(92f, g.tickFrom, 1e-4f)
        assertEquals(4f, g.tickWidth, 1e-4f)
        assertEquals(8f, g.outerStroke, 1e-4f)
        assertEquals(88f, g.outerRadius, 1e-4f)
        assertEquals(24f, g.innerStroke, 1e-4f)
        // The outer ring's inside edge (84) less the 8px gap is the inner ring's outside edge (76).
        assertEquals(64f, g.innerRadius, 1e-4f)
        assertEquals(52f, g.openingRadius, 1e-4f)
    }

    /** DS-027: strokes stay their dp size; only the radii follow the square. */
    @Test
    fun `strokes keep their dp size whatever the square`() {
        val small = geometry(sidePx = 100f, density = 1f)
        val large = geometry(sidePx = 400f, density = 1f)
        assertEquals(small.innerStroke, large.innerStroke, 0f)
        assertEquals(small.outerStroke, large.outerStroke, 0f)
        assertEquals(26f, small.openingRadius, 1e-4f)
        assertEquals(176f, large.openingRadius, 1e-4f)
    }

    /**
     * DS-027: nothing is drawn outside the square, and the centre has room, from the smallest
     * square the widget draws (a 110dp 2 × 2 widget less `spacing.sm` each side, UI-014) upwards.
     */
    @Test
    fun `everything fits inside the square`() {
        val density = 2.625f
        for (sideDp in listOf(94f, 150f, 300f)) {
            val side = sideDp * density
            val g = geometry(sidePx = side, density = density)
            assertTrue(g.tickTo <= side / 2 + 1e-3f)
            assertTrue(g.outerRadius + g.outerStroke / 2 <= g.tickFrom + 1e-3f)
            assertTrue(g.innerRadius + g.innerStroke / 2 < g.outerRadius - g.outerStroke / 2)
            assertTrue(g.openingRadius > 0f)
        }
    }

    /** DS-027: a square too small for the strokes leaves no opening rather than a negative one. */
    @Test
    fun `a tiny square has no opening`() {
        val g = geometry(sidePx = 20f, density = 1f)
        assertEquals(0f, g.openingRadius, 0f)
        assertTrue(g.innerRadius >= 0f)
    }
}
