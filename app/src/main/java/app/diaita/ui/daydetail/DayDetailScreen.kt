package app.diaita.ui.daydetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.data.db.entities.MealSlot
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.common.FoodRowCard
import app.diaita.ui.common.MicronutrientTotalsLine
import app.diaita.ui.common.QuantitySheet
import app.diaita.ui.common.label
import app.diaita.ui.diary.DiaryRow
import app.diaita.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DayDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editingEntry by viewModel.editingEntry.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.undo)
    val isToday = uiState.date == DateUtils.today()

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
        topBar = {
            TopAppBar(
                title = { Text(formatDate(uiState.date)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (!isToday && uiState.sections.isNotEmpty()) {
                        IconButton(onClick = { viewModel.copyToToday() }) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = stringResource(R.string.copy_day_to_today),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.kcal_amount, uiState.totalCalories.toInt()),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    if (uiState.trackMacros) {
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
                    if (uiState.trackMicronutrients) {
                        MicronutrientTotalsLine(
                            fiberGrams = uiState.totalFiber,
                            sugarGrams = uiState.totalSugar,
                            saltGrams = uiState.totalSalt,
                            hasEntries = uiState.sections.isNotEmpty(),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.sections.isEmpty()) {
                Text(stringResource(R.string.day_detail_empty), style = MaterialTheme.typography.bodyMedium)
            } else {
                val showHeaders =
                    uiState.sections.size > 1 ||
                        uiState.sections.firstOrNull()?.slot != MealSlot.UNSPECIFIED
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.sections.forEach { section ->
                        if (showHeaders) {
                            item(key = "header_${section.slot}") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(section.slot.label(), style = MaterialTheme.typography.titleSmall)
                                    if (!isToday && section.slot != MealSlot.UNSPECIFIED) {
                                        TextButton(onClick = { viewModel.copyToToday(section.slot) }) {
                                            Text(stringResource(R.string.copy_meal_to_today))
                                        }
                                    }
                                }
                            }
                        }
                        items(section.rows, key = ::rowKey) { row ->
                            DayDetailRowCard(
                                row = row,
                                onClick =
                                    if (row is DiaryRow.Food) {
                                        { viewModel.requestEdit(row) }
                                    } else {
                                        null
                                    },
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
private fun DayDetailRowCard(
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

private fun formatDate(isoDate: String): String =
    runCatching {
        LocalDate
            .parse(isoDate)
            .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
    }.getOrDefault(isoDate)
