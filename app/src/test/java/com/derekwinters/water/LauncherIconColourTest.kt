package com.derekwinters.water

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BRAND-002, BRAND-003: the launcher icon's resources are painted in the fixed brand palette. The
 * expected values are copied from docs/spec/brand.md, never read from DesignSystemColors.
 */
class LauncherIconColourTest {
    private val background = "background" to 0xFFFFFFFF.toInt()
    private val blue = "blue" to 0xFF1361A7.toInt()
    private val grey = "grey" to 0xFF818589.toInt()

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
        val expected = listOf("the drop (BRAND-010)" to blue, "the wave (BRAND-011)" to grey)
        assertEquals("The foreground draws exactly BRAND-010 and BRAND-011, one <path> each.", expected.size, paths.size)
        expected.forEachIndexed { index, (role, colour) ->
            assertColour(role, colour, paths[index].getAttribute("android:strokeColor"))
        }
    }

    @Test
    fun `the monochrome layer is the same geometry in white`() {
        val foreground = AppModuleFiles.parse(AppModuleFiles.file("src/main/res/drawable/ic_launcher_foreground.xml"))
            .getElementsByTagName("path").elements()
        val monochrome = AppModuleFiles.parse(AppModuleFiles.file("src/main/res/drawable/ic_launcher_monochrome.xml"))
            .getElementsByTagName("path").elements()
        // BRAND-030: same paths, every paint white.
        assertEquals(foreground.map { it.getAttribute("android:pathData") }, monochrome.map { it.getAttribute("android:pathData") })
        monochrome.forEach { assertColour("a monochrome element (BRAND-030)", "white" to 0xFFFFFFFF.toInt(), it.getAttribute("android:strokeColor")) }
    }

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
