package app.diaita.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import app.diaita.R

@Composable
fun LogWeightDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var weightText by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.log_weight)) },
        text = {
            Column {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = {
                        weightText = it
                        hasError = false
                    },
                    label = { Text(stringResource(R.string.weight_kg)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (hasError) {
                    Text(stringResource(R.string.weight_error), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weight = weightText.replace(',', '.').toDoubleOrNull()
                    if (weight != null && weight > 0) onConfirm(weight) else hasError = true
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
