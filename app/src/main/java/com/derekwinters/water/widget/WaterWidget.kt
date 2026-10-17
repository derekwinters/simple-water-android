package com.derekwinters.water.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
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
import com.derekwinters.water.core.RingState
import com.derekwinters.water.designsystem.DesignSystemColors
import com.derekwinters.water.designsystem.RingBitmap
import com.derekwinters.water.designsystem.Spacing
import kotlin.math.roundToInt

/**
 * The home screen widget (docs/spec/screens.md UI-014–019): today's two rings with the app's cup
 * in the middle, and nothing else. The rings come from `Rings` in `:core` (UI-015 invariant) and are
 * drawn to a bitmap by the design system's `RingBitmap` (DS-027), since a widget cannot draw arcs.
 */
class WaterWidget : GlanceAppWidget() {
    /** UI-014: composed for the widget's actual size, so the rings follow every resize. */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as WaterApp
        // Widget invariant: computed now, for today, every time the launcher asks (UI-018).
        val drawn = WaterWidgetUpdater.current(app)
        WaterWidgetUpdater.armMidnight(app)
        provideContent {
            // UI-018: a refresh while this session is running stores newer rings in the state.
            val rings = WaterWidgetUpdater.newer(
                drawn = drawn,
                storedRings = currentState(WaterWidgetUpdater.RINGS_KEY),
                storedAtMillis = currentState(WaterWidgetUpdater.AT_KEY),
            )
            WidgetContent(rings)
        }
    }
}

/** UI-015–017: the card, the rings and the cup, and the tap that opens home. */
@Composable
private fun WidgetContent(rings: RingState) {
    val context = LocalContext.current
    val colors = DesignSystemColors.Default
    val spacing = Spacing.Default
    val size = LocalSize.current
    // UI-014: the rings fill the smaller side, less spacing.sm on each side, centred.
    val side = (minOf(size.width, size.height) - spacing.sm * 2).coerceAtLeast(spacing.sm)
    val density = context.resources.displayMetrics.density
    // UI-016, BRAND-056: the launcher foreground itself, never a copy of the cup.
    val cup = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)?.mutate()
    val bitmap = RingBitmap.render(
        sidePx = (side.value * density).roundToInt(),
        density = density,
        fraction = rings.fraction,
        outerFraction = rings.outerFraction,
        outerTicks = rings.outerTicks,
        centre = cup,
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
            modifier = GlanceModifier.size(side),
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
