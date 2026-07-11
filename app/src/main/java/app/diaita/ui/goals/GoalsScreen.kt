package app.diaita.ui.goals

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.ui.AppViewModelProvider
import kotlinx.coroutines.launch

@Composable
fun GoalsScreen(
    modifier: Modifier = Modifier,
    viewModel: GoalsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val goalSavedMessage = stringResource(R.string.goal_saved)
    val exportedMessage = stringResource(R.string.diary_exported)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val csv = viewModel.exportDiaryCsv()
            context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
            snackbarHostState.showSnackbar(exportedMessage)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect {
            snackbarHostState.showSnackbar(goalSavedMessage)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.daily_goal), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = state.calorieTarget,
                onValueChange = viewModel::onCalorieTargetChanged,
                label = { Text(stringResource(R.string.calories_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = viewModel::applyQuickSplit, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.quick_split))
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = state.proteinTarget,
                onValueChange = viewModel::onProteinTargetChanged,
                label = { Text(stringResource(R.string.protein_g)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.fatTarget,
                onValueChange = viewModel::onFatTargetChanged,
                label = { Text(stringResource(R.string.fat_g)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.carbsTarget,
                onValueChange = viewModel::onCarbsTargetChanged,
                label = { Text(stringResource(R.string.carbs_g)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.weightTarget,
                onValueChange = viewModel::onWeightTargetChanged,
                label = { Text(stringResource(R.string.weight_target_kg)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.hasError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.goal_error), color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = viewModel::saveGoal, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save_goal))
            }
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { exportLauncher.launch("calorie_counter_export.csv") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.export_csv))
            }
        }
    }
}
