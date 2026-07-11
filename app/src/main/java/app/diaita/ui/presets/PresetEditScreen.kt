package app.diaita.ui.presets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.common.QuantitySheet
import app.diaita.util.NutrientMath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    presetId: Long? = null,
    viewModel: PresetViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.editState.collectAsStateWithLifecycle()
    // The food picker is a separate sub-screen instead of an always-visible search list,
    // to keep the edit form itself uncluttered.
    var showFoodPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(presetId) {
        if (presetId != null && presetId > 0) viewModel.loadForEdit(presetId)
    }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.consumeSaved()
            onDone()
        }
    }

    if (showFoodPicker) {
        FoodPicker(
            state = state,
            viewModel = viewModel,
            onClose = {
                viewModel.onSearchQueryChanged("")
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
                                if (state.editingPresetId != null) R.string.edit_preset else R.string.new_preset,
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
                        .padding(16.dp),
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChanged,
                    label = { Text(stringResource(R.string.preset_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.items_in_preset), style = MaterialTheme.typography.titleSmall)
                if (state.draftItems.isEmpty()) {
                    Text(
                        stringResource(R.string.no_items_yet),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.draftItems) { draftItem ->
                        // Same shape as a diary row (name over grams+kcal, minus the time),
                        // so it's obvious which item is the calorie heavyweight when trimming.
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(draftItem.food.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(
                                        R.string.item_grams_kcal,
                                        draftItem.quantityGrams.toInt(),
                                        NutrientMath.caloriesFor(draftItem.food, draftItem.quantityGrams).toInt(),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { viewModel.removeDraftItem(draftItem) }) {
                                Text(stringResource(R.string.remove))
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { showFoodPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_food))
                }
                if (state.hasError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.preset_error), color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                viewModel.onSearchQueryChanged("")
                showFoodPicker = false
            },
        )
    }
}

/** Full-screen search-and-pick mode entered via the "Add food" button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodPicker(
    state: PresetEditUiState,
    viewModel: PresetViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

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
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                label = { Text(stringResource(R.string.search_to_add)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
            )
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.searchResults, key = { it.id }) { food ->
                    ListItem(
                        headlineContent = { Text(food.name) },
                        supportingContent = {
                            Text(stringResource(R.string.kcal_per_100g, food.caloriesPer100g.toInt()))
                        },
                        modifier = Modifier.clickable { viewModel.onFoodSelected(food) },
                    )
                }
            }
        }
    }
}
