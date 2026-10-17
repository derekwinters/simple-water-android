package com.derekwinters.water.designsystem

import androidx.compose.ui.graphics.Color

/**
 * The bespoke semantic colour tokens (`DS-050`): `accent`, `recovery`, `neutral`, `bg`, `fg`, `dim`,
 * `line`, `chip`, a destructive red, and this app's added `ideal` grey (`DS-053`). `:designsystem`'s
 * own components read these directly, never a raw colour value — the same palette is also mapped
 * onto Material 3's `ColorScheme` roles (`DS-051`; see `ColorSchemeMapping.kt`).
 *
 * `accent` is the brand blue (docs/spec/brand.md `BRAND-002` `blue`), renamed from Interval
 * Trainer's `work` green; `ideal` is the brand grey. The other values are carried over from
 * Interval Trainer unchanged. `docs/spec/design-system.md` (`DS-050`, `DS-053`) fixes every value.
 */
data class DesignSystemColors(
    val accent: Color,
    val recovery: Color,
    val neutral: Color,
    val bg: Color,
    val fg: Color,
    val dim: Color,
    val line: Color,
    val chip: Color,
    val destructive: Color,
    /** `DS-053`: the brand grey, for `ProgressRing`'s outer (ideal) ring (`DS-026`). */
    val ideal: Color,
) {
    companion object {
        val Default = DesignSystemColors(
            accent = Color(0xFF1361A7),
            recovery = Color(0xFFF5C518),
            neutral = Color(0xFF8A93A3),
            bg = Color(0xFF121212),
            fg = Color(0xFFF5F5F5),
            dim = Color(0xFF9AA0A6),
            line = Color(0xFF3C3C3C),
            chip = Color(0xFF262626),
            destructive = Color(0xFFE53935),
            ideal = Color(0xFF818589),
        )
    }
}
