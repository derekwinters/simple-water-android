package com.derekwinters.water.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * JetBrains Mono, bundled as a resource font (`res/font/jetbrains_mono_regular.ttf`) rather than
 * fetched at build or run time; its SIL Open Font License is `designsystem/OFL-jetbrains-mono.txt`.
 * Ported unchanged from Interval Trainer.
 */
val JetBrainsMono = FontFamily(Font(R.font.jetbrains_mono_regular, FontWeight.Normal))

/** "tnum": the OpenType feature tag for tabular (fixed-width) figures. */
private const val TABULAR_FIGURES = "tnum"

/**
 * The three numeric typographic roles (`DS-030`–`033`) — the only shapes a volume, weight or time
 * value renders in. Interval Trainer's `TimerTypography` (`timer.large`/`stat`/`inline`), renamed
 * because this app shows volumes rather than timers; the values are unchanged.
 */
data class NumericTypography(
    val large: TextStyle,
    val stat: TextStyle,
    val inline: TextStyle,
) {
    companion object {
        val Default = NumericTypography(
            // numeric.large (DS-030): today's total inside the progress ring.
            large = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize = 64.sp,
                fontFeatureSettings = TABULAR_FIGURES,
            ),
            // numeric.stat (DS-031): goals, stepper values, field values.
            stat = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize = 20.sp,
                fontFeatureSettings = TABULAR_FIGURES,
            ),
            // numeric.inline (DS-032): amounts in list rows.
            inline = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize = 16.sp,
                fontFeatureSettings = TABULAR_FIGURES,
            ),
        )
    }
}
