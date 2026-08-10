package app.diaita.ui.addfood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.common.FoodSearchContent
import app.diaita.ui.common.QuantitySheet
import app.diaita.ui.common.QuickAddDialog
import app.diaita.ui.common.friendlyDateLabel
import app.diaita.ui.common.rememberFoodBarcodeScanner
import app.diaita.ui.presets.PresetViewModel
import app.diaita.ui.presets.presetLogList
import app.diaita.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddFoodViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchState by viewModel.foodSearch.state.collectAsStateWithLifecycle()
    val dayPartBoundaries by viewModel.dayPartBoundaries.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val launchScan = rememberFoodBarcodeScanner(viewModel.foodSearch::onBarcodeScanned)

    LaunchedEffect(uiState.justLogged) {
        if (uiState.justLogged) {
            viewModel.consumeJustLogged()
            onDone()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.add_food))
                        // Backfilling another day is rare enough that the date deserves a
                        // callout — otherwise a log quietly lands somewhere unexpected.
                        if (viewModel.logDate != DateUtils.today()) {
                            Text(
                                friendlyDateLabel(viewModel.logDate),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = launchScan) {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = stringResource(R.string.scan_barcode),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
        ) {
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                Tab(
                    selected = uiState.selectedTab == AddFoodTab.FOODS,
                    onClick = { viewModel.onTabSelected(AddFoodTab.FOODS) },
                    text = { Text(stringResource(R.string.tab_foods)) },
                )
                Tab(
                    selected = uiState.selectedTab == AddFoodTab.PRESETS,
                    onClick = { viewModel.onTabSelected(AddFoodTab.PRESETS) },
                    text = { Text(stringResource(R.string.tab_presets)) },
                )
            }

            when (uiState.selectedTab) {
                AddFoodTab.FOODS ->
                    FoodSearchContent(
                        state = searchState,
                        manualSaveLabel = stringResource(R.string.save_and_log),
                        onSearchQueryChanged = viewModel.foodSearch::onSearchQueryChanged,
                        onFoodSelected = viewModel.foodSearch::onFoodSelected,
                        onManualSave = viewModel.foodSearch::saveManualFood,
                        onEditFood = viewModel.foodSearch::onEditFood,
                        onEditSave = viewModel.foodSearch::saveEditedFood,
                        onEditDismiss = viewModel.foodSearch::dismissEdit,
                        onToggleFavorite = viewModel.foodSearch::onToggleFavorite,
                        onSourceFilterChanged = viewModel.foodSearch::onSourceFilterChanged,
                        onLoadMore = viewModel.foodSearch::loadMoreFromOff,
                        onDeleteFood = viewModel.foodSearch::deleteEditedFood,
                        modifier = Modifier.padding(16.dp),
                        onQuickAdd = viewModel::showQuickAdd,
                    )
                AddFoodTab.PRESETS -> PresetsTab(onLogged = onDone, logDate = viewModel.logDate)
            }
        }
    }

    uiState.foodPendingQuantity?.let { food ->
        QuantitySheet(
            food = food,
            initialQuantityGrams = uiState.pendingQuantitySuggestion,
            initialMealSlot = DateUtils.defaultMealSlotNow(dayPartBoundaries),
            showMealSlotPicker = true,
            onDismiss = viewModel::dismissQuantityDialog,
            onConfirm = { grams, slot ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.confirmQuantity(food.id, grams, slot)
            },
        )
    }

    if (uiState.showQuickAdd) {
        QuickAddDialog(
            initialMealSlot = DateUtils.defaultMealSlotNow(dayPartBoundaries),
            onDismiss = viewModel::dismissQuickAdd,
            onConfirm = { name, kcal, slot ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.confirmQuickAdd(name, kcal, slot)
            },
        )
    }
}

@Composable
private fun PresetsTab(
    onLogged: () -> Unit,
    logDate: String,
    presetViewModel: PresetViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val presets by presetViewModel.presets.collectAsStateWithLifecycle()
    val justLogged by presetViewModel.justLogged.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(justLogged) {
        if (justLogged) {
            presetViewModel.consumeJustLogged()
            onLogged()
        }
    }

    if (presets.isEmpty()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.no_presets_addfood))
        }
    } else {
        LazyColumn(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presetLogList(
                presets = presets,
                onLogPreset = { presetId ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    presetViewModel.logPreset(presetId, logDate)
                },
            )
        }
    }
}
