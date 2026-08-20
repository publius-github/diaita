package app.diaita.ui.common

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.diaita.R
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.MealSlot
import app.diaita.ui.theme.FieldGap
import app.diaita.ui.theme.ItemGap
import app.diaita.ui.theme.SectionGap
import app.diaita.util.NutrientMath
import app.diaita.util.formatGramsForInput
import app.diaita.util.normalizedDecimalOrNull

/** Input units the quantity sheet offers; availability depends on the food's conversions. */
enum class QuantityUnit {
    GRAMS,
    MILLILITERS,
    PIECES,
    SERVINGS,
    ;

    /** Grams one unit of this measure weighs for [food]; null when the food lacks the conversion. */
    fun gramsPerUnit(food: FoodItem): Double? =
        when (this) {
            GRAMS -> 1.0
            MILLILITERS -> food.gramsPerMilliliter
            PIECES -> food.gramsPerPiece
            SERVINGS -> food.defaultServingGrams
        }

    companion object {
        fun availableFor(food: FoodItem): List<QuantityUnit> = entries.filter { it.gramsPerUnit(food) != null }
    }
}

/**
 * Bottom sheet for choosing how much of [food] to log (or how much an entry should
 * become when editing). Offers quick serving chips, unit selection (g / ml / pieces /
 * servings, per the food's conversions), live kcal preview and an optional meal-slot picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuantitySheet(
    food: FoodItem,
    onDismiss: () -> Unit,
    onConfirm: (grams: Double, mealSlot: MealSlot) -> Unit,
    initialQuantityGrams: Double? = null,
    initialMealSlot: MealSlot = MealSlot.UNSPECIFIED,
    showMealSlotPicker: Boolean = false,
    confirmLabel: String = stringResource(R.string.log),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantityText by remember(food.id) {
        mutableStateOf(formatGramsForInput(initialQuantityGrams ?: food.defaultServingGrams ?: 100.0))
    }
    var selectedSlot by remember(food.id) { mutableStateOf(initialMealSlot) }
    var unit by remember(food.id) { mutableStateOf(QuantityUnit.GRAMS) }
    val availableUnits = remember(food.id) { QuantityUnit.availableFor(food) }
    val focusRequester = remember { FocusRequester() }

    val amount = quantityText.normalizedDecimalOrNull()?.takeIf { it > 0 }
    val grams = amount?.let { it * (unit.gramsPerUnit(food) ?: 1.0) }
    val confirm = { grams?.let { onConfirm(it, selectedSlot) } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .navigationBarsPadding()
                    .imePadding(),
        ) {
            Text(food.name, style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.kcal_per_100g, food.caloriesPer100g.toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(ItemGap))
            if (availableUnits.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(ItemGap)) {
                    availableUnits.forEach { candidate ->
                        FilterChip(
                            selected = unit == candidate,
                            onClick = {
                                if (unit != candidate) {
                                    // Convert the typed amount so the grams stay the same.
                                    val currentGrams = grams
                                    unit = candidate
                                    currentGrams?.let { g ->
                                        candidate.gramsPerUnit(food)?.let { perUnit ->
                                            quantityText = formatGramsForInput(g / perUnit)
                                        }
                                    }
                                }
                            },
                            label = { Text(unitLabel(candidate)) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(ItemGap))
            }
            if (unit == QuantityUnit.GRAMS) {
                Row(horizontalArrangement = Arrangement.spacedBy(ItemGap)) {
                    AssistChip(
                        onClick = { quantityText = "100" },
                        label = { Text(stringResource(R.string.hundred_grams_chip)) },
                    )
                    food.defaultServingGrams?.let { serving ->
                        AssistChip(
                            onClick = { quantityText = formatGramsForInput(serving) },
                            label = { Text(stringResource(R.string.serving_chip, serving.toInt())) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(FieldGap))
            }
            CompactTextField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = quantityFieldLabel(unit),
                isError = grams == null,
                supportingText =
                    grams?.let {
                        listOfNotNull(
                            stringResource(
                                R.string.kcal_estimate,
                                NutrientMath.caloriesFor(food, it).toInt(),
                            ),
                            if (unit != QuantityUnit.GRAMS) {
                                stringResource(R.string.grams_equivalent, formatGramsForInput(it))
                            } else {
                                null
                            },
                        ).joinToString(" · ")
                    },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
            )
            if (showMealSlotPicker) {
                Spacer(modifier = Modifier.height(FieldGap))
                MealSlotPicker(selected = selectedSlot, onSelected = { selectedSlot = it })
            }
            Spacer(modifier = Modifier.height(SectionGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ItemGap, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Button(onClick = { confirm() }, enabled = grams != null) { Text(confirmLabel) }
            }
        }
    }
    LaunchedEffect(food.id) { focusRequester.requestFocus() }
}

@Composable
private fun unitLabel(unit: QuantityUnit): String =
    stringResource(
        when (unit) {
            QuantityUnit.GRAMS -> R.string.unit_grams
            QuantityUnit.MILLILITERS -> R.string.unit_milliliters
            QuantityUnit.PIECES -> R.string.unit_pieces
            QuantityUnit.SERVINGS -> R.string.unit_servings
        },
    )

@Composable
private fun quantityFieldLabel(unit: QuantityUnit): String =
    stringResource(
        when (unit) {
            QuantityUnit.GRAMS -> R.string.quantity_grams
            QuantityUnit.MILLILITERS -> R.string.quantity_milliliters
            QuantityUnit.PIECES -> R.string.quantity_pieces
            QuantityUnit.SERVINGS -> R.string.quantity_servings
        },
    )
