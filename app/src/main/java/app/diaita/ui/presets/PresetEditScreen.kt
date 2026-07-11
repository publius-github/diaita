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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.addfood.QuantityDialog

@Composable
fun PresetEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    presetId: Long? = null,
    viewModel: PresetViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.editState.collectAsStateWithLifecycle()

    LaunchedEffect(presetId) {
        if (presetId != null && presetId > 0) viewModel.loadForEdit(presetId)
    }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.consumeSaved()
            onDone()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChanged,
            label = { Text(stringResource(R.string.preset_name_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.items_in_preset), style = MaterialTheme.typography.titleSmall)
        if (state.draftItems.isEmpty()) {
            Text(
                stringResource(R.string.no_items_yet),
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            state.draftItems.forEach { draftItem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(
                            R.string.item_with_grams,
                            draftItem.food.name,
                            draftItem.quantityGrams.toInt(),
                        ),
                    )
                    TextButton(onClick = { viewModel.removeDraftItem(draftItem) }) {
                        Text(stringResource(R.string.remove))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            label = { Text(stringResource(R.string.search_to_add)) },
            modifier = Modifier.fillMaxWidth(),
        )
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
        if (state.hasError) {
            Text(stringResource(R.string.preset_error), color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(onClick = viewModel::savePreset, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.save_preset))
        }
    }

    state.foodPendingQuantity?.let { food ->
        QuantityDialog(
            food = food,
            onDismiss = viewModel::dismissQuantityDialog,
            onConfirm = { grams, _ -> viewModel.addDraftItem(food, grams) },
        )
    }
}
