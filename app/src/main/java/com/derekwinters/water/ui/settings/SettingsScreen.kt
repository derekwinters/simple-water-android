package com.derekwinters.water.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.derekwinters.water.WaterApp
import com.derekwinters.water.core.ActivityLevel
import com.derekwinters.water.core.CupTextMode
import com.derekwinters.water.core.DrinkSizes
import com.derekwinters.water.core.GoalMath
import com.derekwinters.water.core.Units
import com.derekwinters.water.core.VolumeUnit
import com.derekwinters.water.core.WeightUnit
import com.derekwinters.water.data.UserSettings
import com.derekwinters.water.designsystem.AlertDialog
import com.derekwinters.water.designsystem.AppText
import com.derekwinters.water.designsystem.AppTheme
import com.derekwinters.water.designsystem.ChoiceChips
import com.derekwinters.water.designsystem.ListLayout
import com.derekwinters.water.designsystem.NumberField
import com.derekwinters.water.designsystem.StockListItem
import com.derekwinters.water.designsystem.TextRole
import com.derekwinters.water.designsystem.Toggle
import com.derekwinters.water.reminders.Notifications
import com.derekwinters.water.reminders.ReminderScheduler
import com.derekwinters.water.ui.reminders.reminderSummary
import kotlinx.coroutines.launch

/** Which editor dialog is open (UI-007). */
private enum class Editor { None, Weight, Activity, Goal, Sizes }

@Composable
fun SettingsRoute(
    app: WaterApp,
    settings: State<UserSettings>,
    onBack: () -> Unit,
    onOpenReminders: () -> Unit,
) {
    val context = LocalContext.current
    // REM-014, REM-015: permission state is re-read whenever the screen resumes.
    var resumeCount by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeCount++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationsAllowed = remember(resumeCount) { Notifications.canPost(context) }
    val exactAlarmsAllowed = remember(resumeCount) { ReminderScheduler.canScheduleExact(context) }

    SettingsScreen(
        settings = settings.value,
        notificationsAllowed = notificationsAllowed,
        showExactAlarmRow = Build.VERSION.SDK_INT in Build.VERSION_CODES.S..Build.VERSION_CODES.S_V2 && !exactAlarmsAllowed,
        onUpdate = { transform -> app.appScope.launch { app.repository.updateSettings(transform) } },
        onOpenReminders = onOpenReminders,
        onOpenNotificationSettings = { openNotificationSettings(context) },
        onOpenExactAlarmSettings = { openExactAlarmSettings(context) },
        onBack = onBack,
    )
}

/** UI-006. */
@Composable
fun SettingsScreen(
    settings: UserSettings,
    notificationsAllowed: Boolean,
    showExactAlarmRow: Boolean,
    onUpdate: ((UserSettings) -> UserSettings) -> Unit,
    onOpenReminders: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unit = settings.volumeUnit
    var editor by rememberSaveable { mutableStateOf(Editor.None) }
    val rowModifier = Modifier.fillMaxWidth()

    ListLayout(title = "Settings", modifier = modifier, onBack = onBack) {
        item { SectionLabel("GOAL") }
        item {
            StockListItem(
                headline = "Weight",
                supportingText = Units.formatWeight(settings.profile.weightKg, settings.profile.weightUnit),
                modifier = rowModifier.clickable { editor = Editor.Weight },
            )
        }
        item {
            StockListItem(
                headline = "Activity level",
                supportingText = settings.profile.activity.label,
                modifier = rowModifier.clickable { editor = Editor.Activity },
            )
        }
        item {
            val recommended = Units.formatVolume(settings.recommendedGoalMl, unit)
            StockListItem(
                headline = "Daily goal",
                supportingText = if (settings.customGoalMl == null) {
                    "$recommended (recommended)"
                } else {
                    "${Units.formatVolume(settings.goalMl, unit)} (custom; recommended $recommended)"
                },
                modifier = rowModifier.clickable { editor = Editor.Goal },
            )
        }
        if (settings.customGoalMl != null) {
            item {
                // GOAL-007.
                StockListItem(
                    headline = "Use recommended goal",
                    supportingText = Units.formatVolume(settings.recommendedGoalMl, unit),
                    modifier = rowModifier.clickable { onUpdate { it.copy(customGoalMl = null) } },
                )
            }
        }

        item { SectionLabel("UNITS") }
        item {
            // GOAL-008.
            StockListItem(headline = "Volume", modifier = rowModifier)
        }
        item {
            ChoiceChips(
                options = VolumeUnit.entries.map { it.label },
                isSelected = { VolumeUnit.entries[it] == unit },
                onToggle = { index -> onUpdate { it.copy(volumeUnit = VolumeUnit.entries[index]) } },
                modifier = Modifier.padding(horizontal = AppTheme.spacing.lg),
            )
        }

        item { SectionLabel("DRINKS") }
        item {
            // LOG-009.
            StockListItem(
                headline = "Quick-add sizes",
                supportingText = settings.quickAddSizesMl.joinToString(", ") { Units.formatVolume(it, unit) },
                modifier = rowModifier.clickable { editor = Editor.Sizes },
            )
        }

        item { SectionLabel("REMINDERS") }
        item {
            StockListItem(
                headline = "Reminders",
                supportingText = reminderSummary(LocalContext.current, settings.reminders),
                modifier = rowModifier.clickable(onClick = onOpenReminders),
                trailingContent = {
                    Toggle(
                        checked = settings.reminders.enabled,
                        onCheckedChange = { on -> onUpdate { it.copy(reminders = it.reminders.copy(enabled = on)) } },
                        contentDescription = "Reminders",
                    )
                },
            )
        }

        item { SectionLabel("WIDGET") }
        item {
            // UI-021, UI-023.
            StockListItem(headline = "Text in cup", modifier = rowModifier)
        }
        item {
            ChoiceChips(
                options = CupTextMode.entries.map { it.label },
                isSelected = { CupTextMode.entries[it] == settings.widget.text },
                onToggle = { index -> onUpdate { it.copy(widget = it.widget.copy(text = CupTextMode.entries[index])) } },
                modifier = Modifier.padding(horizontal = AppTheme.spacing.lg),
            )
        }
        item {
            // UI-020, UI-023.
            StockListItem(
                headline = "Ideal line",
                modifier = rowModifier,
                trailingContent = {
                    Toggle(
                        checked = settings.widget.idealLine,
                        onCheckedChange = { on -> onUpdate { it.copy(widget = it.widget.copy(idealLine = on)) } },
                        contentDescription = "Ideal line",
                    )
                },
            )
        }

        item { SectionLabel("PERMISSIONS") }
        item {
            // REM-014.
            StockListItem(
                headline = "Notifications",
                supportingText = if (notificationsAllowed) "Allowed" else "Blocked — tap to allow reminders",
                modifier = rowModifier.clickable(onClick = onOpenNotificationSettings),
            )
        }
        if (showExactAlarmRow) {
            item {
                // REM-015.
                StockListItem(
                    headline = "Alarms & reminders",
                    supportingText = "Not allowed — reminders may arrive a few minutes late. Tap to allow.",
                    modifier = rowModifier.clickable(onClick = onOpenExactAlarmSettings),
                )
            }
        }
    }

    when (editor) {
        Editor.None -> Unit
        Editor.Weight -> WeightDialog(
            settings = settings,
            onSave = { kg, weightUnit ->
                editor = Editor.None
                onUpdate { it.copy(profile = it.profile.copy(weightKg = kg, weightUnit = weightUnit)) }
            },
            onDismiss = { editor = Editor.None },
        )
        Editor.Activity -> ActivityDialog(
            current = settings.profile.activity,
            onSave = { level ->
                editor = Editor.None
                onUpdate { it.copy(profile = it.profile.copy(activity = level)) }
            },
            onDismiss = { editor = Editor.None },
        )
        Editor.Goal -> GoalDialog(
            settings = settings,
            onSave = { ml ->
                editor = Editor.None
                onUpdate { it.copy(customGoalMl = ml) }
            },
            onDismiss = { editor = Editor.None },
        )
        Editor.Sizes -> SizesDialog(
            settings = settings,
            onSave = { sizes ->
                editor = Editor.None
                onUpdate { it.copy(drinkSizes = sizes) }
            },
            onDismiss = { editor = Editor.None },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    val spacing = AppTheme.spacing
    AppText(
        text = text,
        role = TextRole.Section,
        modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.xl, bottom = spacing.xs),
    )
}

/** GOAL-001, UI-007. */
@Composable
private fun WeightDialog(settings: UserSettings, onSave: (Double, WeightUnit) -> Unit, onDismiss: () -> Unit) {
    val profile = settings.profile
    var weightUnit by rememberSaveable { mutableStateOf(profile.weightUnit) }
    var text by rememberSaveable { mutableStateOf(Units.kgToDisplay(profile.weightKg, profile.weightUnit).toString()) }
    val value = text.toIntOrNull()
    val valid = value != null && GoalMath.isValidWeight(value, weightUnit)
    AlertDialog(
        title = "Weight",
        confirmText = "Save",
        onConfirm = { if (value != null && valid) onSave(Units.displayToKg(value, weightUnit), weightUnit) },
        onDismissRequest = onDismiss,
        cancelText = "Cancel",
        confirmEnabled = valid,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                NumberField(
                    value = text,
                    onValueChange = { text = it },
                    contentDescription = "Weight in ${weightUnit.label}",
                    suffix = weightUnit.label,
                )
                ChoiceChips(
                    options = WeightUnit.entries.map { it.label },
                    isSelected = { WeightUnit.entries[it] == weightUnit },
                    onToggle = { index ->
                        val chosen = WeightUnit.entries[index]
                        if (chosen != weightUnit) {
                            text.toIntOrNull()?.let { v ->
                                text = Units.kgToDisplay(Units.displayToKg(v, weightUnit), chosen).toString()
                            }
                            weightUnit = chosen
                        }
                    },
                )
                if (!valid) {
                    val range = if (weightUnit == WeightUnit.KG) GoalMath.WEIGHT_RANGE_KG else GoalMath.WEIGHT_RANGE_LB
                    AppText(text = "${range.first}–${range.last} ${weightUnit.label}", role = TextRole.Meta)
                }
            }
        },
    )
}

/** GOAL-002, UI-007. */
@Composable
private fun ActivityDialog(current: ActivityLevel, onSave: (ActivityLevel) -> Unit, onDismiss: () -> Unit) {
    var level by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        title = "Activity level",
        confirmText = "Save",
        onConfirm = { onSave(level) },
        onDismissRequest = onDismiss,
        cancelText = "Cancel",
        content = {
            ChoiceChips(
                options = ActivityLevel.entries.map { it.label },
                isSelected = { ActivityLevel.entries[it] == level },
                onToggle = { level = ActivityLevel.entries[it] },
                columns = 2,
            )
        },
    )
}

/** GOAL-006, UI-007. */
@Composable
private fun GoalDialog(settings: UserSettings, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    val unit = settings.volumeUnit
    var text by rememberSaveable { mutableStateOf(Units.mlToDisplay(settings.goalMl, unit).toString()) }
    val ml = GoalMath.customGoalFromInput(text, unit)
    AlertDialog(
        title = "Daily goal",
        confirmText = "Save",
        onConfirm = { ml?.let(onSave) },
        onDismissRequest = onDismiss,
        cancelText = "Cancel",
        confirmEnabled = ml != null,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                NumberField(
                    value = text,
                    onValueChange = { text = it },
                    contentDescription = "Daily goal in ${unit.label}",
                    suffix = unit.label,
                )
                AppText(
                    text = "Recommended: ${Units.formatVolume(settings.recommendedGoalMl, unit)}. " +
                        "Allowed: ${Units.formatVolume(GoalMath.MIN_GOAL_ML, unit)}–${Units.formatVolume(GoalMath.MAX_GOAL_ML, unit)}.",
                    role = TextRole.Meta,
                )
            }
        },
    )
}

/** LOG-004, LOG-009, UI-007: up to four sizes; blanks are dropped. */
@Composable
private fun SizesDialog(settings: UserSettings, onSave: (List<Double>) -> Unit, onDismiss: () -> Unit) {
    val unit = settings.volumeUnit
    val initial = List(DrinkSizes.MAX_COUNT) { index ->
        DrinkSizes.forUnit(settings.drinkSizes, unit).getOrNull(index)?.let { Units.mlToDisplay(it, unit).toString() } ?: ""
    }
    var first by rememberSaveable { mutableStateOf(initial[0]) }
    var second by rememberSaveable { mutableStateOf(initial[1]) }
    var third by rememberSaveable { mutableStateOf(initial[2]) }
    var fourth by rememberSaveable { mutableStateOf(initial[3]) }
    val sizes = DrinkSizes.fromInputs(listOf(first, second, third, fourth), unit)
    AlertDialog(
        title = "Quick-add sizes",
        confirmText = "Save",
        onConfirm = { sizes?.let(onSave) },
        onDismissRequest = onDismiss,
        cancelText = "Cancel",
        confirmEnabled = sizes != null,
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                horizontalAlignment = Alignment.Start,
            ) {
                AppText(text = "The first size is the default, used by the reminder's Log button.", role = TextRole.Meta)
                NumberField(value = first, onValueChange = { first = it }, contentDescription = "Size 1 (default)", suffix = unit.label)
                NumberField(value = second, onValueChange = { second = it }, contentDescription = "Size 2", suffix = unit.label)
                NumberField(value = third, onValueChange = { third = it }, contentDescription = "Size 3", suffix = unit.label)
                NumberField(value = fourth, onValueChange = { fourth = it }, contentDescription = "Size 4", suffix = unit.label)
                AppText(
                    text = "Each ${Units.formatVolume(DrinkSizes.MIN_ML, unit)}–${Units.formatVolume(DrinkSizes.MAX_ML, unit)}; leave blank to remove.",
                    role = TextRole.Meta,
                )
            }
        },
    )
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen(
            settings = UserSettings(onboarded = true, customGoalMl = 3000),
            notificationsAllowed = false,
            showExactAlarmRow = true,
            onUpdate = {},
            onOpenReminders = {},
            onOpenNotificationSettings = {},
            onOpenExactAlarmSettings = {},
            onBack = {},
        )
    }
}
