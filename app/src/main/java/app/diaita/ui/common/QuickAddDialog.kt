package app.diaita.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import app.diaita.R
import app.diaita.data.db.entities.MealSlot
import app.diaita.ui.theme.ItemGap
import app.diaita.util.normalizedDecimalOrNull

/**
 * Calorie-only entry for food you can't weigh or look up — a restaurant plate, a slice of
 * someone's birthday cake. Just the kcal estimate (required), an optional label, and the meal
 * slot; no catalog entry to pick or create. Shared by the Add-food screen (logs to the diary)
 * and the preset editor's food picker (adds a draft item, where the preset owns the meal slot,
 * so [showMealSlotPicker] is off there).
 */
@Composable
fun QuickAddDialog(
    initialMealSlot: MealSlot,
    onDismiss: () -> Unit,
    onConfirm: (name: String, kcal: Double, mealSlot: MealSlot) -> Unit,
    confirmLabel: String = stringResource(R.string.log),
    showMealSlotPicker: Boolean = true,
) {
    var kcalText by remember { mutableStateOf("") }
    var nameText by remember { mutableStateOf("") }
    var selectedSlot by remember { mutableStateOf(initialMealSlot) }
    val focusRequester = remember { FocusRequester() }

    val kcal = kcalText.normalizedDecimalOrNull()?.takeIf { it > 0 }
    val defaultName = stringResource(R.string.quick_entry_default_name)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quick_add_title)) },
        text = {
            Column {
                CompactTextField(
                    value = kcalText,
                    onValueChange = { kcalText = it },
                    label = stringResource(R.string.quick_add_kcal_label),
                    isError = kcalText.isNotBlank() && kcal == null,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = focusRequester,
                )
                Spacer(modifier = Modifier.height(ItemGap))
                CompactTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = stringResource(R.string.quick_add_name_label),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showMealSlotPicker) {
                    Spacer(modifier = Modifier.height(ItemGap))
                    MealSlotPicker(selected = selectedSlot, onSelected = { selectedSlot = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = kcal != null,
                onClick = { kcal?.let { onConfirm(nameText.trim().ifBlank { defaultName }, it, selectedSlot) } },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
