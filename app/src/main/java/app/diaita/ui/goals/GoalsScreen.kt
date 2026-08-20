package app.diaita.ui.goals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.common.CompactTextField
import app.diaita.ui.common.label
import app.diaita.ui.theme.FieldGap
import app.diaita.ui.theme.ItemGap
import app.diaita.ui.theme.ScreenPadding
import app.diaita.ui.theme.SectionGap
import app.diaita.util.formatWeightForDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onBack: () -> Unit,
    onOpenWizard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val goalSavedMessage = stringResource(R.string.goal_saved)

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
                    .padding(ScreenPadding),
        ) {
            if (state.suggestRecalc) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    // Message and action side by side: stacked, the button's own 40dp of
                    // height showed up as empty space under a two-line sentence.
                    Row(
                        modifier =
                            Modifier.padding(
                                start = ScreenPadding,
                                end = ItemGap,
                                top = ItemGap,
                                bottom = ItemGap,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(
                                R.string.recalc_suggestion,
                                formatWeightForDisplay(state.weightUnit.fromKg(state.recalcFromKg ?: 0.0)),
                                formatWeightForDisplay(state.weightUnit.fromKg(state.recalcToKg ?: 0.0)),
                                state.weightUnit.label(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(ItemGap))
                        TextButton(
                            onClick = onOpenWizard,
                            contentPadding = PaddingValues(horizontal = ItemGap),
                        ) {
                            Text(stringResource(R.string.recalc_action))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(SectionGap))
            }
            CompactTextField(
                value = state.calorieTarget,
                onValueChange = viewModel::onCalorieTargetChanged,
                label = stringResource(R.string.calories_label),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onOpenWizard) {
                Text(stringResource(R.string.calc_open))
            }
            Spacer(modifier = Modifier.height(ItemGap))
            OutlinedButton(onClick = viewModel::applyQuickSplit, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.quick_split))
            }
            Spacer(modifier = Modifier.height(SectionGap))
            CompactTextField(
                value = state.proteinTarget,
                onValueChange = viewModel::onProteinTargetChanged,
                label = stringResource(R.string.protein_g),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(FieldGap))
            CompactTextField(
                value = state.fatTarget,
                onValueChange = viewModel::onFatTargetChanged,
                label = stringResource(R.string.fat_g),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(FieldGap))
            CompactTextField(
                value = state.carbsTarget,
                onValueChange = viewModel::onCarbsTargetChanged,
                label = stringResource(R.string.carbs_g),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(FieldGap))
            CompactTextField(
                value = state.weightTarget,
                onValueChange = viewModel::onWeightTargetChanged,
                label = stringResource(R.string.weight_target_label, state.weightUnit.label()),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.hasError) {
                Spacer(modifier = Modifier.height(ItemGap))
                Text(stringResource(R.string.goal_error), color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(SectionGap))
            Button(onClick = viewModel::saveGoal, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save_goal))
            }
        }
    }
}
