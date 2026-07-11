package app.diaita.ui.addfood

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.presets.PresetViewModel
import app.diaita.util.DateUtils

@Composable
fun AddFoodScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddFoodViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(uiState.justLogged) {
        if (uiState.justLogged) {
            viewModel.consumeJustLogged()
            onDone()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
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
            AddFoodTab.FOODS -> FoodsTab(uiState, viewModel)
            AddFoodTab.PRESETS -> PresetsTab(onLogged = onDone)
        }
    }

    uiState.foodPendingQuantity?.let { food ->
        QuantityDialog(
            food = food,
            initialMealSlot = DateUtils.defaultMealSlotNow(),
            showMealSlotPicker = true,
            onDismiss = viewModel::dismissQuantityDialog,
            onConfirm = { grams, slot ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.confirmQuantity(food.id, grams, slot)
            },
        )
    }
}

@Composable
private fun FoodsTab(uiState: AddFoodUiState, viewModel: AddFoodViewModel) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
    ) {
        if (uiState.showManualEntry) {
            ManualEntryForm(uiState, viewModel)
        } else {
            SearchForm(uiState, viewModel)
        }
    }
}

@Composable
private fun SearchForm(uiState: AddFoodUiState, viewModel: AddFoodViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            label = { Text(stringResource(R.string.search_foods)) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (uiState.searchQuery.isBlank() && uiState.recentFoods.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.recent_foods), style = MaterialTheme.typography.bodySmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.recentFoods, key = { it.id }) { food ->
                    AssistChip(
                        onClick = { viewModel.onFoodSelected(food) },
                        label = { Text(food.name) },
                    )
                }
            }
        }
        if (uiState.openFoodFactsEnabled && !uiState.isOnline) {
            Text(
                stringResource(R.string.offline_banner),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (uiState.searchHadNetworkError) {
            Text(
                stringResource(R.string.search_error_off),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (!uiState.searchHadNetworkError && uiState.searchQuery.isNotBlank() && uiState.searchResults.isEmpty()) {
            Text(
                stringResource(R.string.no_matches),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(uiState.searchResults, key = { it.id }) { food ->
                ListItem(
                    headlineContent = { Text(food.name) },
                    supportingContent = {
                        Text(stringResource(R.string.kcal_per_100g, food.caloriesPer100g.toInt()))
                    },
                    modifier = Modifier.clickable { viewModel.onFoodSelected(food) },
                )
            }
        }
        TextButton(
            onClick = { viewModel.onShowManualEntryChanged(true) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.add_manually))
        }
    }
}

@Composable
private fun ManualEntryForm(uiState: AddFoodUiState, viewModel: AddFoodViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = { viewModel.onShowManualEntryChanged(false) }) {
            Text(stringResource(R.string.back_to_search))
        }
        OutlinedTextField(
            value = uiState.manualName,
            onValueChange = viewModel::onManualNameChanged,
            label = { Text(stringResource(R.string.name_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.manualCalories,
            onValueChange = viewModel::onManualCaloriesChanged,
            label = { Text(stringResource(R.string.calories_per_100)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.manualProtein,
            onValueChange = viewModel::onManualProteinChanged,
            label = { Text(stringResource(R.string.protein_per_100)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.manualFat,
            onValueChange = viewModel::onManualFatChanged,
            label = { Text(stringResource(R.string.fat_per_100)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.manualCarbs,
            onValueChange = viewModel::onManualCarbsChanged,
            label = { Text(stringResource(R.string.carbs_per_100)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        if (uiState.manualHasError) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.manual_error), color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = viewModel::saveManualFood, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.save_and_log))
        }
    }
}

@Composable
private fun PresetsTab(
    onLogged: () -> Unit,
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.no_presets_addfood))
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(presets, key = { it.preset.id }) { presetWithItems ->
                ListItem(
                    headlineContent = { Text(presetWithItems.preset.name) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.preset_items_kcal_tap_to_log,
                                presetWithItems.items.size,
                                presetWithItems.totalCalories.toInt(),
                            ),
                        )
                    },
                    modifier = Modifier.clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        presetViewModel.logPreset(presetWithItems.preset.id)
                    },
                )
            }
        }
    }
}
