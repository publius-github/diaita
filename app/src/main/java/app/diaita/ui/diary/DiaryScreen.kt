package app.diaita.ui.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.settings.CoachTone
import app.diaita.data.settings.DayPartShares
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.common.FoodRowCard
import app.diaita.ui.common.LogWeightDialog
import app.diaita.ui.common.MacroProgressRing
import app.diaita.ui.common.MealSlotPicker
import app.diaita.ui.common.MicronutrientTotalsLine
import app.diaita.ui.common.QuantitySheet
import app.diaita.ui.common.friendlyDateLabel
import app.diaita.ui.common.label
import app.diaita.ui.common.progressTrackColor
import app.diaita.util.DateUtils
import app.diaita.util.formatWeightForDisplay
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
fun DiaryScreen(
    onAddFoodClick: (date: String) -> Unit,
    onOpenGoals: () -> Unit,
    onOpenWizard: () -> Unit,
    modifier: Modifier = Modifier,
    jumpToToday: Boolean = false,
    onJumpToTodayHandled: () -> Unit = {},
    viewModel: DiaryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editingEntry by viewModel.editingEntry.collectAsStateWithLifecycle()
    val editingPresetRow by viewModel.editingPresetRow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    var showWeightDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCopySourcePicker by remember { mutableStateOf(false) }
    var copySourceDate by remember { mutableStateOf<String?>(null) }
    val undoLabel = stringResource(R.string.undo)

    // The Home tab doubles as "back to today": AppNavHost raises this flag on every tap.
    LaunchedEffect(jumpToToday) {
        if (jumpToToday) {
            viewModel.selectToday()
            onJumpToTodayHandled()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.undoEvent.collect { event ->
            val result =
                snackbarHostState.showSnackbar(
                    message =
                        context.getString(
                            R.string.deleted_named,
                            event.title ?: context.getString(R.string.preset_fallback),
                        ),
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short,
                )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete(event.entries)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.copyResultEvent.collect { copied ->
            if (copied == 0) {
                snackbarHostState.showSnackbar(context.getString(R.string.nothing_to_copy))
            } else {
                snackbarHostState.showSnackbar(
                    context.resources.getQuantityString(R.plurals.copied_entries, copied, copied),
                )
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddFoodClick(uiState.date) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_food))
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        ) {
            DateNavigationRow(
                date = uiState.date,
                isToday = uiState.isToday,
                onPreviousDay = viewModel::selectPreviousDay,
                onNextDay = viewModel::selectNextDay,
                onDateClick = { showDatePicker = true },
                onCopyFromClick = { showCopySourcePicker = true },
            )
            Spacer(modifier = Modifier.height(4.dp))
            DailyTotalsCard(uiState)
            if (uiState.loaded && uiState.goalCalories == null) {
                Spacer(modifier = Modifier.height(8.dp))
                SetGoalBanner(onOpenGoals = onOpenGoals, onOpenWizard = onOpenWizard)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showWeightDialog = true }) {
                    Text(stringResource(R.string.log_weight))
                }
                uiState.todayWeightKg?.let { weight ->
                    Text(
                        stringResource(
                            R.string.weight_today,
                            formatWeightForDisplay(uiState.weightUnit.fromKg(weight)),
                            uiState.weightUnit.label(),
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
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::copyYesterday) {
                        Text(stringResource(R.string.repeat_yesterday))
                    }
                    OutlinedButton(onClick = { showCopySourcePicker = true }) {
                        Text(stringResource(R.string.copy_from_day))
                    }
                }
            } else {
                val showHeaders =
                    uiState.sections.size > 1 ||
                        uiState.sections.firstOrNull()?.slot != MealSlot.UNSPECIFIED
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    // clearance so the FAB never covers the last entry
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    uiState.sections.forEach { section ->
                        if (showHeaders) {
                            item(key = "header_${section.slot}") {
                                Row(
                                    modifier =
                                        Modifier
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
                                    onClick = { viewModel.requestEdit(row) },
                                    onDelete = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.delete(row)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DiaryDatePickerDialog(
            date = uiState.date,
            onDismiss = { showDatePicker = false },
            onConfirm = { picked ->
                showDatePicker = false
                viewModel.selectDate(picked)
            },
        )
    }

    if (showCopySourcePicker) {
        DiaryDatePickerDialog(
            date = uiState.date,
            onDismiss = { showCopySourcePicker = false },
            onConfirm = { picked ->
                showCopySourcePicker = false
                copySourceDate = picked.toString()
            },
        )
    }

    copySourceDate?.let { source ->
        CopyMealChoiceDialog(
            sourceDate = source,
            onDismiss = { copySourceDate = null },
            onConfirm = { slot ->
                copySourceDate = null
                viewModel.copyFrom(source, slot)
            },
        )
    }

    if (showWeightDialog) {
        LogWeightDialog(
            weightUnit = uiState.weightUnit,
            onDismiss = { showWeightDialog = false },
            onConfirm = { weightKg ->
                viewModel.logWeight(weightKg)
                showWeightDialog = false
            },
        )
    }

    editingEntry?.let { entryWithFood ->
        QuantitySheet(
            food = entryWithFood.food,
            initialQuantityGrams = entryWithFood.entry.quantityGrams,
            initialMealSlot = entryWithFood.entry.mealSlot,
            showMealSlotPicker = true,
            confirmLabel = stringResource(R.string.save),
            onDismiss = viewModel::cancelEdit,
            onConfirm = { grams, slot -> viewModel.confirmEdit(grams, slot) },
        )
    }

    editingPresetRow?.let { presetRow ->
        var selectedSlot by remember(presetRow) {
            mutableStateOf(
                presetRow.entries
                    .first()
                    .entry.mealSlot,
            )
        }
        AlertDialog(
            onDismissRequest = viewModel::cancelEdit,
            title = { Text(presetRow.presetName ?: stringResource(R.string.preset_fallback)) },
            text = {
                MealSlotPicker(selected = selectedSlot, onSelected = { selectedSlot = it })
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmPresetSlot(selectedSlot) }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelEdit) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** Nudges a fresh install towards setting a calorie goal — the ring is meaningless without one. */
@Composable
private fun SetGoalBanner(
    onOpenGoals: () -> Unit,
    onOpenWizard: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
        ) {
            Text(
                stringResource(R.string.first_run_goal_body),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onOpenWizard) {
                    Text(stringResource(R.string.first_run_goal_wizard))
                }
                TextButton(onClick = onOpenGoals) {
                    Text(stringResource(R.string.first_run_goal_manual))
                }
            }
        }
    }
}

/**
 * The `< Today >` stepper above the totals card; the right chevron stops at today and the
 * date label itself opens the calendar picker for a direct jump.
 */
@Composable
private fun DateNavigationRow(
    date: String,
    isToday: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateClick: () -> Unit,
    onCopyFromClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.previous_day))
        }
        Text(
            friendlyDateLabel(date),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .weight(1f)
                    .clickable(onClick = onDateClick)
                    .padding(vertical = 8.dp),
        )
        IconButton(onClick = onCopyFromClick) {
            Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.copy_from_day))
        }
        IconButton(onClick = onNextDay, enabled = !isToday) {
            Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.next_day))
        }
    }
}

/**
 * Second step of "copy from a day": whole day, or just one meal of it. Copying always tops
 * the shown day up — nothing gets replaced.
 */
@Composable
private fun CopyMealChoiceDialog(
    sourceDate: String,
    onDismiss: () -> Unit,
    onConfirm: (MealSlot?) -> Unit,
) {
    var selectedSlot by remember { mutableStateOf<MealSlot?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.copy_from_named_day, friendlyDateLabel(sourceDate))) },
        text = {
            Column {
                Text(
                    stringResource(R.string.copy_choose_scope),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                MealSlotPicker(
                    selected = selectedSlot ?: MealSlot.UNSPECIFIED,
                    onSelected = { slot ->
                        selectedSlot = if (slot == selectedSlot) null else slot
                    },
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(
                        if (selectedSlot == null) R.string.copy_scope_whole_day else R.string.copy_scope_one_meal,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedSlot) }) {
                Text(stringResource(R.string.copy_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Calendar dialog for jumping straight to a day; future days can't be picked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryDatePickerDialog(
    date: String,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    // DatePickerState speaks UTC-midnight millis regardless of the device time zone,
    // so both conversions below deliberately go through ZoneOffset.UTC.
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis =
                LocalDate
                    .parse(date)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli(),
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                        !Instant
                            .ofEpochMilli(utcTimeMillis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .isAfter(LocalDate.now())

                    override fun isSelectableYear(year: Int): Boolean = year <= LocalDate.now().year
                },
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = state.selectedDateMillis != null,
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun DailyTotalsCard(uiState: DiaryUiState) {
    val isOverGoal =
        uiState.goalCalories != null &&
            uiState.goalCalories > 0 &&
            uiState.totalCalories > uiState.goalCalories
    val hasMacroGoals =
        listOfNotNull(uiState.goalProteinG, uiState.goalFatG, uiState.goalCarbsG)
            .any { it > 0 }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Weighted so the nutrient lines wrap inside their own space instead of
                // squeezing the ring: with full nutrient names they need two lines on
                // narrow screens, which costs nothing here — the 120dp ring sets the height.
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.kcal_amount, uiState.totalCalories.toInt()),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (isOverGoal) MaterialTheme.colorScheme.error else Color.Unspecified,
                    )
                    // With macro goals set, the gram figures live next to the P/F/C bars
                    // below (consumed/target); the header line only covers the no-goals case
                    // so the numbers never vanish entirely.
                    if (uiState.trackMacros && !hasMacroGoals) {
                        Text(
                            stringResource(
                                R.string.macros_line,
                                uiState.totalProtein.toInt(),
                                uiState.totalFat.toInt(),
                                uiState.totalCarbs.toInt(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (uiState.trackMicronutrients) {
                        MicronutrientTotalsLine(
                            fiberGrams = uiState.totalFiber,
                            sugarGrams = uiState.totalSugar,
                            saltGrams = uiState.totalSalt,
                            hasEntries = !uiState.isEmpty,
                        )
                    }
                }
                MacroProgressRing(
                    consumedCalories = uiState.totalCalories,
                    goalCalories = uiState.goalCalories,
                )
            }
            if (uiState.goalCalories != null && uiState.goalCalories > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                // Ticks once a minute so the budgets shift on their own when a day-part
                // boundary passes while the screen sits open — recomposition otherwise
                // only happens on a data change.
                val nowMinutes by produceState(initialValue = minuteOfDay()) {
                    while (true) {
                        delay(60_000)
                        value = minuteOfDay()
                    }
                }
                // A past day is fully over: every part's window has closed, and there is no
                // "current" part to emphasize or coach about.
                val dayPartBudgets =
                    buildDayPartBudgets(
                        goalCalories = uiState.goalCalories,
                        shares = uiState.dayPartShares,
                        consumed = uiState.dayPartCalories,
                        nowMinutes = if (uiState.isToday) nowMinutes else 24 * 60,
                        boundaries = uiState.dayPartBoundaries,
                    )
                val currentPart =
                    if (uiState.isToday) currentDayPart(nowMinutes, uiState.dayPartBoundaries) else null
                DayPartBalanceBar(
                    dayPartCalories = uiState.dayPartCalories,
                    dayPartBudgets = dayPartBudgets,
                    shares = uiState.dayPartShares,
                    currentPart = currentPart,
                    dayRemaining = uiState.goalCalories - uiState.totalCalories.toInt(),
                )
                val carriedIn = currentPart?.let { dayPartBudgets[it]?.carriedIn } ?: 0
                if (currentPart != null && carriedIn > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.day_part_carry_note, carriedIn, currentPart.label()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (uiState.isToday && uiState.coachTone != CoachTone.OFF) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PaceMessage(
                        uiState = uiState,
                        goalCalories = uiState.goalCalories,
                        dayPartBudgets = dayPartBudgets,
                    )
                }
            }
            if (hasMacroGoals && uiState.trackMacros) {
                Spacer(modifier = Modifier.height(12.dp))
                MacroBar(
                    label = stringResource(R.string.macro_protein_short),
                    consumed = uiState.totalProtein,
                    targetG = uiState.goalProteinG,
                    overTolerancePercent = uiState.macroOverTolerances.proteinPercent,
                )
                Spacer(modifier = Modifier.height(6.dp))
                MacroBar(
                    label = stringResource(R.string.macro_fat_short),
                    consumed = uiState.totalFat,
                    targetG = uiState.goalFatG,
                    overTolerancePercent = uiState.macroOverTolerances.fatPercent,
                )
                Spacer(modifier = Modifier.height(6.dp))
                MacroBar(
                    label = stringResource(R.string.macro_carbs_short),
                    consumed = uiState.totalCarbs,
                    targetG = uiState.goalCarbsG,
                    overTolerancePercent = uiState.macroOverTolerances.carbsPercent,
                )
            }
        }
    }
}

private enum class PaceMood { EMPTY, ON_TRACK, AHEAD, OVER }

/** Message pools per mood for each coach voice; the snarky set is the app's original one. */
private fun paceMessagePool(
    mood: PaceMood,
    tone: CoachTone,
): List<Int> =
    when (tone) {
        CoachTone.GENTLE ->
            when (mood) {
                PaceMood.EMPTY ->
                    listOf(
                        R.string.pace_empty_gentle_1,
                        R.string.pace_empty_gentle_2,
                        R.string.pace_empty_gentle_3,
                        R.string.pace_empty_gentle_4,
                        R.string.pace_empty_gentle_5,
                    )
                PaceMood.ON_TRACK ->
                    listOf(
                        R.string.pace_ok_gentle_1,
                        R.string.pace_ok_gentle_2,
                        R.string.pace_ok_gentle_3,
                        R.string.pace_ok_gentle_4,
                        R.string.pace_ok_gentle_5,
                    )
                PaceMood.AHEAD ->
                    listOf(
                        R.string.pace_warn_gentle_1,
                        R.string.pace_warn_gentle_2,
                        R.string.pace_warn_gentle_3,
                        R.string.pace_warn_gentle_4,
                        R.string.pace_warn_gentle_5,
                    )
                PaceMood.OVER ->
                    listOf(
                        R.string.pace_over_gentle_1,
                        R.string.pace_over_gentle_2,
                        R.string.pace_over_gentle_3,
                        R.string.pace_over_gentle_4,
                        R.string.pace_over_gentle_5,
                    )
            }
        CoachTone.BRUTAL ->
            when (mood) {
                PaceMood.EMPTY ->
                    listOf(
                        R.string.pace_empty_brutal_1,
                        R.string.pace_empty_brutal_2,
                        R.string.pace_empty_brutal_3,
                        R.string.pace_empty_brutal_4,
                        R.string.pace_empty_brutal_5,
                        R.string.pace_empty_brutal_6,
                        R.string.pace_empty_brutal_7,
                    )
                PaceMood.ON_TRACK ->
                    listOf(
                        R.string.pace_ok_brutal_1,
                        R.string.pace_ok_brutal_2,
                        R.string.pace_ok_brutal_3,
                        R.string.pace_ok_brutal_4,
                        R.string.pace_ok_brutal_5,
                        R.string.pace_ok_brutal_6,
                        R.string.pace_ok_brutal_7,
                    )
                PaceMood.AHEAD ->
                    listOf(
                        R.string.pace_warn_brutal_1,
                        R.string.pace_warn_brutal_2,
                        R.string.pace_warn_brutal_3,
                        R.string.pace_warn_brutal_4,
                        R.string.pace_warn_brutal_5,
                        R.string.pace_warn_brutal_6,
                        R.string.pace_warn_brutal_7,
                    )
                PaceMood.OVER ->
                    listOf(
                        R.string.pace_over_brutal_1,
                        R.string.pace_over_brutal_2,
                        R.string.pace_over_brutal_3,
                        R.string.pace_over_brutal_4,
                        R.string.pace_over_brutal_5,
                        R.string.pace_over_brutal_6,
                        R.string.pace_over_brutal_7,
                    )
            }
        else ->
            when (mood) {
                PaceMood.EMPTY ->
                    listOf(
                        R.string.pace_empty_1,
                        R.string.pace_empty_2,
                        R.string.pace_empty_3,
                        R.string.pace_empty_4,
                        R.string.pace_empty_5,
                        R.string.pace_empty_6,
                        R.string.pace_empty_7,
                    )
                PaceMood.ON_TRACK ->
                    listOf(
                        R.string.pace_ok_1,
                        R.string.pace_ok_2,
                        R.string.pace_ok_3,
                        R.string.pace_ok_4,
                        R.string.pace_ok_5,
                        R.string.pace_ok_6,
                        R.string.pace_ok_7,
                    )
                PaceMood.AHEAD ->
                    listOf(
                        R.string.pace_warn_1,
                        R.string.pace_warn_2,
                        R.string.pace_warn_3,
                        R.string.pace_warn_4,
                        R.string.pace_warn_5,
                        R.string.pace_warn_6,
                        R.string.pace_warn_7,
                    )
                PaceMood.OVER ->
                    listOf(
                        R.string.pace_over_1,
                        R.string.pace_over_2,
                        R.string.pace_over_3,
                        R.string.pace_over_4,
                        R.string.pace_over_5,
                        R.string.pace_over_6,
                        R.string.pace_over_7,
                    )
            }
    }

/**
 * A one-liner "coach" reaction to today's pace: encouraging while on plan, wary once a
 * day-part budget is exceeded, and (in the snarky voice) openly rude once the whole daily
 * goal is blown. A random phrase is picked from the mood's pool and stays put until the
 * mood or the configured tone changes. The caller hides it entirely for [CoachTone.OFF].
 */
@Composable
private fun PaceMessage(
    uiState: DiaryUiState,
    goalCalories: Int,
    dayPartBudgets: Map<DayPart, DayPartBudget>,
) {
    val mood =
        when {
            uiState.totalCalories > goalCalories -> PaceMood.OVER
            DayPart.entries.any { part ->
                (uiState.dayPartCalories[part] ?: 0.0) > (dayPartBudgets[part]?.target ?: 0)
            } -> PaceMood.AHEAD
            uiState.isEmpty -> PaceMood.EMPTY
            else -> PaceMood.ON_TRACK
        }
    val messageRes =
        remember(mood, uiState.coachTone) {
            paceMessagePool(mood, uiState.coachTone).random()
        }
    Text(
        stringResource(messageRes),
        style = MaterialTheme.typography.bodySmall,
        fontStyle = FontStyle.Italic,
        color =
            when (mood) {
                PaceMood.OVER -> MaterialTheme.colorScheme.error
                PaceMood.AHEAD -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
    )
}

/**
 * The daily calorie bar split into three segments — morning / day / evening — sized by the
 * user-configurable [DayPartShares] (default 30/40/30). Each segment fills with that part's
 * consumed calories against its sub-target, turning red when exceeded, so after logging
 * e.g. breakfast you immediately see whether you ate more than the guideline suggests for
 * that part of the day. Sub-targets come from [buildDayPartBudgets], so a deficit left in an
 * already-finished part raises the next part's target instead of counting later food as an
 * overshoot the day as a whole doesn't have. The part the clock currently falls into is
 * emphasized and answers the immediate question directly: how much is left to eat now.
 */
@Composable
private fun DayPartBalanceBar(
    dayPartCalories: Map<DayPart, Double>,
    dayPartBudgets: Map<DayPart, DayPartBudget>,
    shares: DayPartShares,
    currentPart: DayPart?,
    dayRemaining: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DayPart.entries.forEach { part ->
            DayPartSegment(
                part = part,
                consumed = dayPartCalories[part] ?: 0.0,
                target = dayPartBudgets[part]?.target ?: 0,
                isCurrent = part == currentPart,
                dayRemaining = dayRemaining,
                modifier = Modifier.weight(shares.shareOf(part)),
            )
        }
    }
}

@Composable
private fun DayPartSegment(
    part: DayPart,
    consumed: Double,
    target: Int,
    isCurrent: Boolean,
    dayRemaining: Int,
    modifier: Modifier = Modifier,
) {
    val isOver = target > 0 && consumed > target
    val progress = if (target > 0) (consumed / target).toFloat().coerceIn(0f, 1f) else 0f
    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { progress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            // Amber, not error red: a single overshot day part is recoverable within the
            // same day, so it warns rather than declares failure (red stays for the day total).
            color = if (isOver) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            trackColor = progressTrackColor(),
        )
        // Label and figures are stacked, not side-by-side: a narrow (30%-width) segment can't
        // fit "Morning" next to two 4-digit numbers, so the label takes the first line and the
        // consumed/target pair gets the full segment width on its own single line.
        Text(
            part.label(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrent) FontWeight.SemiBold else null,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // The current part answers "how much can I still eat now?" directly; finished and
        // upcoming parts (and an exceeded current one) keep the factual consumed/target pair.
        // The promise is capped by what's left of the DAY: a part's local quota survives an
        // earlier part's overshoot by design (see buildDayPartBudgets), but "N left" must
        // never claim there is room to eat when the day total is already blown.
        val effectiveLeft = minOf(target - consumed.toInt(), dayRemaining)
        val figures =
            if (isCurrent && !isOver && effectiveLeft > 0) {
                stringResource(R.string.day_part_left, effectiveLeft)
            } else {
                stringResource(R.string.day_part_kcal, consumed.toInt(), target)
            }
        Text(
            figures,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrent) FontWeight.SemiBold else null,
            maxLines = 1,
            softWrap = false,
            color =
                if (isOver) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

/** Minutes since midnight of the local wall clock, the input [buildDayPartBudgets] keys on. */
private fun minuteOfDay(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

@Composable
private fun DayPart.label(): String =
    stringResource(
        when (this) {
            DayPart.MORNING -> R.string.day_part_morning
            DayPart.DAY -> R.string.day_part_day
            DayPart.EVENING -> R.string.day_part_evening
        },
    )

/**
 * A small per-macro progress bar with three states. Below the target: plain progress,
 * clamped at 100%. Target reached but within the macro's overshoot tolerance: green check —
 * for protein the target is a floor, for fat/carbs a spent budget, both fine. Past the
 * tolerance: the bar and a warning icon flip to the amber tertiary color — the same
 * "overshot but recoverable" language the day-part bars use — never error red: a blown
 * macro is a nudge, not a failure (only the day total earns red).
 */
@Composable
private fun MacroBar(
    label: String,
    consumed: Double,
    targetG: Int?,
    overTolerancePercent: Int,
) {
    if (targetG == null || targetG <= 0) return
    val progress = (consumed / targetG).toFloat().coerceIn(0f, 1f)
    val reached = consumed >= targetG
    val overLimit = consumed > targetG * (1 + overTolerancePercent / 100.0)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(16.dp),
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier =
                Modifier
                    .weight(1f)
                    .height(6.dp),
            color = if (overLimit) MaterialTheme.colorScheme.tertiary else ProgressIndicatorDefaults.linearColor,
            trackColor = progressTrackColor(),
        )
        // Same consumed/target language as the day-part figures, so both scales read alike.
        Text(
            stringResource(R.string.macro_bar_value, consumed.toInt(), targetG),
            style = MaterialTheme.typography.labelSmall,
            color =
                if (overLimit) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(start = 8.dp),
        )
        Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.CenterEnd) {
            when {
                overLimit ->
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = stringResource(R.string.macro_over_limit),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp),
                    )
                reached ->
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

private fun rowKey(row: DiaryRow): Long =
    when (row) {
        is DiaryRow.Food -> row.entryWithFood.entry.id
        is DiaryRow.Preset ->
            row.entries
                .first()
                .entry.id
    }

@Composable
private fun DiaryRowCard(
    row: DiaryRow,
    onClick: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    val title =
        when (row) {
            is DiaryRow.Food -> row.entryWithFood.food.name
            is DiaryRow.Preset -> row.presetName ?: stringResource(R.string.preset_fallback)
        }
    val time = DateUtils.formatTime(row.loggedAt)
    val subtitle =
        when (row) {
            is DiaryRow.Food ->
                stringResource(
                    R.string.diary_food_subtitle,
                    time,
                    row.entryWithFood.entry.quantityGrams
                        .toInt(),
                    row.calories.toInt(),
                )
            is DiaryRow.Preset ->
                stringResource(
                    R.string.diary_preset_subtitle,
                    time,
                    row.entries.size,
                    row.calories.toInt(),
                )
        }
    FoodRowCard(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        onDelete = onDelete,
    )
}
