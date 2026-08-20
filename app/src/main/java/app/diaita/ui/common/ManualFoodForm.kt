package app.diaita.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import app.diaita.R
import app.diaita.data.db.entities.FoodItem
import app.diaita.ui.theme.FieldGap
import app.diaita.ui.theme.ItemGap
import app.diaita.ui.theme.LabelGap
import app.diaita.ui.theme.SectionGap
import app.diaita.util.normalizedDecimalOrNull

/** Everything the manual-entry/edit form captures about a food; nulls mean "not provided". */
data class FoodDraft(
    val name: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val fiberPer100g: Double? = null,
    val sugarPer100g: Double? = null,
    val saltPer100g: Double? = null,
    val defaultServingGrams: Double? = null,
    val gramsPerMilliliter: Double? = null,
    val gramsPerPiece: Double? = null,
    /** Set when the entry started from a scan of an unknown product; keeps it scannable later. */
    val barcode: String? = null,
)

/**
 * Manual food entry shared by the Add-food screen and the preset food picker. Only the
 * name and calories are required — macros left blank are stored as 0, so a quick "just
 * the calories from the label" entry needs no googling of protein/fat/carbs. Extended
 * nutrients (fiber/sugar/salt) and unit conversions (serving, ml, piece) are optional.
 *
 * With [initial] set, the form becomes an editor prefilled from that food — used to fix
 * mistakes in catalog entries (seeded, manual or cached OpenFoodFacts) straight from search.
 *
 * With [barcode] set (a scan that OpenFoodFacts didn't know), the code is shown and attached to
 * the saved food, so the same product resolves from the local cache on every later scan.
 */
@Composable
fun ManualFoodForm(
    saveLabel: String,
    onBack: () -> Unit,
    onSave: (FoodDraft) -> Unit,
    modifier: Modifier = Modifier,
    initial: FoodItem? = null,
    barcode: String? = null,
    /** Offered only when editing an existing catalog food; null hides the action. */
    onDelete: (() -> Unit)? = null,
    /** True when a delete was refused because diary entries still reference the food. */
    deleteBlocked: Boolean = false,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var caloriesText by rememberSaveable {
        mutableStateOf(
            initial?.caloriesPer100g?.toFieldText(showZero = true) ?: "",
        )
    }
    var proteinText by rememberSaveable { mutableStateOf(initial?.proteinPer100g?.toFieldText() ?: "") }
    var fatText by rememberSaveable { mutableStateOf(initial?.fatPer100g?.toFieldText() ?: "") }
    var carbsText by rememberSaveable { mutableStateOf(initial?.carbsPer100g?.toFieldText() ?: "") }
    var fiberText by rememberSaveable { mutableStateOf(initial?.fiberPer100g?.toFieldText() ?: "") }
    var sugarText by rememberSaveable { mutableStateOf(initial?.sugarPer100g?.toFieldText() ?: "") }
    var saltText by rememberSaveable { mutableStateOf(initial?.saltPer100g?.toFieldText() ?: "") }
    var servingText by rememberSaveable { mutableStateOf(initial?.defaultServingGrams?.toFieldText() ?: "") }
    var gramsPerMlText by rememberSaveable { mutableStateOf(initial?.gramsPerMilliliter?.toFieldText() ?: "") }
    var gramsPerPieceText by rememberSaveable { mutableStateOf(initial?.gramsPerPiece?.toFieldText() ?: "") }
    var hasError by rememberSaveable { mutableStateOf(false) }

    fun submit() {
        val calories = caloriesText.normalizedDecimalOrNull()
        val protein = proteinText.optionalMacroOrNull()
        val fat = fatText.optionalMacroOrNull()
        val carbs = carbsText.optionalMacroOrNull()
        val fiber = fiberText.optionalPositiveOrBlank()
        val sugar = sugarText.optionalPositiveOrBlank()
        val salt = saltText.optionalPositiveOrBlank()
        val serving = servingText.optionalPositiveOrBlank()
        val gramsPerMl = gramsPerMlText.optionalPositiveOrBlank()
        val gramsPerPiece = gramsPerPieceText.optionalPositiveOrBlank()
        val optionalFieldsValid =
            listOf(fiberText, sugarText, saltText, servingText, gramsPerMlText, gramsPerPieceText)
                .zip(listOf(fiber, sugar, salt, serving, gramsPerMl, gramsPerPiece))
                .all { (text, parsed) -> text.isBlank() || parsed != null }
        if (name.isBlank() ||
            calories == null ||
            protein == null ||
            fat == null ||
            carbs == null ||
            !optionalFieldsValid
        ) {
            hasError = true
            return
        }
        onSave(
            FoodDraft(
                name = name.trim(),
                caloriesPer100g = calories,
                proteinPer100g = protein,
                fatPer100g = fat,
                carbsPer100g = carbs,
                fiberPer100g = fiber,
                sugarPer100g = sugar,
                saltPer100g = salt,
                defaultServingGrams = serving,
                gramsPerMilliliter = gramsPerMl,
                gramsPerPiece = gramsPerPiece,
                barcode = barcode,
            ),
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back_to_search))
        }
        barcode?.let { code ->
            Text(
                stringResource(R.string.manual_scanned_barcode, code),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(LabelGap))
        }
        CompactTextField(
            value = name,
            onValueChange = {
                name = it
                hasError = false
            },
            label = stringResource(R.string.name_label),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Spacer(modifier = Modifier.height(FieldGap))
        MacroField(
            value = caloriesText,
            onValueChange = {
                caloriesText = it
                hasError = false
            },
            labelRes = R.string.calories_per_100,
        )
        Spacer(modifier = Modifier.height(FieldGap))
        MacroField(
            value = proteinText,
            onValueChange = {
                proteinText = it
                hasError = false
            },
            labelRes = R.string.protein_per_100,
        )
        Spacer(modifier = Modifier.height(FieldGap))
        MacroField(
            value = fatText,
            onValueChange = {
                fatText = it
                hasError = false
            },
            labelRes = R.string.fat_per_100,
        )
        Spacer(modifier = Modifier.height(FieldGap))
        MacroField(
            value = carbsText,
            onValueChange = {
                carbsText = it
                hasError = false
            },
            labelRes = R.string.carbs_per_100,
        )
        Spacer(modifier = Modifier.height(FieldGap))
        Row(horizontalArrangement = Arrangement.spacedBy(ItemGap)) {
            MacroField(
                value = fiberText,
                onValueChange = {
                    fiberText = it
                    hasError = false
                },
                labelRes = R.string.fiber_per_100,
                modifier = Modifier.weight(1f),
            )
            MacroField(
                value = sugarText,
                onValueChange = {
                    sugarText = it
                    hasError = false
                },
                labelRes = R.string.sugar_per_100,
                modifier = Modifier.weight(1f),
            )
            MacroField(
                value = saltText,
                onValueChange = {
                    saltText = it
                    hasError = false
                },
                labelRes = R.string.salt_per_100,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(ItemGap))
        Text(
            stringResource(R.string.unit_conversions_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(LabelGap))
        Row(horizontalArrangement = Arrangement.spacedBy(ItemGap)) {
            MacroField(
                value = servingText,
                onValueChange = {
                    servingText = it
                    hasError = false
                },
                labelRes = R.string.serving_grams_label,
                modifier = Modifier.weight(1f),
            )
            MacroField(
                value = gramsPerMlText,
                onValueChange = {
                    gramsPerMlText = it
                    hasError = false
                },
                labelRes = R.string.grams_per_ml_label,
                modifier = Modifier.weight(1f),
            )
            MacroField(
                value = gramsPerPieceText,
                onValueChange = {
                    gramsPerPieceText = it
                    hasError = false
                },
                labelRes = R.string.grams_per_piece_label,
                imeAction = ImeAction.Done,
                onDone = ::submit,
                modifier = Modifier.weight(1f),
            )
        }
        if (hasError) {
            Spacer(modifier = Modifier.height(ItemGap))
            Text(stringResource(R.string.manual_error), color = MaterialTheme.colorScheme.error)
        }
        if (deleteBlocked) {
            Spacer(modifier = Modifier.height(ItemGap))
            Text(stringResource(R.string.delete_food_blocked), color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(SectionGap))
        Button(onClick = ::submit, modifier = Modifier.fillMaxWidth()) {
            Text(saveLabel)
        }
        onDelete?.let { delete ->
            Spacer(modifier = Modifier.height(LabelGap))
            TextButton(onClick = delete, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.delete_food_from_catalog),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Blank means "unknown, store as 0"; anything typed must parse as a number. */
private fun String.optionalMacroOrNull(): Double? = if (isBlank()) 0.0 else normalizedDecimalOrNull()

/** Blank means "not provided" (null); anything typed must parse as a positive number. */
private fun String.optionalPositiveOrBlank(): Double? =
    if (isBlank()) null else normalizedDecimalOrNull()?.takeIf { it > 0 }

/**
 * Prefill text for a numeric field: whole values lose the ".0" noise, and 0 collapses to
 * blank for optional macros (mirroring [optionalMacroOrNull]) unless [showZero] forces it
 * (calories are required, so an explicit 0 must stay visible).
 */
private fun Double.toFieldText(showZero: Boolean = false): String =
    when {
        this == 0.0 && !showZero -> ""
        this % 1.0 == 0.0 -> toLong().toString()
        else -> toString()
    }

@Composable
private fun MacroField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
) {
    CompactTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(labelRes),
        modifier = modifier.fillMaxWidth(),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = imeAction,
            ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
    )
}
