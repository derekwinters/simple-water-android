package com.derekwinters.water.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.derekwinters.water.WaterApp
import com.derekwinters.water.core.EntryRules
import com.derekwinters.water.core.GoalMath
import com.derekwinters.water.core.History
import com.derekwinters.water.core.Units
import com.derekwinters.water.core.VolumeUnit
import com.derekwinters.water.core.WeekSummary
import com.derekwinters.water.data.DrinkEntry
import com.derekwinters.water.data.UserSettings
import com.derekwinters.water.designsystem.AlertDialog
import com.derekwinters.water.designsystem.AppText
import com.derekwinters.water.designsystem.AppTheme
import com.derekwinters.water.designsystem.ListLayout
import com.derekwinters.water.designsystem.NumberField
import com.derekwinters.water.designsystem.NumericText
import com.derekwinters.water.designsystem.PrimaryButton
import com.derekwinters.water.designsystem.ProgressRing
import com.derekwinters.water.designsystem.ScreenHeaderAction
import com.derekwinters.water.designsystem.SecondaryButton
import com.derekwinters.water.designsystem.StatusIconButton
import com.derekwinters.water.designsystem.TextRole
import com.derekwinters.water.designsystem.UndoBar
import com.derekwinters.water.ui.formatTime
import com.derekwinters.water.ui.rememberToday
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** LOG-008: how long the undo bar stays. */
private const val UNDO_MILLIS = 5_000L

@Composable
fun HomeRoute(
    app: WaterApp,
    settings: State<UserSettings>,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val current = settings.value
    val context = LocalContext.current
    val today by rememberToday()
    val zone = remember(today) { ZoneId.systemDefault() }
    val repository = app.repository

    val entries by remember(today, zone) { repository.entriesOn(today, zone) }
        .collectAsState(initial = emptyList())
    val weekEntries by remember(today, zone) { repository.entriesBetween(today.minusDays(6), today, zone) }
        .collectAsState(initial = emptyList())
    val records by repository.goalRecords.collectAsState(initial = emptyList())
    val week = History.week(
        endDate = today,
        today = today,
        totals = History.totalsByDay(weekEntries.map { it.toLoggedAmount() }, zone),
        records = records,
        currentGoalMl = current.goalMl,
    )

    // LOG-008: the most recently deleted entry, while its undo bar shows.
    var removed by remember { mutableStateOf<DrinkEntry?>(null) }
    LaunchedEffect(removed) {
        if (removed != null) {
            delay(UNDO_MILLIS)
            removed = null
        }
    }

    // REM-014: ask for the notification permission once the home screen first shows.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var askedForPermission by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!askedForPermission &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            askedForPermission = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    HomeScreen(
        settings = current,
        entries = entries,
        week = week,
        removed = removed,
        timeOf = { formatTime(context, it.loggedAt, zone) },
        onAdd = { amount -> app.appScope.launch { repository.addEntry(amount) } },
        onDelete = { entry ->
            removed = entry
            app.appScope.launch { repository.deleteEntry(entry) }
        },
        onUndo = {
            val entry = removed
            removed = null
            if (entry != null) app.appScope.launch { repository.restoreEntry(entry) }
        },
        onOpenSettings = onOpenSettings,
        onOpenHistory = onOpenHistory,
    )
}

/** UI-003: the home screen. */
@Composable
fun HomeScreen(
    settings: UserSettings,
    entries: List<DrinkEntry>,
    week: WeekSummary,
    removed: DrinkEntry?,
    timeOf: (DrinkEntry) -> String,
    onAdd: (Int) -> Unit,
    onDelete: (DrinkEntry) -> Unit,
    onUndo: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val unit = settings.volumeUnit
    val total = entries.sumOf { it.amountMl }
    var customOpen by rememberSaveable { mutableStateOf(false) }

    ListLayout(
        title = "Today",
        modifier = modifier,
        trailingAction = ScreenHeaderAction.Icon(
            icon = Icons.Filled.Settings,
            contentDescription = "Settings",
            onClick = onOpenSettings,
        ),
        bottomSlot = if (removed == null) {
            null
        } else {
            {
                UndoBar(
                    message = "Removed ${Units.formatVolume(removed.amountMl, unit)}",
                    actionLabel = "Undo",
                    onAction = onUndo,
                )
            }
        },
    ) {
        item {
            ProgressCard(totalMl = total, goalMl = settings.goalMl, unit = unit)
        }
        item {
            QuickAddRow(
                sizes = settings.drinkSizes,
                unit = unit,
                onAdd = onAdd,
                onCustom = { customOpen = true },
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenHistory)
                    .padding(horizontal = spacing.lg, vertical = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(text = "This week", role = TextRole.BodyStrong, modifier = Modifier.weight(1f))
                AppText(text = "${week.metCount} of ${week.days.size} days met ›", role = TextRole.Meta)
            }
        }
        item {
            AppText(
                text = "TODAY'S DRINKS",
                role = TextRole.Section,
                modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.lg, bottom = spacing.sm),
            )
        }
        if (entries.isEmpty()) {
            item {
                AppText(
                    text = "Nothing logged yet today.",
                    role = TextRole.Meta,
                    modifier = Modifier.padding(horizontal = spacing.lg),
                )
            }
        }
        items(entries, key = { it.id }) { entry ->
            EntryRow(
                time = timeOf(entry),
                amount = Units.formatVolume(entry.amountMl, unit),
                onDelete = { onDelete(entry) },
            )
        }
        // Room for the undo bar so it never covers the last entry.
        item {
            Column(modifier = Modifier.padding(bottom = spacing.xxl + spacing.xxl + spacing.xxl)) {}
        }
    }

    if (customOpen) {
        CustomAmountDialog(
            unit = unit,
            onAdd = { amount ->
                customOpen = false
                onAdd(amount)
            },
            onDismiss = { customOpen = false },
        )
    }
}

/** LOG-003, UI-004. */
@Composable
private fun ProgressCard(totalMl: Int, goalMl: Int, unit: VolumeUnit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val remaining = GoalMath.remainingMl(totalMl, goalMl)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm)
            .clip(RoundedCornerShape(spacing.md))
            .background(colors.chip)
            .padding(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        ProgressRing(fraction = GoalMath.fraction(totalMl, goalMl)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NumericText(text = Units.formatNumber(totalMl, unit), style = AppTheme.numericTypography.large)
                AppText(text = "of ${Units.formatVolume(goalMl, unit)}", role = TextRole.Meta)
            }
        }
        if (remaining == 0) {
            AppText(text = "Goal met", role = TextRole.BodyStrong, color = colors.work)
        } else {
            AppText(text = "${Units.formatVolume(remaining, unit)} to go", role = TextRole.Body)
        }
    }
}

/** LOG-004, LOG-005: the default size is the screen's single primary action (DS-001). */
@Composable
private fun QuickAddRow(sizes: List<Int>, unit: VolumeUnit, onAdd: (Int) -> Unit, onCustom: () -> Unit) {
    val spacing = AppTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        val default = sizes.first()
        PrimaryButton(
            text = "+ ${Units.formatVolume(default, unit)}",
            onClick = { onAdd(default) },
            modifier = Modifier.fillMaxWidth(),
        )
        val others = sizes.drop(1).map { size -> "+ ${Units.formatVolume(size, unit)}" to { onAdd(size) } } +
            ("Custom…" to onCustom)
        others.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                row.forEach { (label, action) ->
                    SecondaryButton(text = label, onClick = action, modifier = Modifier.weight(1f))
                }
                repeat(3 - row.size) { Column(modifier = Modifier.weight(1f)) {} }
            }
        }
    }
}

/** LOG-007. */
@Composable
private fun EntryRow(time: String, amount: String, onDelete: () -> Unit) {
    val spacing = AppTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(text = time, role = TextRole.Meta, modifier = Modifier.weight(1f))
        NumericText(text = amount, style = AppTheme.numericTypography.inline)
        StatusIconButton(
            icon = Icons.Filled.Delete,
            contentDescription = "Delete $amount at $time",
            onClick = onDelete,
            modifier = Modifier.padding(start = spacing.sm),
        )
    }
}

/** LOG-006. */
@Composable
private fun CustomAmountDialog(unit: VolumeUnit, onAdd: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val amount = EntryRules.amountFromInput(text, unit)
    AlertDialog(
        title = "Custom amount",
        confirmText = "Add",
        onConfirm = { amount?.let(onAdd) },
        onDismissRequest = onDismiss,
        cancelText = "Cancel",
        confirmEnabled = amount != null,
        content = {
            NumberField(
                value = text,
                onValueChange = { text = it },
                contentDescription = "Amount in ${unit.label}",
                suffix = unit.label,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val entries = listOf(
        DrinkEntry(id = 2, amountMl = 500, loggedAt = 0L),
        DrinkEntry(id = 1, amountMl = 250, loggedAt = 0L),
    )
    AppTheme {
        HomeScreen(
            settings = UserSettings(onboarded = true),
            entries = entries,
            week = WeekSummary(emptyList()),
            removed = null,
            timeOf = { "9:30" },
            onAdd = {},
            onDelete = {},
            onUndo = {},
            onOpenSettings = {},
            onOpenHistory = {},
        )
    }
}
