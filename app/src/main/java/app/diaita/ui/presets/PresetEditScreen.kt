package app.diaita.ui.presets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.common.CompactTextField
import app.diaita.ui.common.FoodRowCard
import app.diaita.ui.common.FoodSearchContent
import app.diaita.ui.common.MealSlotPicker
import app.diaita.ui.common.QuantitySheet
import app.diaita.ui.common.QuickAddDialog
import app.diaita.ui.common.rememberFoodBarcodeScanner
import app.diaita.ui.theme.ItemGap
import app.diaita.ui.theme.ScreenPadding
import app.diaita.ui.theme.SectionGap
import app.diaita.util.NutrientMath
import app.diaita.util.normalizedDecimalOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    presetId: Long? = null,
    /** Creating from the Dishes tab: the cooked weight becomes a required part of the form. */
    isDish: Boolean = false,
    viewModel: PresetViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.editState.collectAsStateWithLifecycle()
    // The food picker is a separate sub-screen instead of an always-visible search list,
    // to keep the edit form itself uncluttered.
    var showFoodPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(presetId, isDish) {
        if (presetId != null && presetId > 0) {
            viewModel.loadForEdit(presetId)
        } else {
            viewModel.startNewDraft(isDish)
        }
    }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.consumeSaved()
            onDone()
        }
    }

    if (showFoodPicker) {
        FoodPicker(
            viewModel = viewModel,
            onClose = {
                viewModel.foodSearch.clearSearch()
                showFoodPicker = false
            },
            modifier = modifier,
        )
    } else {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                when {
                                    state.editingPresetId != null -> R.string.edit_preset
                                    state.isDish -> R.string.new_dish
                                    else -> R.string.new_preset
                                },
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDone) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
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
                        .fillMaxSize()
                        .padding(ScreenPadding),
            ) {
                CompactTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChanged,
                    label = stringResource(R.string.preset_name_label),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                Spacer(modifier = Modifier.height(SectionGap))
                Text(stringResource(R.string.preset_meal_label), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(ItemGap))
                MealSlotPicker(
                    selected = state.mealSlot,
                    onSelected = viewModel::onMealSlotChanged,
                )
                // A plain preset has no cooked weight at all, so a new one started from the
                // Custom tab is not asked for one — that is what keeps each tab's Add button
                // producing the kind of preset the tab lists.
                if (state.isDish || state.editingPresetId != null) {
                    Spacer(modifier = Modifier.height(SectionGap))
                    CookedWeightField(state, viewModel::onCookedWeightChanged)
                }
                Spacer(modifier = Modifier.height(SectionGap))
                Text(stringResource(R.string.items_in_preset), style = MaterialTheme.typography.titleSmall)
                if (state.draftItems.isEmpty()) {
                    Text(
                        stringResource(R.string.no_items_yet),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(modifier = Modifier.height(ItemGap))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(ItemGap),
                ) {
                    itemsIndexed(state.draftItems) { index, draftItem ->
                        // The same shared card as a diary row, so both lists render (and
                        // evolve) identically; tapping a row edits its quantity.
                        FoodRowCard(
                            title = draftItem.food.name,
                            subtitle =
                                stringResource(
                                    R.string.item_grams_kcal,
                                    draftItem.quantityGrams.toInt(),
                                    NutrientMath.caloriesFor(draftItem.food, draftItem.quantityGrams).toInt(),
                                ),
                            onClick = { viewModel.requestEditDraftItem(index) },
                            onDelete = { viewModel.removeDraftItem(index) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(ItemGap))
                OutlinedButton(
                    onClick = { showFoodPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_food))
                }
                if (state.hasError) {
                    Spacer(modifier = Modifier.height(ItemGap))
                    Text(stringResource(R.string.preset_error), color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(ItemGap))
                Button(onClick = viewModel::savePreset, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.save_preset))
                }
            }
        }
    }

    state.foodPendingQuantity?.let { food ->
        QuantitySheet(
            food = food,
            onDismiss = viewModel::dismissQuantityDialog,
            onConfirm = { grams, _ ->
                viewModel.addDraftItem(food, grams)
                showFoodPicker = false
            },
        )
    }

    if (state.showQuickAdd) {
        QuickAddDialog(
            // The preset itself owns the meal slot, so the dialog's picker is hidden and the
            // slot handed back is ignored.
            initialMealSlot = state.mealSlot,
            showMealSlotPicker = false,
            confirmLabel = stringResource(R.string.add),
            onDismiss = viewModel::dismissQuickAdd,
            onConfirm = { name, kcal, _ ->
                viewModel.confirmQuickAdd(name, kcal)
                showFoodPicker = false
            },
        )
    }

    // Tapping an existing item reopens the same quantity sheet the diary edit uses.
    state.editingDraftIndex?.let { index ->
        state.draftItems.getOrNull(index)?.let { draftItem ->
            QuantitySheet(
                food = draftItem.food,
                initialQuantityGrams = draftItem.quantityGrams,
                confirmLabel = stringResource(R.string.save),
                onDismiss = viewModel::cancelDraftItemEdit,
                onConfirm = { grams, _ -> viewModel.confirmDraftItemEdit(grams) },
            )
        }
    }
}

/**
 * Optional cooked-dish weight. Blank keeps a plain one-tap preset; a value turns the preset
 * into a dish logged by portion. The helper line previews the resulting kcal per 100 g of
 * cooked food so the user sees the dish's density while tuning ingredients.
 */
@Composable
private fun CookedWeightField(
    state: PresetEditUiState,
    onValueChange: (String) -> Unit,
) {
    val cookedWeight = state.cookedWeightText.normalizedDecimalOrNull()?.takeIf { it > 0 }
    val totalCalories = state.draftItems.sumOf { NutrientMath.caloriesFor(it.food, it.quantityGrams) }
    CompactTextField(
        value = state.cookedWeightText,
        onValueChange = onValueChange,
        label =
            stringResource(
                if (state.isDish) R.string.cooked_weight_label_required else R.string.cooked_weight_label,
            ),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
        isError = state.hasCookedWeightError,
        supportingText =
            when {
                state.hasCookedWeightError && state.isDish -> stringResource(R.string.cooked_weight_required_error)
                state.hasCookedWeightError -> stringResource(R.string.cooked_weight_error)
                cookedWeight != null && state.draftItems.isNotEmpty() ->
                    stringResource(R.string.kcal_per_100g, (totalCalories / cookedWeight * 100).toInt())
                state.isDish -> stringResource(R.string.cooked_weight_dish_helper)
                else -> stringResource(R.string.cooked_weight_helper)
            },
    )
}

/**
 * Full-screen search-and-pick mode entered via the "Add food" button. Uses the exact same
 * search/scan/manual pane as the main Add-food screen, so barcode scanning works here too.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodPicker(
    viewModel: PresetViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchState by viewModel.foodSearch.state.collectAsStateWithLifecycle()
    val launchScan = rememberFoodBarcodeScanner(viewModel.foodSearch::onBarcodeScanned)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_food)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
        FoodSearchContent(
            state = searchState,
            manualSaveLabel = stringResource(R.string.save_and_add),
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
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(ScreenPadding),
            onQuickAdd = viewModel::showQuickAdd,
        )
    }
}
