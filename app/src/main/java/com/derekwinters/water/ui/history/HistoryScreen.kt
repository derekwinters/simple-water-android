package com.derekwinters.water.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.derekwinters.water.WaterApp
import com.derekwinters.water.core.DaySummary
import com.derekwinters.water.core.History
import com.derekwinters.water.core.Units
import com.derekwinters.water.core.VolumeUnit
import com.derekwinters.water.core.WeekSummary
import com.derekwinters.water.data.UserSettings
import com.derekwinters.water.designsystem.AppText
import com.derekwinters.water.designsystem.AppTheme
import com.derekwinters.water.designsystem.ListLayout
import com.derekwinters.water.designsystem.NumericText
import com.derekwinters.water.designsystem.SecondaryButton
import com.derekwinters.water.designsystem.TextRole
import com.derekwinters.water.ui.formatDay
import com.derekwinters.water.ui.rememberToday
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HistoryRoute(app: WaterApp, settings: State<UserSettings>, onBack: () -> Unit) {
    val current = settings.value
    val today by rememberToday()
    val zone = remember(today) { ZoneId.systemDefault() }
    // HIST-001: the window's last day, as an epoch day so it survives recreation.
    var endEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    val end = LocalDate.ofEpochDay(endEpochDay).let { if (it.isAfter(today)) today else it }

    val entries by remember(end, zone) { app.repository.entriesBetween(end.minusDays(6), end, zone) }
        .collectAsState(initial = emptyList())
    val records by app.repository.goalRecords.collectAsState(initial = emptyList())
    val week = History.week(
        endDate = end,
        today = today,
        totals = History.totalsByDay(entries.map { it.toLoggedAmount() }, zone),
        records = records,
        currentGoalMl = current.goalMl,
    )

    HistoryScreen(
        week = week,
        unit = current.volumeUnit,
        canGoForward = History.canGoForward(end, today),
        onPrevious = { endEpochDay = History.previousEnd(end).toEpochDay() },
        onNext = { endEpochDay = History.nextEnd(end, today).toEpochDay() },
        onBack = onBack,
    )
}

/** UI-005, HIST-001–004. */
@Composable
fun HistoryScreen(
    week: WeekSummary,
    unit: VolumeUnit,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    ListLayout(title = "History", modifier = modifier, onBack = onBack) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                NumericText(
                    text = "${week.metCount} of ${week.days.size}",
                    style = AppTheme.numericTypography.stat,
                )
                AppText(text = "days met their goal", role = TextRole.Meta)
                AppText(
                    text = "Average ${Units.formatVolume(week.averageMl, unit)} a day",
                    role = TextRole.Body,
                    modifier = Modifier.padding(top = spacing.sm),
                )
            }
        }
        item {
            val first = week.days.firstOrNull()?.date
            val last = week.days.lastOrNull()?.date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                SecondaryButton(text = "‹ Previous", onClick = onPrevious)
                AppText(
                    text = if (first != null && last != null) "${formatDay(first)} – ${formatDay(last)}" else "",
                    role = TextRole.Meta,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(text = "Next ›", onClick = onNext, enabled = canGoForward)
            }
        }
        // Newest first, like today's entries on home.
        items(week.days.reversed(), key = { it.date.toEpochDay() }) { day ->
            DayRow(day = day, unit = unit)
        }
    }
}

/** HIST-003: weekday and date, the total against that day's goal, a bar and a "met" mark. */
@Composable
private fun DayRow(day: DaySummary, unit: VolumeUnit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = formatDay(day.date), role = TextRole.BodyStrong, modifier = Modifier.weight(1f))
            NumericText(
                text = "${Units.formatNumber(day.totalMl, unit)} / ${Units.formatVolume(day.goalMl, unit)}",
                style = AppTheme.numericTypography.inline,
                color = if (day.met) colors.work else colors.fg,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(spacing.sm)
                .clip(RoundedCornerShape(spacing.xs))
                .background(colors.chip),
        ) {
            if (day.fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(day.fraction)
                        .fillMaxHeight()
                        .background(colors.work),
                )
            }
        }
        if (day.met) {
            AppText(text = "✓ Goal met", role = TextRole.Meta, color = colors.work)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    val end = LocalDate.of(2026, 10, 11)
    val week = History.week(
        endDate = end,
        today = end,
        totals = mapOf(end to 2450.0, end.minusDays(1) to 1200.0, end.minusDays(3) to 3000.0),
        records = emptyList(),
        currentGoalMl = 2450,
    )
    AppTheme {
        HistoryScreen(week = week, unit = VolumeUnit.ML, canGoForward = false, onPrevious = {}, onNext = {}, onBack = {})
    }
}
