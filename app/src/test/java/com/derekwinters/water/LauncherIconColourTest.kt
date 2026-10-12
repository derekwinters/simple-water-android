package com.derekwinters.water

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * BRAND-002, BRAND-003: the launcher icon's resources are painted in the fixed brand palette. The
 * expected values are copied from docs/spec/brand.md, never read from DesignSystemColors. Also the
 * icon's structure: BRAND-012, BRAND-013, BRAND-030, BRAND-031, BRAND-055 and the baked-coordinates
 * invariant.
 */
class LauncherIconColourTest {
    private val background = "background" to 0xFFFFFFFF.toInt()
    private val blue = "blue" to 0xFF1361A7.toInt()
    private val grey = "grey" to 0xFF818589.toInt()
    private val white = "white" to 0xFFFFFFFF.toInt()

    @Test
    fun `the launcher background colour is the palette's background`() {
        val resource = AppModuleFiles.file("src/main/res/values/ic_launcher_background.xml")
        assertTrue("No ${resource.path} (BRAND-051).", resource.isFile)
        val declared = AppModuleFiles.parse(resource).getElementsByTagName("color").elements()
            .singleOrNull { it.getAttribute("name") == "ic_launcher_background" }
        assertTrue("No <color name=\"ic_launcher_background\"> (BRAND-051).", declared != null)
        assertColour("the launcher background", background, declared!!.textContent.trim())
    }

    @Test
    fun `the foreground's two elements are painted in their palette roles, in order`() {
        val drawable = AppModuleFiles.file("src/main/res/drawable/ic_launcher_foreground.xml")
        assertTrue("No ${drawable.path} (BRAND-051).", drawable.isFile)
        val paths = AppModuleFiles.parse(drawable).getElementsByTagName("path").elements()
        assertEquals("The foreground draws exactly BRAND-012 and BRAND-013, one <path> each.", 2, paths.size)
        val (water, cup) = paths
        // BRAND-012: the water is filled and stroked in blue.
        assertColour("the water's fill (BRAND-012)", blue, water.getAttribute("android:fillColor"))
        assertColour("the water's stroke (BRAND-012)", blue, water.getAttribute("android:strokeColor"))
        // BRAND-013: the cup is a grey stroke with no fill.
        assertColour("the cup's stroke (BRAND-013)", grey, cup.getAttribute("android:strokeColor"))
        assertNoFill("the cup (BRAND-013)", cup)
    }

    @Test
    fun `the monochrome layer is the same geometry in white, with a stroke-free water`() {
        val foreground = AppModuleFiles.parse(AppModuleFiles.file("src/main/res/drawable/ic_launcher_foreground.xml"))
            .getElementsByTagName("path").elements()
        val monochrome = AppModuleFiles.parse(AppModuleFiles.file("src/main/res/drawable/ic_launcher_monochrome.xml"))
            .getElementsByTagName("path").elements()
        // BRAND-030: same paths, every paint white.
        assertEquals(foreground.map { it.getAttribute("android:pathData") }, monochrome.map { it.getAttribute("android:pathData") })
        val (water, cup) = monochrome
        assertColour("the monochrome water (BRAND-030)", white, water.getAttribute("android:fillColor"))
        assertColour("the monochrome cup (BRAND-030)", white, cup.getAttribute("android:strokeColor"))
        assertNoFill("the monochrome cup (BRAND-030)", cup)
        // BRAND-031: no stroke on the water, so the gap to the cup stays visible.
        assertNoStroke("the monochrome water (BRAND-031)", water)
    }

    @Test
    fun `the notification icon is the cup and water as a white silhouette`() {
        val paths = AppModuleFiles.parse(AppModuleFiles.file("src/main/res/drawable/ic_notification.xml"))
            .getElementsByTagName("path").elements()
        // BRAND-055: water filled, cup stroked, both white; the water has no stroke.
        assertEquals("The notification icon draws the water and the cup, one <path> each (BRAND-055).", 2, paths.size)
        val (water, cup) = paths
        assertColour("the notification water (BRAND-055)", white, water.getAttribute("android:fillColor"))
        assertNoStroke("the notification water (BRAND-055)", water)
        assertColour("the notification cup (BRAND-055)", white, cup.getAttribute("android:strokeColor"))
        assertNoFill("the notification cup (BRAND-055)", cup)
    }

    @Test
    fun `every icon path uses absolute, baked commands`() {
        // Invariant (docs/spec/brand.md): only M, L, Q and Z, in final canvas coordinates; no groups.
        listOf("ic_launcher_foreground.xml", "ic_launcher_monochrome.xml", "ic_notification.xml").forEach { name ->
            val document = AppModuleFiles.parse(AppModuleFiles.file("src/main/res/drawable/$name"))
            assertEquals("$name has a <group>; bake its transform into the paths.", 0, document.getElementsByTagName("group").length)
            document.getElementsByTagName("path").elements().forEach { path ->
                val data = path.getAttribute("android:pathData")
                val commands = data.filter { it.isLetter() }.toSet()
                assertTrue("$name path \"$data\" uses commands other than M, L, Q, Z.", commands.all { it in "MLQZ" })
            }
        }
    }

    private fun assertNoFill(role: String, path: Element) {
        val fill = path.getAttribute("android:fillColor")
        assertTrue("$role has fill \"$fill\"; it must have none.", fill.isEmpty() || alpha(fill) == 0)
    }

    private fun assertNoStroke(role: String, path: Element) {
        val colour = path.getAttribute("android:strokeColor")
        val width = path.getAttribute("android:strokeWidth")
        assertTrue(
            "$role has a stroke (colour \"$colour\", width \"$width\"); it must have none.",
            colour.isEmpty() || alpha(colour) == 0 || width.isEmpty() || width.toFloat() == 0f,
        )
    }

    private fun alpha(colour: String): Int = parseAndroidColour(colour) ushr 24

    private fun assertColour(role: String, colour: Pair<String, Int>, written: String) {
        val (name, argb) = colour
        assertEquals(
            "$role is painted \"$written\", not the palette's `$name` (docs/spec/brand.md BRAND-002).",
            hex(argb),
            hex(parseAndroidColour(written)),
        )
    }

    private fun parseAndroidColour(value: String): Int {
        val digits = value.removePrefix("#")
        val argb = if (digits.length == 6) "FF$digits" else digits
        require(argb.length == 8) { "\"$value\" is not an Android colour literal." }
        return argb.toLong(16).toInt()
    }

    private fun hex(argb: Int): String = "#%08X".format(argb)
}
