package com.derekwinters.water.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The token values (`DS-050`, `DS-053`) and the primary button's text contrast (`DS-054`).
 * Expected hex values are constants here, never read back from another token.
 */
class DesignSystemColorsTest {
    private val colors = DesignSystemColors.Default

    /** DS-050: the accent is the brand blue (docs/spec/brand.md `blue`, #1361A7). */
    @Test
    fun `accent is the brand blue`() {
        assertEquals(Color(0xFF1361A7), colors.accent)
    }

    /** DS-053: the `ideal` token is the brand grey (docs/spec/brand.md `grey`, #818589). */
    @Test
    fun `ideal is the brand grey`() {
        assertEquals(Color(0xFF818589), colors.ideal)
    }

    /** DS-001, DS-054: the primary button is an `accent` fill with `fg` text. */
    @Test
    fun `primary button is fg text on an accent fill`() {
        assertEquals(colors.accent, colors.primaryButtonFill)
        assertEquals(colors.fg, colors.primaryButtonInk)
    }

    /** DS-054: the primary button's text reaches 4.5:1 against its fill. */
    @Test
    fun `primary button text meets 4_5 to 1 contrast`() {
        val ratio = contrastRatio(colors.primaryButtonInk, colors.primaryButtonFill)
        assertTrue("primary button contrast $ratio is below 4.5:1", ratio >= 4.5)
    }

    /** The contrast oracle itself, against WCAG's own fixed points. */
    @Test
    fun `contrast ratio matches WCAG reference values`() {
        assertEquals(21.0, contrastRatio(Color(0xFF000000), Color(0xFFFFFFFF)), 1e-6)
        assertEquals(1.0, contrastRatio(Color(0xFF1361A7), Color(0xFF1361A7)), 1e-6)
        // #767676 on white is the well-known lightest grey that passes 4.5:1 (4.54:1).
        assertEquals(4.54, contrastRatio(Color(0xFF767676), Color(0xFFFFFFFF)), 0.01)
    }

    /** WCAG 2.x contrast ratio: (L1 + 0.05) / (L2 + 0.05), L the relative luminance of sRGB. */
    private fun contrastRatio(a: Color, b: Color): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun relativeLuminance(c: Color): Double =
        0.2126 * linear(c.red) + 0.7152 * linear(c.green) + 0.0722 * linear(c.blue)

    private fun linear(channel: Float): Double {
        val v = channel.toDouble()
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
}
