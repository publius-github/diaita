package app.diaita.ui.goals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val goalSavedMessage = stringResource(R.string.goal_saved)
    var showCalculator by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect {
            snackbarHostState.showSnackbar(goalSavedMessage)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.daily_goal)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            OutlinedTextField(
                value = state.calorieTarget,
                onValueChange = viewModel::onCalorieTargetChanged,
                label = { Text(stringResource(R.string.calories_label)) },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { showCalculator = true }) {
                Text(stringResource(R.string.calc_open))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = viewModel::applyQuickSplit, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.quick_split))
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = state.proteinTarget,
                onValueChange = viewModel::onProteinTargetChanged,
                label = { Text(stringResource(R.string.protein_g)) },
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
                value = state.fatTarget,
                onValueChange = viewModel::onFatTargetChanged,
                label = { Text(stringResource(R.string.fat_g)) },
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
                value = state.carbsTarget,
                onValueChange = viewModel::onCarbsTargetChanged,
                label = { Text(stringResource(R.string.carbs_g)) },
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
                value = state.weightTarget,
                onValueChange = viewModel::onWeightTargetChanged,
                label = { Text(stringResource(R.string.weight_target_kg)) },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
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
        }
    }

    if (showCalculator) {
        GoalCalculatorDialog(
            onDismiss = { showCalculator = false },
            onApply = { calories ->
                viewModel.applyCalculatedCalories(calories)
                showCalculator = false
            },
        )
    }
}
