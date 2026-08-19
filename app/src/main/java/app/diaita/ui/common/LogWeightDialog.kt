package app.diaita.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import app.diaita.R
import app.diaita.data.settings.WeightUnit
import app.diaita.util.normalizedDecimalOrNull

/** Weight input in the user's display unit; [onConfirm] always receives kilograms. */
@Composable
fun LogWeightDialog(
    weightUnit: WeightUnit,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var weightText by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val confirm = {
        val weight = weightText.normalizedDecimalOrNull()
        if (weight != null && weight > 0) onConfirm(weightUnit.toKg(weight)) else hasError = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.log_weight)) },
        text = {
            Column {
                CompactTextField(
                    value = weightText,
                    onValueChange = {
                        weightText = it
                        hasError = false
                    },
                    label = stringResource(R.string.weight_input_label, weightUnit.label()),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    focusRequester = focusRequester,
                )
                if (hasError) {
                    Text(stringResource(R.string.weight_error), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = { confirm() }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
