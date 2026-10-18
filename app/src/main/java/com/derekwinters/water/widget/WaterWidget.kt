package com.derekwinters.water.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import com.derekwinters.water.MainActivity
import com.derekwinters.water.R
import com.derekwinters.water.WaterApp
import com.derekwinters.water.core.CupState
import com.derekwinters.water.designsystem.DesignSystemColors
import com.derekwinters.water.designsystem.Spacing

/**
 * The home screen widget (docs/spec/screens.md UI-014–023): the app's cup filling the card, the
 * water rising toward today's goal, with the ideal line and the optional text or check. The values
 * come from `WidgetCup` in `:core` (the widget invariants); `CupBitmap` draws them, since a widget
 * cannot draw paths.
 */
class WaterWidget : GlanceAppWidget() {
    /** UI-014: composed for the widget's actual size, so the cup follows every resize. */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as WaterApp
        // Widget invariant: computed now, for today, every time the launcher asks (UI-018).
        val drawn = WaterWidgetUpdater.current(app)
        WaterWidgetUpdater.armMidnight(app)
        provideContent {
            // UI-018: a refresh while this session is running stores a newer cup in the state.
            val cup = WaterWidgetUpdater.newer(
                drawn = drawn,
                storedCup = currentState(WaterWidgetUpdater.CUP_KEY),
                storedAtMillis = currentState(WaterWidgetUpdater.AT_KEY),
            )
            WidgetContent(cup)
        }
    }
}

/** UI-015–017, UI-020–022: the card, the cup, and the tap that opens home. */
@Composable
private fun WidgetContent(cup: CupState) {
    val context = LocalContext.current
    val colors = DesignSystemColors.Default
    val spacing = Spacing.Default
    val size = LocalSize.current
    val density = context.resources.displayMetrics.density
    // UI-021: the numeric font (DS-030).
    val typeface = ResourcesCompat.getFont(context, com.derekwinters.water.designsystem.R.font.jetbrains_mono_regular)
    // UI-015: the cup as large as fits the card less spacing.md each side; the bitmap is the cup alone.
    val bitmap = CupBitmap.render(
        cardWidthPx = size.width.value * density,
        cardHeightPx = size.height.value * density,
        paddingPx = spacing.md.value * density,
        state = cup,
        typeface = typeface,
        colors = colors,
    )
    // UI-017: the same request as the reminder notification's tap (REM-012).
    val openHome = Intent(context, MainActivity::class.java)
        .setAction(MainActivity.ACTION_OPEN_HOME)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.chip)
            .cornerRadius(spacing.md)
            .clickable(actionStartActivity(openHome)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = context.getString(R.string.app_name),
            modifier = GlanceModifier.size((bitmap.width / density).dp, (bitmap.height / density).dp),
        )
    }
}

/** UI-018: receives the launcher's broadcasts for [WaterWidget]. */
class WaterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WaterWidget()

    /** UI-018: the last widget was removed, so nothing needs the midnight refresh. */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WaterWidgetUpdater.cancelMidnight(context)
    }
}
