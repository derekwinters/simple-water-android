package com.derekwinters.water.designsystem

import androidx.compose.ui.graphics.Color

/**
 * The bespoke semantic colour tokens (`DS-050`): `work`, `recovery`, `neutral`, `bg`, `fg`, `dim`,
 * `line`, `chip`, a destructive red, and this app's added `ideal` grey (`DS-053`). `:designsystem`'s own components read these directly,
 * never a raw colour value — the same palette is also mapped onto Material 3's `ColorScheme` roles
 * (`DS-051`; see `ColorSchemeMapping.kt`).
 *
 * `docs/spec/cues.md` (`CUE-030`, `CUE-031`) fixes what each interval kind's colour *means* — work
 * is green, recovery is yellow, warm-up and cool-down share a neutral, red is never used for a
 * kind — but `CUE-034` explicitly leaves the literal values to the design system. Neither
 * `CUE-034` nor `docs/spec/design-system.md` gives a hex value or a light/dark choice for any of
 * the nine tokens; [Default] is this implementation's reasonable choice for a dark-surfaced
 * running-screen palette, not a value fixed by either specification — see this change's pull
 * request description for that decision.
 */
data class DesignSystemColors(
    val work: Color,
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
            work = Color(0xFF2ECC71),
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
