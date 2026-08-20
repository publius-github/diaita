package app.diaita.ui.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.pluralStringResource
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
import app.diaita.data.db.entities.WaterEntry
import app.diaita.data.settings.CoachTone
import app.diaita.data.settings.DayPartOverThresholds
import app.diaita.data.settings.DayPartShares
import app.diaita.data.settings.HomeProgressStyle
import app.diaita.data.settings.SettingsRepository
import app.diaita.data.settings.WaterOverThresholds
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.common.CalorieOverBand
import app.diaita.ui.common.CalorieProgressBar
import app.diaita.ui.common.CompactProgressRing
import app.diaita.ui.common.CompactRowCard
import app.diaita.ui.common.FoodRowCard
import app.diaita.ui.common.LogWeightDialog
import app.diaita.ui.common.MacroProgressRing
import app.diaita.ui.common.MealSlotPicker
import app.diaita.ui.common.MicronutrientTotalsLine
import app.diaita.ui.common.ProgressGauge
import app.diaita.ui.common.QuantitySheet
import app.diaita.ui.common.calorieOverBand
import app.diaita.ui.common.friendlyDateLabel
import app.diaita.ui.common.label
import app.diaita.ui.common.progressTrackColor
import app.diaita.ui.theme.ItemGap
import app.diaita.ui.theme.LabelGap
import app.diaita.ui.theme.ScreenPadding
import app.diaita.ui.theme.SectionGap
import app.diaita.util.BmiBand
import app.diaita.util.DateUtils
import app.diaita.util.WaterBand
import app.diaita.util.bmi
import app.diaita.util.bmiBand
import app.diaita.util.formatWeightForDisplay
import app.diaita.util.waterBand
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
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
    var showWaterEntries by remember { mutableStateOf(false) }
    var editingWaterEntry by remember { mutableStateOf<WaterEntry?>(null) }
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
        // Rapid taps must not queue a snackbar each: collectLatest cancels the previous
        // showSnackbar (which removes it), and the burst list keeps every tap so the one
        // visible snackbar shows the accumulated amount and Undo removes the whole burst.
        val burst = mutableListOf<WaterEntry>()
        viewModel.waterAddedEvent.collectLatest { entry ->
            burst += entry
            val result =
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.water_added, burst.sumOf { it.amountMl }),
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short,
                )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoWater(burst.toList())
            }
            burst.clear()
        }
    }

    // The taps dialog only exists while there are taps; deleting the last one (or switching
    // to a day without any) closes it and any per-tap edit instead of leaving a stale dialog.
    LaunchedEffect(uiState.waterEntries.isEmpty()) {
        if (uiState.waterEntries.isEmpty()) {
            showWaterEntries = false
            editingWaterEntry = null
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
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = ScreenPadding),
        ) {
            // No top inset: the day switcher opens the screen right under the status bar, the
            // way the Presets tabs do.
            DateNavigationRow(
                date = uiState.date,
                isToday = uiState.isToday,
                onPreviousDay = viewModel::selectPreviousDay,
                onNextDay = viewModel::selectNextDay,
                onDateClick = { showDatePicker = true },
                onCopyFromClick = { showCopySourcePicker = true },
            )
            Spacer(modifier = Modifier.height(ItemGap))
            DailyTotalsCard(uiState)
            if (uiState.loaded && uiState.goalCalories == null) {
                Spacer(modifier = Modifier.height(ItemGap))
                SetGoalBanner(onOpenGoals = onOpenGoals, onOpenWizard = onOpenWizard)
            }
            // One rhythm down the top of the screen: the gap under the day switcher, around
            // the card and around the action row are all the same.
            Spacer(modifier = Modifier.height(ItemGap))
            // FlowRow, not Row: three labeled buttons don't fit one line on narrow screens
            // in Russian — overflow must wrap, not clip. Justified like a paragraph of text
            // (SpaceBetween): every button keeps exactly the width its label needs and the
            // leftover goes into the gaps, so the group is flush with both screen edges.
            // Equal-width buttons were tried and reverted — a third of the row is not enough
            // for "Log weight", which then broke onto a second line and doubled the row.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(ItemGap),
            ) {
                OutlinedButton(onClick = { showWeightDialog = true }) {
                    Text(stringResource(R.string.log_weight))
                }
                if (uiState.trackWater) {
                    OutlinedButton(
                        onClick = viewModel::logWater,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WaterBlue),
                    ) {
                        Text(stringResource(R.string.log_water))
                    }
                }
                // Replaces the FAB, which floated over the delete icons of the last list rows.
                // Filled green (primary) and labeled like its two outlined neighbors.
                Button(onClick = { onAddFoodClick(uiState.date) }) {
                    Text(stringResource(R.string.log_food))
                }
            }
            Spacer(modifier = Modifier.height(ItemGap))
            if (uiState.isEmpty) {
                Text(
                    stringResource(R.string.diary_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(SectionGap))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(ItemGap),
                ) {
                    OutlinedButton(onClick = viewModel::copyYesterday) {
                        Text(stringResource(R.string.repeat_yesterday))
                    }
                    OutlinedButton(onClick = { showCopySourcePicker = true }) {
                        Text(stringResource(R.string.copy_from_day))
                    }
                }
                // Water logged on a day without food still needs its list row to stay editable.
                if (uiState.trackWater && uiState.waterEntries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(SectionGap))
                    WaterRowCard(
                        entries = uiState.waterEntries,
                        totalMl = uiState.waterTotalMl,
                        onClick = { showWaterEntries = true },
                    )
                }
            } else {
                val showHeaders =
                    uiState.sections.size > 1 ||
                        uiState.sections.firstOrNull()?.slot != MealSlot.UNSPECIFIED
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(ItemGap),
                    // small clearance so the last card doesn't sit flush on the nav bar
                    contentPadding = PaddingValues(bottom = ScreenPadding),
                ) {
                    uiState.sections.forEach { section ->
                        if (showHeaders) {
                            item(key = "header_${section.slot}") {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(top = LabelGap)
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
                    // Water sits after the meals as one collapsed row; drilling in lists the
                    // individual taps for per-tap delete/edit.
                    if (uiState.trackWater && uiState.waterEntries.isNotEmpty()) {
                        item(key = "water_row") {
                            Box(modifier = Modifier.animateItem()) {
                                WaterRowCard(
                                    entries = uiState.waterEntries,
                                    totalMl = uiState.waterTotalMl,
                                    onClick = { showWaterEntries = true },
                                    // Water belongs to no meal, so it gets the same breathing
                                    // room a meal header puts between two sections instead of
                                    // reading as one more dinner item.
                                    modifier = Modifier.padding(top = SectionGap),
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

    if (showWaterEntries) {
        WaterEntriesDialog(
            entries = uiState.waterEntries,
            onDismiss = { showWaterEntries = false },
            onDelete = { entry -> viewModel.undoWater(listOf(entry)) },
            onEdit = { entry -> editingWaterEntry = entry },
        )
    }

    editingWaterEntry?.let { entry ->
        WaterEditDialog(
            entry = entry,
            onDismiss = { editingWaterEntry = null },
            onConfirm = { ml ->
                viewModel.updateWaterEntry(entry, ml)
                editingWaterEntry = null
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
                    .padding(horizontal = ScreenPadding, vertical = ItemGap)
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
                    .padding(vertical = ItemGap),
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
                Spacer(modifier = Modifier.height(ItemGap))
                MealSlotPicker(
                    selected = selectedSlot ?: MealSlot.UNSPECIFIED,
                    onSelected = { slot ->
                        selectedSlot = if (slot == selectedSlot) null else slot
                    },
                )
                Spacer(modifier = Modifier.height(LabelGap))
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
    // Over goal but under maintenance is amber, past maintenance red — same zone the
    // ring/gauge/bar colors with, so the headline and the indicator never disagree.
    val calorieBand =
        if (uiState.goalCalories != null && uiState.goalCalories > 0) {
            calorieOverBand(uiState.totalCalories, uiState.goalCalories, uiState.maintenanceCalories)
        } else {
            CalorieOverBand.WITHIN
        }
    val hasMacroGoals =
        listOfNotNull(uiState.goalProteinG, uiState.goalFatG, uiState.goalCarbsG)
            .any { it > 0 }

    val style = uiState.homeProgressStyle
    val hasCalorieGoal = uiState.goalCalories != null && uiState.goalCalories > 0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(ScreenPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Weighted so the nutrient lines wrap inside their own space instead of
                // squeezing the ring: with full nutrient names they need two lines on
                // narrow screens, which costs nothing here — the 120dp ring sets the height.
                Column(modifier = Modifier.weight(1f)) {
                    // The compact styles carry the goal in the headline ("X / Y kcal") because
                    // their indicators only show a percentage; the classic ring keeps the
                    // figures in its center, so its headline stays the plain total.
                    Text(
                        if (hasCalorieGoal && style != HomeProgressStyle.RING) {
                            stringResource(
                                R.string.kcal_amount_of_goal,
                                uiState.totalCalories.toInt(),
                                uiState.goalCalories,
                            )
                        } else {
                            stringResource(R.string.kcal_amount, uiState.totalCalories.toInt())
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color =
                            when (calorieBand) {
                                CalorieOverBand.WITHIN -> Color.Unspecified
                                CalorieOverBand.OVER_GOAL -> MaterialTheme.colorScheme.tertiary
                                CalorieOverBand.OVER_MAINTENANCE -> MaterialTheme.colorScheme.error
                            },
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
                            overTolerances = uiState.microOverTolerances,
                        )
                    }
                    TodayWeightLine(uiState)
                }
                when {
                    style == HomeProgressStyle.RING ->
                        MacroProgressRing(
                            consumedCalories = uiState.totalCalories,
                            goalCalories = uiState.goalCalories,
                            maintenanceCalories = uiState.maintenanceCalories,
                        )
                    // "No self-hiding UI": without a goal the indicator slot still says why
                    // it is empty instead of silently vanishing (BAR shows it here too, in
                    // place of the bar that would otherwise render below).
                    !hasCalorieGoal ->
                        Text(
                            stringResource(R.string.progress_no_goal),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    style == HomeProgressStyle.COMPACT_RING ->
                        CompactProgressRing(
                            consumedCalories = uiState.totalCalories,
                            goalCalories = uiState.goalCalories,
                            maintenanceCalories = uiState.maintenanceCalories,
                        )
                    style == HomeProgressStyle.GAUGE ->
                        ProgressGauge(
                            consumedCalories = uiState.totalCalories,
                            goalCalories = uiState.goalCalories,
                            maintenanceCalories = uiState.maintenanceCalories,
                        )
                }
            }
            if (style == HomeProgressStyle.BAR && hasCalorieGoal) {
                Spacer(modifier = Modifier.height(CardBlockGap))
                CalorieProgressBar(
                    consumedCalories = uiState.totalCalories,
                    goalCalories = uiState.goalCalories,
                    maintenanceCalories = uiState.maintenanceCalories,
                )
            }
            if (uiState.goalCalories != null && uiState.goalCalories > 0) {
                Spacer(modifier = Modifier.height(CardBlockGap))
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
                // No carry on past days: a finished day is a factual record, and rolling its
                // deficits forward would repaint a merely light day as an evening failure.
                val dayPartBudgets =
                    buildDayPartBudgets(
                        goalCalories = uiState.goalCalories,
                        shares = uiState.dayPartShares,
                        consumed = uiState.dayPartCalories,
                        nowMinutes = if (uiState.isToday) nowMinutes else 24 * 60,
                        boundaries = uiState.dayPartBoundaries,
                        applyCarry = uiState.isToday,
                    )
                val currentPart =
                    if (uiState.isToday) currentDayPart(nowMinutes, uiState.dayPartBoundaries) else null
                DayPartBalanceBar(
                    dayPartCalories = uiState.dayPartCalories,
                    dayPartBudgets = dayPartBudgets,
                    shares = uiState.dayPartShares,
                    currentPart = currentPart,
                    dayRemaining = uiState.goalCalories - uiState.totalCalories.toInt(),
                    overThresholds = uiState.dayPartOverThresholds,
                )
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
                Spacer(modifier = Modifier.height(CardBlockGap))
                // No spacers between the bars: each row is already as tall as one text line
                // (16sp labels), so stacking them flush gives the same vertical rhythm as
                // consecutive lines in the pace message — the user found 6dp gaps too airy.
                MacroBar(
                    label = stringResource(R.string.macro_protein_short),
                    consumed = uiState.totalProtein,
                    targetG = uiState.goalProteinG,
                    overTolerancePercent = uiState.macroOverTolerances.proteinPercent,
                )
                MacroBar(
                    label = stringResource(R.string.macro_fat_short),
                    consumed = uiState.totalFat,
                    targetG = uiState.goalFatG,
                    overTolerancePercent = uiState.macroOverTolerances.fatPercent,
                )
                MacroBar(
                    label = stringResource(R.string.macro_carbs_short),
                    consumed = uiState.totalCarbs,
                    targetG = uiState.goalCarbsG,
                    overTolerancePercent = uiState.macroOverTolerances.carbsPercent,
                )
                if (uiState.trackWater) {
                    WaterBar(
                        totalMl = uiState.waterTotalMl,
                        targetMl = uiState.waterTargetMl,
                        overThresholds = uiState.waterOverThresholds,
                    )
                }
            } else if (uiState.trackWater) {
                Spacer(modifier = Modifier.height(CardBlockGap))
                WaterBar(
                    totalMl = uiState.waterTotalMl,
                    targetMl = uiState.waterTargetMl,
                    overThresholds = uiState.waterOverThresholds,
                )
            }
        }
    }
}

/**
 * The water scale under the P/F/C bars — same row anatomy (16dp label, bar, icon slot,
 * fixed-width figures) so the four bars align. Reaching the target earns the same check
 * mark as the macro bars, only water-blue; drinking moderately past it is still fine, but
 * far past it the bar warns in amber and then red (user-tunable slack, wide by default).
 * Target comes from the wizard weight (~30 ml/kg) or a flat 2 l without one.
 */
@Composable
private fun WaterBar(
    totalMl: Int,
    targetMl: Int,
    overThresholds: WaterOverThresholds,
) {
    val progress = if (targetMl > 0) (totalMl.toFloat() / targetMl).coerceIn(0f, 1f) else 0f
    val band = waterBand(totalMl, targetMl, overThresholds.amberPercent, overThresholds.redPercent)
    val barColor =
        when (band) {
            WaterBand.WITHIN -> WaterBlue
            WaterBand.OVER_AMBER -> MaterialTheme.colorScheme.tertiary
            WaterBand.OVER_RED -> MaterialTheme.colorScheme.error
        }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.water_short),
            style = MaterialTheme.typography.labelSmall,
            color = WaterBlue,
            modifier = Modifier.width(16.dp),
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier =
                Modifier
                    .weight(1f)
                    .height(6.dp),
            color = barColor,
            trackColor = WaterBlue.copy(alpha = 0.24f),
        )
        Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            when {
                band != WaterBand.WITHIN ->
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = stringResource(R.string.water_over_limit),
                        tint = barColor,
                        modifier = Modifier.size(16.dp),
                    )
                totalMl >= targetMl ->
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.water_goal_reached),
                        tint = WaterBlue,
                        modifier = Modifier.size(16.dp),
                    )
            }
        }
        Text(
            stringResource(R.string.water_bar_value, totalMl, targetMl),
            style = MaterialTheme.typography.labelSmall,
            color = barColor,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.End,
            modifier = Modifier.width(MACRO_FIGURES_WIDTH),
        )
    }
}

/**
 * The diary-list row that collapses the day's water taps, mirroring how a preset collapses
 * its entries: title plus "total · N taps"; tapping opens the per-tap dialog. Deliberately
 * no trailing delete — removing single taps (or all of them) lives in the drill-in.
 */
@Composable
private fun WaterRowCard(
    entries: List<WaterEntry>,
    totalMl: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompactRowCard(
        title = stringResource(R.string.water_row_title),
        subtitle = pluralStringResource(R.plurals.water_row_subtitle, entries.size, totalMl, entries.size),
        onClick = onClick,
        modifier = modifier,
        titleColor = WaterBlue,
    )
}

/** The drill-in behind the water row: every tap with its time, deletable and tappable to edit. */
@Composable
private fun WaterEntriesDialog(
    entries: List<WaterEntry>,
    onDismiss: () -> Unit,
    onDelete: (WaterEntry) -> Unit,
    onEdit: (WaterEntry) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.water_row_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.water_entries_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(ItemGap))
                entries.forEach { entry ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onEdit(entry) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            DateUtils.formatTime(entry.loggedAt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            stringResource(R.string.water_ml_value, entry.amountMl),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onDelete(entry) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        },
    )
}

/** One slider to resize a single tap, same 50 ml steps and range as the glass-size setting. */
@Composable
private fun WaterEditDialog(
    entry: WaterEntry,
    onDismiss: () -> Unit,
    onConfirm: (ml: Int) -> Unit,
) {
    var ml by remember(entry) { mutableIntStateOf(entry.amountMl) }
    val range = SettingsRepository.WATER_GLASS_RANGE_ML

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.water_edit_title)) },
        text = {
            Column {
                Text(stringResource(R.string.water_ml_value, ml))
                Slider(
                    value = ml.toFloat(),
                    onValueChange = { ml = ((it / 50).roundToInt() * 50).coerceIn(range) },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(ml) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/**
 * Gap between the blocks stacked inside the summary card (indicator, day-part bar, macro bars).
 * Tighter than [SectionGap] on purpose: inside one card the blocks belong together.
 */
private val CardBlockGap = 12.dp

/** Water is outside the Material scheme on purpose — no theme slot reads as "water". */
private val WaterBlue = Color(0xFF42A5F5)

/**
 * The day's weigh-in under the nutrient lines, colored by the BMI band it lands in — the
 * same bands and palette as the History BMI tile (incl. the athletic-build shift), so the
 * two never disagree. Neutral when no height is known (wizard never run) and a "—"
 * placeholder when the day has no weigh-in — the line renders in every state per the
 * no-self-hiding-UI guardrail.
 */
@Composable
private fun TodayWeightLine(uiState: DiaryUiState) {
    val weight = uiState.todayWeightKg
    if (weight == null) {
        Text(
            stringResource(R.string.weight_today_none),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val band = bmi(weight, uiState.wizardHeightCm)?.let { bmiBand(it, uiState.athleticBmiBands) }
    Text(
        stringResource(
            R.string.weight_today,
            formatWeightForDisplay(uiState.weightUnit.fromKg(weight)),
            uiState.weightUnit.label(),
        ),
        style = MaterialTheme.typography.bodySmall,
        color =
            when (band) {
                BmiBand.NORMAL -> MaterialTheme.colorScheme.primary
                BmiBand.UNDERWEIGHT, BmiBand.OVERWEIGHT -> MaterialTheme.colorScheme.tertiary
                BmiBand.OBESE -> MaterialTheme.colorScheme.error
                null -> MaterialTheme.colorScheme.onSurfaceVariant
            },
    )
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
    overThresholds: DayPartOverThresholds,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DayPart.entries.forEach { part ->
            DayPartSegment(
                part = part,
                consumed = dayPartCalories[part] ?: 0.0,
                budget = dayPartBudgets[part] ?: DayPartBudget(0, 0, 0),
                isCurrent = part == currentPart,
                dayRemaining = dayRemaining,
                overThresholds = overThresholds,
                modifier = Modifier.weight(shares.shareOf(part)),
            )
        }
    }
}

@Composable
private fun DayPartSegment(
    part: DayPart,
    consumed: Double,
    budget: DayPartBudget,
    isCurrent: Boolean,
    dayRemaining: Int,
    overThresholds: DayPartOverThresholds,
    modifier: Modifier = Modifier,
) {
    val target = budget.target
    val consumedInt = consumed.roundToInt()
    val isOver = consumedInt > target
    // A small overshoot stays green (noise, not a signal); past the configurable slack the
    // segment warns in amber, and turns red only when the day total is over too — red marks
    // a part whose excess belongs to a genuinely blown day, not a lone recoverable slip.
    val band = dayPartOverBand(consumedInt, target, overThresholds, dayOver = dayRemaining < 0)
    val barColor =
        when (band) {
            DayPartOverBand.UNDER, DayPartOverBand.SMALL -> MaterialTheme.colorScheme.primary
            DayPartOverBand.MEDIUM -> MaterialTheme.colorScheme.tertiary
            DayPartOverBand.LARGE -> MaterialTheme.colorScheme.error
        }
    val progress = if (target > 0) (consumed / target).toFloat().coerceIn(0f, 1f) else 0f
    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { progress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            color = barColor,
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
        // A non-zero carry is shown inline as "consumed / base±carry" so the inflated or
        // shrunken target explains itself right where it appears (the separate note line
        // under the bar is gone). "N left" stays capped by what's left of the whole day.
        val effectiveLeft = minOf(target - consumedInt, dayRemaining)
        val figures =
            when {
                isCurrent && !isOver && effectiveLeft > 0 ->
                    stringResource(R.string.day_part_left, effectiveLeft)
                budget.carriedIn != 0 ->
                    stringResource(R.string.day_part_kcal_carry, consumedInt, budget.base, budget.carriedIn)
                else ->
                    stringResource(R.string.day_part_kcal, consumedInt, target)
            }
        Text(
            figures,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrent) FontWeight.SemiBold else null,
            maxLines = 1,
            softWrap = false,
            color =
                when (band) {
                    DayPartOverBand.UNDER, DayPartOverBand.SMALL -> MaterialTheme.colorScheme.onSurfaceVariant
                    DayPartOverBand.MEDIUM -> MaterialTheme.colorScheme.tertiary
                    DayPartOverBand.LARGE -> MaterialTheme.colorScheme.error
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
        // Fixed-width slots on the right (icon, then figures pinned to the card edge) keep
        // all three bars exactly the same length regardless of how many digits each ratio has.
        Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.Center) {
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
            textAlign = TextAlign.End,
            modifier = Modifier.width(MACRO_FIGURES_WIDTH),
        )
    }
}

/** Wide enough for the worst realistic case ("888/888 g") so the ratio never resizes the row. */
private val MACRO_FIGURES_WIDTH = 64.dp

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
