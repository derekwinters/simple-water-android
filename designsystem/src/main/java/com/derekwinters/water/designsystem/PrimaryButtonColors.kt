package com.derekwinters.water.designsystem

import androidx.compose.ui.graphics.Color

/** `DS-001`: `PrimaryButton`'s fill. */
internal val DesignSystemColors.primaryButtonFill: Color get() = accent

/**
 * `DS-001`, `DS-054`: `PrimaryButton`'s text colour, chosen to reach 4.5:1 against
 * [primaryButtonFill] (`DesignSystemColorsTest` computes the ratio).
 */
internal val DesignSystemColors.primaryButtonInk: Color get() = fg
