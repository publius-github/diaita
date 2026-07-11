package app.diaita.ui.goals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.diaita.R
import app.diaita.util.ActivityLevel
import app.diaita.util.GoalCalculator
import app.diaita.util.WeightPlan
import app.diaita.util.normalizedDecimalOrNull

/**
 * Mifflin-St Jeor calorie suggestion dialog. Shows the running estimate as soon as all
 * fields parse; Apply hands the number back so the Goals form fills itself in.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalCalculatorDialog(
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit,
) {
    var male by remember { mutableStateOf(true) }
    var ageText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf(ActivityLevel.LIGHT) }
    var plan by remember { mutableStateOf(WeightPlan.MAINTAIN) }

    val age = ageText.toIntOrNull()?.takeIf { it in 10..100 }
    val height = heightText.normalizedDecimalOrNull()?.takeIf { it in 100.0..250.0 }
    val weight = weightText.normalizedDecimalOrNull()?.takeIf { it in 30.0..300.0 }
    val result =
        if (age != null && height != null && weight != null) {
            GoalCalculator.dailyCalories(male, age, height, weight, activity, plan)
        } else {
            null
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calc_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                FlowRow {
                    FilterChip(
                        selected = male,
                        onClick = { male = true },
                        label = { Text(stringResource(R.string.calc_sex_male)) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    FilterChip(
                        selected = !male,
                        onClick = { male = false },
                        label = { Text(stringResource(R.string.calc_sex_female)) },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it },
                    label = { Text(stringResource(R.string.calc_age)) },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    label = { Text(stringResource(R.string.calc_height)) },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text(stringResource(R.string.calc_weight)) },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.calc_activity), style = MaterialTheme.typography.labelMedium)
                FlowRow {
                    ActivityLevel.entries.forEach { level ->
                        FilterChip(
                            selected = activity == level,
                            onClick = { activity = level },
                            label = { Text(level.label()) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.calc_plan), style = MaterialTheme.typography.labelMedium)
                FlowRow {
                    WeightPlan.entries.forEach { weightPlan ->
                        FilterChip(
                            selected = plan == weightPlan,
                            onClick = { plan = weightPlan },
                            label = { Text(weightPlan.label()) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
                result?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.calc_result, it),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { result?.let(onApply) },
                enabled = result != null,
            ) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun ActivityLevel.label(): String =
    stringResource(
        when (this) {
            ActivityLevel.SEDENTARY -> R.string.activity_sedentary
            ActivityLevel.LIGHT -> R.string.activity_light
            ActivityLevel.MODERATE -> R.string.activity_moderate
            ActivityLevel.ACTIVE -> R.string.activity_active
        },
    )

@Composable
private fun WeightPlan.label(): String =
    stringResource(
        when (this) {
            WeightPlan.LOSE -> R.string.plan_lose
            WeightPlan.MAINTAIN -> R.string.plan_maintain
            WeightPlan.GAIN -> R.string.plan_gain
        },
    )
