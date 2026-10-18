package com.derekwinters.water.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Requirement IDs refer to docs/spec/brand.md (BRAND-002, BRAND-012, BRAND-013, BRAND-017,
 * BRAND-056) and docs/spec/screens.md (UI-014, UI-015, UI-016, UI-020, UI-021, UI-022 and the
 * widget invariants). The master icon is read from docs/brand/icon.svg, never copied into the test.
 */
class CupGeometryTest {
    private val masterSvg: String by lazy {
        var directory: File? = File(System.getProperty("user.dir")!!).absoluteFile
        while (directory != null) {
            val svg = File(directory, "docs/brand/icon.svg")
            if (svg.isFile) return@lazy svg.readText()
            directory = directory.parentFile
        }
        throw IllegalStateException("No docs/brand/icon.svg above ${System.getProperty("user.dir")}.")
    }

    /** The `<path>` elements of the master SVG, in order: the water, then the cup. */
    private val masterPaths: List<String> by lazy { Regex("<path [^>]*/>").findAll(masterSvg).map { it.value }.toList() }

    private fun attribute(element: String, name: String): String =
        Regex("""\b$name="([^"]*)"""").find(element)!!.groupValues[1]

    /** An SVG path as its commands and numbers, so `M1 2` and `M1.0,2` compare equal. */
    private fun tokens(d: String): List<Any> = Regex("""[A-Za-z]|-?\d+(\.\d+)?""").findAll(d).map {
        it.value.toDoubleOrNull() ?: it.value
    }.toList()

    private fun assertSamePath(message: String, expected: String, actual: String) {
        val e = tokens(expected)
        val a = tokens(actual)
        assertEquals("$message: $actual", e.size, a.size)
        e.zip(a).forEach { (x, y) ->
            if (x is Double) assertEquals("$message: $actual", x, y as Double, 1e-9) else assertEquals(message, x, y)
        }
    }

    private fun assertBox(expected: Box, actual: Box) {
        listOf(expected.left to actual.left, expected.top to actual.top, expected.right to actual.right, expected.bottom to actual.bottom)
            .forEach { (e, a) -> assertEquals("$actual", e, a, 1e-9) }
    }

    // BRAND-056, BRAND-013, widget invariant: the cup is the master icon's outline, exactly.
    @Test
    fun theCupIsTheMasterIconsOutline() {
        assertSamePath("cup", attribute(masterPaths[1], "d"), CupGeometry.cupPath.toSvg())
        assertEquals(attribute(masterPaths[1], "stroke-width").toDouble(), CupGeometry.CUP_STROKE, 0.0)
    }

    // BRAND-056, BRAND-012: at three-quarters the water is the master icon's water, exactly.
    @Test
    fun atThreeQuartersTheWaterIsTheMasterIconsWater() {
        assertSamePath("water", attribute(masterPaths[0], "d"), CupGeometry.waterPath(0.75)!!.toSvg())
        assertEquals(attribute(masterPaths[0], "stroke-width").toDouble(), CupGeometry.WATER_STROKE, 0.0)
    }

    // BRAND-002, BRAND-056, widget invariant: the master's colours, not design-system tokens.
    @Test
    fun theColoursAreTheBrandPalette() {
        assertEquals(0xFF1361A7L, CupGeometry.BLUE)
        assertEquals(0xFF818589L, CupGeometry.GREY)
        assertEquals("#1361A7", attribute(masterPaths[0], "fill"))
        assertEquals("#1361A7", attribute(masterPaths[0], "stroke"))
        assertEquals("#818589", attribute(masterPaths[1], "stroke"))
        assertEquals(CupGeometry.BLUE, 0xFF000000L or attribute(masterPaths[0], "fill").drop(1).toLong(16))
        assertEquals(CupGeometry.GREY, 0xFF000000L or attribute(masterPaths[1], "stroke").drop(1).toLong(16))
    }

    // UI-015, BRAND-056: empty draws no water; full puts the top at 36.64; the level rises evenly.
    @Test
    fun theWaterRisesFromEmptyToFull() {
        assertNull(CupGeometry.waterPath(0.0))
        assertNull(CupGeometry.waterPath(-0.2))
        assertEquals(72.48, CupGeometry.waterTop(0.0), 1e-9)
        assertEquals(45.6, CupGeometry.waterTop(0.75), 1e-9)
        assertEquals(36.64, CupGeometry.waterTop(1.0), 1e-9)
        assertEquals(36.64, CupGeometry.waterTop(1.7), 1e-9)
        assertEquals(CupGeometry.waterTop(0.25) - CupGeometry.waterTop(0.5), CupGeometry.waterTop(0.5) - CupGeometry.waterTop(0.75), 1e-9)
    }

    /** The inner edge of the cup's left wall at height [y]: BRAND-013's centre line plus half its stroke. */
    private fun cupInnerLeft(y: Double): Double {
        val t = (y - 33.84) / (73.32 - 33.84)
        val centre = 37.2 + t * (41.064 - 37.2)
        val slope = (41.064 - 37.2) / (73.32 - 33.84)
        return centre + CupGeometry.CUP_STROKE / 2 * Math.sqrt(1 + slope * slope)
    }

    // BRAND-056: at every level the water (with its stroke) keeps clear of the cup's walls.
    @Test
    fun theWaterKeepsItsGapToTheCupAtEveryLevel() {
        for (step in 1..100) {
            val f = step / 100.0
            val path = CupGeometry.waterPath(f)!!
            val top = CupGeometry.waterTop(f)
            val (left, right) = CupGeometry.waterEdges(top)
            assertEquals(54.0, (left + right) / 2, 1e-9)
            // The first point is the top left; the half-waves split the width in four.
            val start = path.first() as PathOp.MoveTo
            assertEquals(left, start.x, 1e-9)
            assertEquals(top, start.y, 1e-9)
            val waves = path.filterIsInstance<PathOp.QuadTo>()
            assertEquals(4, waves.size)
            waves.forEachIndexed { i, q ->
                assertEquals(left + (right - left) * (i + 1) / 4, q.x, 1e-9)
                assertEquals(top + if (i % 2 == 0) -2.52 else 2.52, q.cy, 1e-9)
            }
            for (y in listOf(top, (top + 72.48) / 2, 72.48)) {
                val (l, _) = CupGeometry.waterEdges(y)
                val gap = l - CupGeometry.WATER_STROKE / 2 - cupInnerLeft(y)
                assertTrue("gap $gap at f=$f y=$y", gap > 0.5)
            }
        }
        assertEquals(72.48 + 0.63, CupGeometry.WATER_CLIP_BOTTOM, 1e-9)
    }

    // UI-020: the ideal line spans the water's width plus 0.6 each side, and stays off the walls.
    @Test
    fun theIdealLineSpansTheWaterAndClearsTheWalls() {
        assertEquals(0.9, CupGeometry.IDEAL_STROKE, 0.0)
        assertEquals(listOf(2.2, 1.6), CupGeometry.IDEAL_DASH)
        assertEquals(0.75, CupGeometry.IDEAL_OPACITY, 0.0)
        for (step in 0..20) {
            val f = step / 20.0
            val line = CupGeometry.idealLine(f)
            val (left, right) = CupGeometry.waterEdges(line.y)
            assertEquals(CupGeometry.waterTop(f), line.y, 1e-9)
            assertEquals(left - 0.6, line.x1, 1e-9)
            assertEquals(right + 0.6, line.x2, 1e-9)
            assertTrue("f=$f", line.x1 - CupGeometry.IDEAL_STROKE / 2 > cupInnerLeft(line.y))
        }
        assertEquals(CupGeometry.idealLine(1.0), CupGeometry.idealLine(1.4))
        assertEquals(CupGeometry.idealLine(0.0), CupGeometry.idealLine(-1.0))
    }

    // UI-015, UI-014: the cup fills the card less spacing.md each side, centred, at any size.
    @Test
    fun theCupFillsTheCardLessThePadding() {
        val bounds = CupGeometry.BOUNDS
        assertBox(Box(35.1, 31.74, 72.9, 78.78), bounds)
        for ((w, h) in listOf(110.0 to 110.0, 170.0 to 236.0, 300.0 to 140.0, 140.0 to 400.0)) {
            val p = CupGeometry.placement(w, h, padding = 12.0)
            val drawnW = bounds.width * p.scale
            val drawnH = bounds.height * p.scale
            assertTrue(drawnW <= w - 24 + 1e-9 && drawnH <= h - 24 + 1e-9)
            assertTrue("fills one side at $w x $h", Math.abs(drawnW - (w - 24)) < 1e-9 || Math.abs(drawnH - (h - 24)) < 1e-9)
            assertEquals(w / 2, p.left + drawnW / 2, 1e-9)
            assertEquals(h / 2, p.top + drawnH / 2, 1e-9)
        }
    }

    /**
     * The numeric font's metrics (JetBrains Mono, DS-030): every character advances 0.6 em, and a
     * line runs from 1.02 em above the baseline to 0.30 em below it.
     */
    private fun monoBox(text: String, size: Double, baseline: Double) =
        Box(-text.length * 0.6 * size / 2, baseline - 1.02 * size, text.length * 0.6 * size / 2, baseline + 0.30 * size)

    private fun block(number: String, unit: String?): Box {
        val n = monoBox(number, 100.0, 0.0)
        if (unit == null) return n
        return n.union(monoBox(unit, 100.0 * CupGeometry.UNIT_SIZE, 100.0 * CupGeometry.UNIT_BASELINE))
    }

    /** Where [box] lands in icon units after [fit]. */
    private fun placed(box: Box, fit: TextFit) =
        Box(fit.x(box.left), fit.y(box.top), fit.x(box.right), fit.y(box.bottom))

    /** The block's four corners are inside the water's edges with at least [padding], and above the bottom. */
    private fun assertInsideTheWalls(message: String, drawn: Box, padding: Double) {
        for (y in listOf(drawn.top, drawn.bottom)) {
            val (left, right) = CupGeometry.waterEdges(y)
            assertTrue("$message: left ${drawn.left} at y $y", drawn.left - left >= padding)
            assertTrue("$message: right ${drawn.right} at y $y", right - drawn.right >= padding)
        }
        assertTrue(message, drawn.top > CupGeometry.waterTop(1.0))
        assertTrue(message, drawn.bottom < 72.48)
    }

    // UI-021: the widest strings fit inside the walls at 2 × 2, centred on the cup.
    @Test
    fun theWidestTextFitsInsideTheWalls() {
        assertBox(Box(54 - 16.336 / 2, 54.56 - 17.5 / 2, 54 + 16.336 / 2, 54.56 + 17.5 / 2), CupGeometry.TEXT_BOX)
        // 2 × 2: a 110dp card at a common density; geometry is in icon units, so any size agrees.
        val card = CupGeometry.placement(110.0 * 2.625, 110.0 * 2.625, padding = 12.0 * 2.625)
        for ((number, unit) in listOf("99%" to null, "999" to "fl oz", "9.9" to "L", "45%" to null, "1" to "fl oz")) {
            val box = block(number, unit)
            val fit = CupGeometry.fitText(box)
            val drawn = placed(box, fit)
            assertInsideTheWalls(number, drawn, padding = 2.0)
            assertEquals(number, 54.0, (drawn.left + drawn.right) / 2, 1e-9)
            assertEquals(number, 54.56, (drawn.top + drawn.bottom) / 2, 1e-9)
            assertTrue(number, drawn.width <= 16.336 + 1e-9 && drawn.height <= 17.5 + 1e-9)
            assertTrue("$number fills the box one way", Math.abs(drawn.width - 16.336) < 1e-9 || Math.abs(drawn.height - 17.5) < 1e-9)
            // Legible at 2 × 2: the number's line is at least 8 px tall.
            assertTrue(number, 132 * fit.scale * card.scale >= 8)
        }
        // Longer text is drawn smaller, so "100" and "999" fit the same cup.
        assertTrue(CupGeometry.fitText(block("999", "fl oz")).scale < CupGeometry.fitText(block("99", "fl oz")).scale)
    }

    // UI-022: the check fits the same box at 75% of its height, centred on the same point.
    @Test
    fun theCheckFitsTheTextBoxAtThreeQuartersHeight() {
        assertEquals(listOf(-30.0 to 2.0, -10.0 to 22.0, 30.0 to -22.0), CupGeometry.CHECK_POINTS)
        assertEquals(13.0, CupGeometry.CHECK_STROKE, 0.0)
        // UI-021, UI-022: the outlines, in bg at 35%.
        assertEquals(20.0, CupGeometry.CHECK_OUTLINE, 0.0)
        assertEquals(12.0, CupGeometry.TEXT_OUTLINE, 0.0)
        assertEquals(0.35, CupGeometry.OUTLINE_OPACITY, 0.0)
        val box = CupGeometry.checkBox
        assertEquals(Box(-30.0, -22.0, 30.0, 22.0), box)
        val fit = CupGeometry.fitText(box, heightShare = 0.75)
        val drawn = placed(box, fit)
        assertTrue(drawn.width <= 16.336 + 1e-9 && drawn.height <= 17.5 * 0.75 + 1e-9)
        assertEquals(54.0, (drawn.left + drawn.right) / 2, 1e-9)
        assertEquals(54.56, (drawn.top + drawn.bottom) / 2, 1e-9)
        // With its round-capped stroke, each of the check's points still clears the walls.
        val half = CupGeometry.CHECK_STROKE / 2 * fit.scale
        for ((x, y) in CupGeometry.CHECK_POINTS) {
            val px = fit.x(x)
            val py = fit.y(y)
            val (left, right) = CupGeometry.waterEdges(py + half)
            assertTrue("check point ($x, $y)", px - half - left >= 1.0 && right - (px + half) >= 1.0)
            assertTrue("check point ($x, $y)", py - half > CupGeometry.waterTop(1.0) && py + half < 72.48)
        }
    }
}
