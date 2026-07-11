package app.diaita.ui.addfood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.diaita.R
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.MealSlot
import app.diaita.ui.common.label

@Composable
fun QuantityDialog(
    food: FoodItem,
    onDismiss: () -> Unit,
    onConfirm: (grams: Double, mealSlot: MealSlot) -> Unit,
    initialQuantityGrams: Double? = null,
    initialMealSlot: MealSlot = MealSlot.UNSPECIFIED,
    showMealSlotPicker: Boolean = false,
    confirmLabel: String = stringResource(R.string.log),
) {
    var quantityText by remember(food.id) {
        mutableStateOf((initialQuantityGrams ?: food.defaultServingGrams)?.toInt()?.toString() ?: "100")
    }
    var selectedSlot by remember(food.id) { mutableStateOf(initialMealSlot) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food.name) },
        text = {
            Column {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text(stringResource(R.string.quantity_grams)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                food.defaultServingGrams?.let { serving ->
                    TextButton(onClick = { quantityText = serving.toInt().toString() }) {
                        Text(stringResource(R.string.use_default_serving, serving.toInt()))
                    }
                }
                if (showMealSlotPicker) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MealSlotPicker(
                        selected = selectedSlot,
                        onSelected = { slot ->
                            selectedSlot = if (selectedSlot == slot) MealSlot.UNSPECIFIED else slot
                        },
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { quantityText.toDoubleOrNull()?.let { onConfirm(it, selectedSlot) } }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun MealSlotPicker(selected: MealSlot, onSelected: (MealSlot) -> Unit) {
    val slots = listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER, MealSlot.SNACK)
    Column {
        slots.chunked(2).forEach { rowSlots ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowSlots.forEach { slot ->
                    FilterChip(
                        selected = selected == slot,
                        onClick = { onSelected(slot) },
                        label = { Text(slot.label()) },
                    )
                }
            }
        }
    }
}
