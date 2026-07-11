package app.diaita.ui.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.data.db.entities.MealSlot
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.addfood.QuantityDialog
import app.diaita.ui.common.LogWeightDialog
import app.diaita.ui.common.MacroProgressRing
import app.diaita.ui.common.label
import app.diaita.util.DateUtils
import java.time.LocalDate
import java.util.Locale

@Composable
fun DiaryScreen(
    onAddFoodClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val confirmDeleteRow by viewModel.confirmDeleteRow.collectAsStateWithLifecycle()
    val editingEntry by viewModel.editingEntry.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    var showWeightDialog by remember { mutableStateOf(false) }
    val deletedMessage = stringResource(R.string.deleted)
    val undoLabel = stringResource(R.string.undo)

    LaunchedEffect(Unit) {
        viewModel.undoEvent.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = deletedMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete(event.entries)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddFoodClick) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_food))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
        ) {
            DailyTotalsCard(uiState)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showWeightDialog = true }) {
                    Text(stringResource(R.string.log_weight))
                }
                uiState.todayWeightKg?.let { weight ->
                    Text(
                        stringResource(
                            R.string.weight_today,
                            String.format(Locale.US, "%.1f", weight),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.isEmpty) {
                Text(
                    stringResource(R.string.diary_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                val showHeaders = uiState.sections.size > 1 ||
                    uiState.sections.firstOrNull()?.slot != MealSlot.UNSPECIFIED
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.sections.forEach { section ->
                        if (showHeaders) {
                            item(key = "header_${section.slot}") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .animateItem(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        section.slot.label(),
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        stringResource(
                                            R.string.kcal_amount,
                                            section.rows.sumOf { it.calories }.toInt(),
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        items(section.rows, key = ::rowKey) { row ->
                            Box(modifier = Modifier.animateItem()) {
                                DiaryRowCard(
                                    row = row,
                                    onClick = if (row is DiaryRow.Food) {
                                        { viewModel.requestEdit(row) }
                                    } else {
                                        null
                                    },
                                    onDelete = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.requestDelete(row)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWeightDialog) {
        LogWeightDialog(
            onDismiss = { showWeightDialog = false },
            onConfirm = { weight ->
                viewModel.logWeight(weight)
                showWeightDialog = false
            },
        )
    }

    confirmDeleteRow?.let { row ->
        val title = when (row) {
            is DiaryRow.Food -> row.entryWithFood.food.name
            is DiaryRow.Preset -> row.presetName
        }
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.delete_confirm_title, title)) },
            text = { Text(stringResource(R.string.delete_confirm_body)) },
            confirmButton = {
                Button(onClick = viewModel::confirmDelete) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    editingEntry?.let { entryWithFood ->
        QuantityDialog(
            food = entryWithFood.food,
            initialQuantityGrams = entryWithFood.entry.quantityGrams,
            initialMealSlot = entryWithFood.entry.mealSlot,
            showMealSlotPicker = true,
            confirmLabel = stringResource(R.string.save),
            onDismiss = viewModel::cancelEdit,
            onConfirm = { grams, slot -> viewModel.confirmEdit(grams, slot) },
        )
    }
}

@Composable
private fun DailyTotalsCard(uiState: DiaryUiState) {
    val isOverGoal = uiState.goalCalories != null &&
        uiState.goalCalories > 0 &&
        uiState.totalCalories > uiState.goalCalories

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(stringResource(R.string.today_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.kcal_amount, uiState.totalCalories.toInt()),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (isOverGoal) MaterialTheme.colorScheme.error else Color.Unspecified,
                    )
                    Text(
                        stringResource(
                            R.string.macros_line,
                            uiState.totalProtein.toInt(),
                            uiState.totalFat.toInt(),
                            uiState.totalCarbs.toInt(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                MacroProgressRing(
                    consumedCalories = uiState.totalCalories,
                    goalCalories = uiState.goalCalories,
                )
            }
            if (uiState.goalCalories != null && uiState.goalCalories > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                DayPartBalanceBar(
                    dayPartCalories = uiState.dayPartCalories,
                    goalCalories = uiState.goalCalories,
                )
                Spacer(modifier = Modifier.height(8.dp))
                PaceMessage(uiState = uiState, goalCalories = uiState.goalCalories)
            }
            val hasMacroGoals = listOfNotNull(uiState.goalProteinG, uiState.goalFatG, uiState.goalCarbsG)
                .any { it > 0 }
            if (hasMacroGoals) {
                Spacer(modifier = Modifier.height(12.dp))
                MacroBar(
                    label = stringResource(R.string.macro_protein_short),
                    consumed = uiState.totalProtein,
                    targetG = uiState.goalProteinG,
                )
                Spacer(modifier = Modifier.height(6.dp))
                MacroBar(
                    label = stringResource(R.string.macro_fat_short),
                    consumed = uiState.totalFat,
                    targetG = uiState.goalFatG,
                )
                Spacer(modifier = Modifier.height(6.dp))
                MacroBar(
                    label = stringResource(R.string.macro_carbs_short),
                    consumed = uiState.totalCarbs,
                    targetG = uiState.goalCarbsG,
                )
            }
        }
    }
}

private enum class PaceMood { EMPTY, ON_TRACK, AHEAD, OVER }

/**
 * A one-liner "coach" reaction to today's pace: encouraging while on plan, wary once a
 * day-part budget is exceeded, and openly rude once the whole daily goal is blown.
 * Within each mood the phrase rotates by day of year so it doesn't get stale.
 */
@Composable
private fun PaceMessage(uiState: DiaryUiState, goalCalories: Int) {
    val mood = when {
        uiState.totalCalories > goalCalories -> PaceMood.OVER
        DayPart.entries.any { part ->
            (uiState.dayPartCalories[part] ?: 0.0) > goalCalories * part.share
        } -> PaceMood.AHEAD
        uiState.isEmpty -> PaceMood.EMPTY
        else -> PaceMood.ON_TRACK
    }
    val variants = when (mood) {
        PaceMood.EMPTY -> listOf(R.string.pace_empty)
        PaceMood.ON_TRACK -> listOf(R.string.pace_ok_1, R.string.pace_ok_2, R.string.pace_ok_3)
        PaceMood.AHEAD -> listOf(R.string.pace_warn_1, R.string.pace_warn_2, R.string.pace_warn_3)
        PaceMood.OVER -> listOf(R.string.pace_over_1, R.string.pace_over_2, R.string.pace_over_3)
    }
    val dayOfYear = remember { LocalDate.now().dayOfYear }
    Text(
        stringResource(variants[dayOfYear % variants.size]),
        style = MaterialTheme.typography.bodySmall,
        fontStyle = FontStyle.Italic,
        color = when (mood) {
            PaceMood.OVER -> MaterialTheme.colorScheme.error
            PaceMood.AHEAD -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

/**
 * The daily calorie bar split into three segments — morning / day / evening — sized by the
 * healthy-eating reference shares from [DayPart.share] (30/40/30). Each segment fills with
 * that part's consumed calories against its own sub-target, turning red when exceeded, so
 * after logging e.g. breakfast you immediately see whether you ate more than the guideline
 * suggests for that part of the day.
 */
@Composable
private fun DayPartBalanceBar(dayPartCalories: Map<DayPart, Double>, goalCalories: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DayPart.entries.forEach { part ->
            DayPartSegment(
                part = part,
                consumed = dayPartCalories[part] ?: 0.0,
                target = (goalCalories * part.share).toInt(),
                modifier = Modifier.weight(part.share),
            )
        }
    }
}

@Composable
private fun DayPartSegment(part: DayPart, consumed: Double, target: Int, modifier: Modifier = Modifier) {
    val isOver = target > 0 && consumed > target
    val progress = if (target > 0) (consumed / target).toFloat().coerceIn(0f, 1f) else 0f
    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(part.label(), style = MaterialTheme.typography.labelSmall)
            Text(
                stringResource(R.string.day_part_kcal, consumed.toInt(), target),
                style = MaterialTheme.typography.labelSmall,
                color = if (isOver) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun DayPart.label(): String = stringResource(
    when (this) {
        DayPart.MORNING -> R.string.day_part_morning
        DayPart.DAY -> R.string.day_part_day
        DayPart.EVENING -> R.string.day_part_evening
    },
)

/**
 * A small per-macro progress bar. Progress is clamped at 100% and the color never changes on
 * exceeding the target (per user preference) — a green check circle marks a reached goal instead.
 */
@Composable
private fun MacroBar(label: String, consumed: Double, targetG: Int?) {
    if (targetG == null || targetG <= 0) return
    val progress = (consumed / targetG).toFloat().coerceIn(0f, 1f)
    val reached = consumed >= targetG

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(16.dp),
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
        )
        Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.CenterEnd) {
            if (reached) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.macro_goal_reached),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun rowKey(row: DiaryRow): Long = when (row) {
    is DiaryRow.Food -> row.entryWithFood.entry.id
    is DiaryRow.Preset -> row.entries.first().entry.id
}

@Composable
private fun DiaryRowCard(row: DiaryRow, onClick: (() -> Unit)?, onDelete: () -> Unit) {
    val title = when (row) {
        is DiaryRow.Food -> row.entryWithFood.food.name
        is DiaryRow.Preset -> row.presetName
    }
    val time = DateUtils.formatTime(row.loggedAt)
    val subtitle = when (row) {
        is DiaryRow.Food -> stringResource(
            R.string.diary_food_subtitle,
            time,
            row.entryWithFood.entry.quantityGrams.toInt(),
            row.calories.toInt(),
        )
        is DiaryRow.Preset -> stringResource(
            R.string.diary_preset_subtitle,
            time,
            row.entries.size,
            row.calories.toInt(),
        )
    }
    val cardModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }

    Card(modifier = cardModifier) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}
