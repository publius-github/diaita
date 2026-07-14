package app.diaita.ui.addfood

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import app.diaita.R
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.common.ManualFoodForm
import app.diaita.ui.common.QuantitySheet
import app.diaita.ui.presets.PresetViewModel
import app.diaita.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddFoodViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val scanPrompt = stringResource(R.string.scan_prompt)

    val scanLauncher =
        rememberLauncherForActivityResult(ScanContract()) { result ->
            result.contents?.let(viewModel::onBarcodeScanned)
        }

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
                title = { Text(stringResource(R.string.add_food)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scanLauncher.launch(
                                ScanOptions()
                                    .setDesiredBarcodeFormats(ScanOptions.PRODUCT_CODE_TYPES)
                                    .setPrompt(scanPrompt)
                                    .setBeepEnabled(false),
                            )
                        },
                    ) {
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
                AddFoodTab.FOODS -> FoodsTab(uiState, viewModel)
                AddFoodTab.PRESETS -> PresetsTab(onLogged = onDone)
            }
        }
    }

    uiState.foodPendingQuantity?.let { food ->
        QuantitySheet(
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
private fun FoodsTab(
    uiState: AddFoodUiState,
    viewModel: AddFoodViewModel,
) {
    Column(
        modifier =
            Modifier
                .padding(16.dp)
                .fillMaxSize(),
    ) {
        if (uiState.showManualEntry) {
            ManualFoodForm(
                saveLabel = stringResource(R.string.save_and_log),
                onBack = { viewModel.onShowManualEntryChanged(false) },
                onSave = viewModel::saveManualFood,
            )
        } else {
            SearchForm(uiState, viewModel)
        }
    }
}

@Composable
private fun SearchForm(
    uiState: AddFoodUiState,
    viewModel: AddFoodViewModel,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            label = { Text(stringResource(R.string.search_foods)) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.offSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
        )
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
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
        uiState.scanError?.let { error ->
            Text(
                stringResource(
                    when (error) {
                        ScanError.NOT_FOUND -> R.string.scan_not_found
                        ScanError.NETWORK -> R.string.search_error_off
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (!uiState.searchHadNetworkError &&
            !uiState.offSearching &&
            uiState.searchQuery.isNotBlank() &&
            uiState.searchResults.isEmpty()
        ) {
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
                    modifier =
                        Modifier.clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            presetViewModel.logPreset(presetWithItems.preset.id)
                        },
                )
            }
        }
    }
}
