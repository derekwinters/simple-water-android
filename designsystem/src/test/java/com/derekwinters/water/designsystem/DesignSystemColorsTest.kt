package com.derekwinters.water.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/** `DS-053`: the `ideal` token is the brand grey (docs/spec/brand.md `grey`, #818589). */
class DesignSystemColorsTest {
    @Test
    fun `ideal is the brand grey`() {
        assertEquals(Color(0xFF818589), DesignSystemColors.Default.ideal)
    }
}
