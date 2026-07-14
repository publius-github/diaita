package app.diaita.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import app.diaita.R
import app.diaita.util.normalizedDecimalOrNull

/**
 * Manual food entry shared by the Add-food screen and the preset food picker. Only the
 * name and calories are required — macros left blank are stored as 0, so a quick "just
 * the calories from the label" entry needs no googling of protein/fat/carbs.
 */
@Composable
fun ManualFoodForm(
    saveLabel: String,
    onBack: () -> Unit,
    onSave: (
        name: String,
        caloriesPer100g: Double,
        proteinPer100g: Double,
        fatPer100g: Double,
        carbsPer100g: Double,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var caloriesText by rememberSaveable { mutableStateOf("") }
    var proteinText by rememberSaveable { mutableStateOf("") }
    var fatText by rememberSaveable { mutableStateOf("") }
    var carbsText by rememberSaveable { mutableStateOf("") }
    var hasError by rememberSaveable { mutableStateOf(false) }

    fun submit() {
        val calories = caloriesText.normalizedDecimalOrNull()
        val protein = proteinText.optionalMacroOrNull()
        val fat = fatText.optionalMacroOrNull()
        val carbs = carbsText.optionalMacroOrNull()
        if (name.isBlank() || calories == null || protein == null || fat == null || carbs == null) {
            hasError = true
            return
        }
        onSave(name.trim(), calories, protein, fat, carbs)
    }

    Column(modifier = modifier.fillMaxSize()) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back_to_search))
        }
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                hasError = false
            },
            label = { Text(stringResource(R.string.name_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        MacroField(
            value = caloriesText,
            onValueChange = {
                caloriesText = it
                hasError = false
            },
            labelRes = R.string.calories_per_100,
        )
        Spacer(modifier = Modifier.height(8.dp))
        MacroField(
            value = proteinText,
            onValueChange = {
                proteinText = it
                hasError = false
            },
            labelRes = R.string.protein_per_100,
        )
        Spacer(modifier = Modifier.height(8.dp))
        MacroField(
            value = fatText,
            onValueChange = {
                fatText = it
                hasError = false
            },
            labelRes = R.string.fat_per_100,
        )
        Spacer(modifier = Modifier.height(8.dp))
        MacroField(
            value = carbsText,
            onValueChange = {
                carbsText = it
                hasError = false
            },
            labelRes = R.string.carbs_per_100,
            imeAction = ImeAction.Done,
            onDone = ::submit,
        )
        if (hasError) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.manual_error), color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = ::submit, modifier = Modifier.fillMaxWidth()) {
            Text(saveLabel)
        }
    }
}

/** Blank means "unknown, store as 0"; anything typed must parse as a number. */
private fun String.optionalMacroOrNull(): Double? = if (isBlank()) 0.0 else normalizedDecimalOrNull()

@Composable
private fun MacroField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = imeAction,
            ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        modifier = Modifier.fillMaxWidth(),
    )
}
