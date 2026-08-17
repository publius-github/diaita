package app.diaita.ui.presets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.diaita.R
import app.diaita.data.repository.PresetSummary
import app.diaita.util.formatGramsForInput
import app.diaita.util.normalizedDecimalOrNull

/**
 * Bottom sheet for logging an eaten portion of a dish: asks for the cooked grams on the
 * plate (prefilled with the whole-dish weight) and previews the resulting calories, which
 * scale linearly — the dish's total kcal times eaten/cooked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishPortionSheet(
    dish: PresetSummary,
    onDismiss: () -> Unit,
    onConfirm: (eatenGrams: Double) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cookedWeight = dish.preset.cookedWeightGrams ?: return
    var portionText by remember(dish.preset.id) { mutableStateOf(formatGramsForInput(cookedWeight)) }

    val eatenGrams = portionText.normalizedDecimalOrNull()?.takeIf { it > 0 }
    val confirm = { eatenGrams?.let(onConfirm) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .navigationBarsPadding()
                    .imePadding(),
        ) {
            Text(dish.preset.name, style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(
                    R.string.dish_whole_summary,
                    cookedWeight.toInt(),
                    dish.totalCalories.toInt(),
                    (dish.totalCalories / cookedWeight * 100).toInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { portionText = "100" },
                    label = { Text(stringResource(R.string.hundred_grams_chip)) },
                )
                AssistChip(
                    onClick = { portionText = formatGramsForInput(cookedWeight) },
                    label = { Text(stringResource(R.string.whole_dish_chip)) },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = portionText,
                onValueChange = { portionText = it },
                label = { Text(stringResource(R.string.dish_portion_label)) },
                singleLine = true,
                isError = eatenGrams == null,
                supportingText = {
                    eatenGrams?.let {
                        Text(
                            stringResource(
                                R.string.kcal_estimate,
                                (dish.totalCalories * it / cookedWeight).toInt(),
                            ),
                        )
                    }
                },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Button(onClick = { confirm() }, enabled = eatenGrams != null) {
                    Text(stringResource(R.string.log))
                }
            }
        }
    }
}
