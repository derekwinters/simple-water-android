package com.derekwinters.water.ui.reminders

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.derekwinters.water.WaterApp
import com.derekwinters.water.core.ReminderSchedule
import com.derekwinters.water.core.ReminderSettings
import com.derekwinters.water.data.UserSettings
import com.derekwinters.water.designsystem.AppText
import com.derekwinters.water.designsystem.AppTheme
import com.derekwinters.water.designsystem.ChoiceChips
import com.derekwinters.water.designsystem.ListLayout
import com.derekwinters.water.designsystem.StockListItem
import com.derekwinters.water.designsystem.TextRole
import com.derekwinters.water.designsystem.Toggle
import com.derekwinters.water.designsystem.ValueStepper
import com.derekwinters.water.ui.formatMinuteOfDay
import com.derekwinters.water.ui.formatTime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun RemindersRoute(app: WaterApp, settings: State<UserSettings>, onBack: () -> Unit) {
    RemindersScreen(
        reminders = settings.value.reminders,
        // UI-008, REM-008: every change is saved and rescheduled at once.
        onChange = { transform ->
            app.appScope.launch {
                app.repository.updateSettings { it.copy(reminders = transform(it.reminders)) }
            }
        },
        onBack = onBack,
    )
}

/** UI-008. */
@Composable
fun RemindersScreen(
    reminders: ReminderSettings,
    onChange: ((ReminderSettings) -> ReminderSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val spacing = AppTheme.spacing
    val days = DayOfWeek.entries
    val step = ReminderSettings.TIME_STEP_MINUTES

    ListLayout(title = "Reminders", modifier = modifier, onBack = onBack) {
        item {
            StockListItem(
                headline = "Remind me to drink",
                supportingText = if (reminders.enabled) "On" else "Off",
                modifier = Modifier.fillMaxWidth(),
                trailingContent = {
                    Toggle(
                        checked = reminders.enabled,
                        onCheckedChange = { on -> onChange { it.copy(enabled = on) } },
                        contentDescription = "Reminders",
                    )
                },
            )
        }
        item { Label("DAYS") }
        item {
            // REM-001, REM-005.
            ChoiceChips(
                options = days.map { it.getDisplayName(TextStyle.NARROW, Locale.getDefault()) },
                descriptions = days.map { it.getDisplayName(TextStyle.FULL, Locale.getDefault()) },
                isSelected = { days[it] in reminders.days },
                onToggle = { index ->
                    val day = days[index]
                    onChange { it.copy(days = if (day in it.days) it.days - day else it.days + day) }
                },
                multiSelect = true,
                modifier = Modifier.padding(horizontal = spacing.lg),
            )
        }
        item { Label("FROM") }
        item {
            // REM-001, REM-003: 30-minute steps; the end follows the start.
            TimeRow(
                label = formatMinuteOfDay(context, reminders.startMinute),
                onEarlier = { onChange { ReminderSchedule.withStart(it, it.startMinute - step) } },
                onLater = { onChange { ReminderSchedule.withStart(it, it.startMinute + step) } },
                earlierEnabled = reminders.startMinute > 0,
                laterEnabled = reminders.startMinute < ReminderSettings.LAST_MINUTE,
                what = "start",
            )
        }
        item { Label("UNTIL") }
        item {
            TimeRow(
                label = formatMinuteOfDay(context, reminders.endMinute),
                onEarlier = { onChange { ReminderSchedule.withEnd(it, it.endMinute - step) } },
                onLater = { onChange { ReminderSchedule.withEnd(it, it.endMinute + step) } },
                earlierEnabled = reminders.endMinute > 0,
                laterEnabled = reminders.endMinute < ReminderSettings.LAST_MINUTE,
                what = "end",
            )
        }
        item { Label("EVERY") }
        item {
            // REM-001.
            val intervals = ReminderSettings.INTERVALS
            ChoiceChips(
                options = intervals.map { "$it min" },
                isSelected = { intervals[it] == reminders.intervalMinutes },
                onToggle = { index -> onChange { it.copy(intervalMinutes = intervals[index]) } },
                modifier = Modifier.padding(horizontal = spacing.lg),
            )
        }
        item {
            AppText(
                text = nextReminderText(context, reminders),
                role = TextRole.Meta,
                modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.xl),
            )
        }
        item {
            AppText(
                text = "Reminders stop for the day once you reach your goal.",
                role = TextRole.Meta,
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    val spacing = AppTheme.spacing
    AppText(
        text = text,
        role = TextRole.Section,
        modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.xl, bottom = spacing.sm),
    )
}

@Composable
private fun TimeRow(
    label: String,
    onEarlier: () -> Unit,
    onLater: () -> Unit,
    earlierEnabled: Boolean,
    laterEnabled: Boolean,
    what: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        ValueStepper(
            label = label,
            onDecrement = onEarlier,
            onIncrement = onLater,
            decrementDescription = "Make the $what time earlier",
            incrementDescription = "Make the $what time later",
            decrementEnabled = earlierEnabled,
            incrementEnabled = laterEnabled,
        )
    }
}

/** UI-008: "Next reminder: today 10:00", or "No reminders scheduled". */
fun nextReminderText(context: Context, reminders: ReminderSettings): String {
    val next = ReminderSchedule.next(LocalDateTime.now(), reminders) ?: return "No reminders scheduled"
    val today = LocalDate.now()
    val day = when (next.toLocalDate()) {
        today -> "today"
        today.plusDays(1) -> "tomorrow"
        else -> next.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    }
    return "Next reminder: $day ${formatTime(context, next.toLocalTime())}"
}

/** The settings screen's one-line summary: "Every 60 min, 08:00–20:00, every day". */
fun reminderSummary(context: Context, reminders: ReminderSettings): String {
    if (!reminders.enabled) return "Off"
    val days = when (reminders.days) {
        DayOfWeek.entries.toSet() -> "every day"
        emptySet<DayOfWeek>() -> "no days selected"
        else -> DayOfWeek.entries.filter { it in reminders.days }
            .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }
    return "Every ${reminders.intervalMinutes} min, " +
        "${formatMinuteOfDay(context, reminders.startMinute)}–${formatMinuteOfDay(context, reminders.endMinute)}, $days"
}

@Preview(showBackground = true)
@Composable
private fun RemindersScreenPreview() {
    AppTheme {
        RemindersScreen(reminders = ReminderSettings.DEFAULT, onChange = {}, onBack = {})
    }
}
