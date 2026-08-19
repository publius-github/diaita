package app.diaita.ui.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.data.settings.MacroSplit
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.common.CompactTextField
import app.diaita.ui.common.label
import app.diaita.ui.theme.ItemGap
import app.diaita.ui.theme.LabelGap
import app.diaita.ui.theme.ScreenPadding
import app.diaita.ui.theme.SectionGap
import app.diaita.util.ExerciseLevel
import app.diaita.util.WeightPlan
import app.diaita.util.WorkActivity

/**
 * Step-by-step calorie goal wizard: about you, work activity, exercise, plan, then a
 * Mifflin-St Jeor based suggestion the user can apply as the daily goal in one tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalWizardScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalWizardViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect { onDone() }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wizard_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(ScreenPadding),
        ) {
            Text(
                stringResource(R.string.wizard_step, state.step + 1, GoalWizardViewModel.LAST_STEP + 1),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(ItemGap))
            when (state.step) {
                0 -> AboutYouStep(state, viewModel)
                1 -> WorkStep(state, viewModel)
                2 -> ExerciseStep(state, viewModel)
                3 -> PlanStep(state, viewModel)
                else -> ResultStep(state, viewModel)
            }
            Spacer(modifier = Modifier.height(SectionGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.step > 0) {
                    TextButton(onClick = viewModel::previousStep) {
                        Text(stringResource(R.string.back))
                    }
                } else {
                    // Empty slot so SpaceBetween still parks Next on the right without Back.
                    Spacer(modifier = Modifier)
                }
                if (state.step < GoalWizardViewModel.LAST_STEP) {
                    Button(
                        onClick = viewModel::nextStep,
                        enabled = state.step != 0 || state.aboutYouValid,
                    ) { Text(stringResource(R.string.wizard_next)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutYouStep(
    state: GoalWizardUiState,
    viewModel: GoalWizardViewModel,
) {
    Text(stringResource(R.string.wizard_about_you), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(ItemGap))
    FlowRow {
        FilterChip(
            selected = state.male,
            onClick = { viewModel.onSexSelected(true) },
            label = { Text(stringResource(R.string.calc_sex_male)) },
            modifier = Modifier.padding(end = 8.dp),
        )
        FilterChip(
            selected = !state.male,
            onClick = { viewModel.onSexSelected(false) },
            label = { Text(stringResource(R.string.calc_sex_female)) },
        )
    }
    Spacer(modifier = Modifier.height(ItemGap))
    CompactTextField(
        value = state.ageText,
        onValueChange = viewModel::onAgeChanged,
        label = stringResource(R.string.calc_age),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(ItemGap))
    if (state.useImperialHeight) {
        // Pound users think in feet and inches; the math still runs on centimeters.
        Row(horizontalArrangement = Arrangement.spacedBy(ItemGap)) {
            CompactTextField(
                value = state.heightFeetText,
                onValueChange = viewModel::onHeightFeetChanged,
                label = stringResource(R.string.calc_height_feet),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.weight(1f),
            )
            CompactTextField(
                value = state.heightInchesText,
                onValueChange = viewModel::onHeightInchesChanged,
                label = stringResource(R.string.calc_height_inches),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        CompactTextField(
            value = state.heightText,
            onValueChange = viewModel::onHeightChanged,
            label = stringResource(R.string.calc_height),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(modifier = Modifier.height(ItemGap))
    CompactTextField(
        value = state.weightText,
        onValueChange = viewModel::onWeightChanged,
        label = stringResource(R.string.calc_weight_label, state.weightUnit.label()),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun WorkStep(
    state: GoalWizardUiState,
    viewModel: GoalWizardViewModel,
) {
    Text(stringResource(R.string.wizard_work_title), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(LabelGap))
    WorkActivity.entries.forEach { work ->
        WizardOption(
            selected = state.work == work,
            onClick = { viewModel.onWorkSelected(work) },
            label = work.label(),
            description = work.description(),
        )
    }
}

@Composable
private fun ExerciseStep(
    state: GoalWizardUiState,
    viewModel: GoalWizardViewModel,
) {
    Text(stringResource(R.string.wizard_exercise_title), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(LabelGap))
    ExerciseLevel.entries.forEach { exercise ->
        WizardOption(
            selected = state.exercise == exercise,
            onClick = { viewModel.onExerciseSelected(exercise) },
            label = exercise.label(),
            description = exercise.description(),
        )
    }
}

@Composable
private fun PlanStep(
    state: GoalWizardUiState,
    viewModel: GoalWizardViewModel,
) {
    Text(stringResource(R.string.wizard_plan_title), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(LabelGap))
    WeightPlan.entries.forEach { plan ->
        WizardOption(
            selected = state.plan == plan,
            onClick = { viewModel.onPlanSelected(plan) },
            label = plan.label(),
            description = plan.description(),
        )
    }
}

@Composable
private fun WizardOption(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    description: String,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(description) },
        leadingContent = {
            RadioButton(selected = selected, onClick = onClick)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider()
}

@Composable
private fun ResultStep(
    state: GoalWizardUiState,
    viewModel: GoalWizardViewModel,
) {
    val result = state.result ?: return
    Text(stringResource(R.string.wizard_result_title), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(ItemGap))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(ScreenPadding)) {
            ResultRow(stringResource(R.string.wizard_bmr_label), result.bmr)
            Spacer(modifier = Modifier.height(ItemGap))
            ResultRow(stringResource(R.string.wizard_tdee_label), result.tdee)
            Spacer(modifier = Modifier.height(ItemGap))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(ItemGap))
            Text(
                stringResource(R.string.wizard_target_label),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                stringResource(R.string.calc_result, result.targetCalories),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (result.raisedToMinimum) {
                Spacer(modifier = Modifier.height(LabelGap))
                Text(
                    stringResource(R.string.wizard_min_raised),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    state.currentGoalCalories?.let { current ->
        if (current != result.targetCalories) {
            Spacer(modifier = Modifier.height(ItemGap))
            Text(
                stringResource(R.string.wizard_replaces_goal, current, result.targetCalories),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(modifier = Modifier.height(ItemGap))
    Text(
        stringResource(R.string.wizard_macro_split_title),
        style = MaterialTheme.typography.labelMedium,
    )
    Spacer(modifier = Modifier.height(LabelGap))
    MacroSplitPicker(selected = state.macroSplit, onSelected = viewModel::onMacroSplitSelected)
    Spacer(modifier = Modifier.height(SectionGap))
    Button(onClick = viewModel::applyAsGoal, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.wizard_apply))
    }
    Spacer(modifier = Modifier.height(ItemGap))
    Text(
        stringResource(R.string.wizard_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Chips of common protein/fat/carbs calorie splits applied to the suggested goal. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MacroSplitPicker(
    selected: MacroSplit,
    onSelected: (MacroSplit) -> Unit,
) {
    FlowRow {
        MacroSplit.PRESETS.forEach { split ->
            FilterChip(
                selected = selected == split,
                onClick = { onSelected(split) },
                label = {
                    Text(
                        stringResource(
                            R.string.wizard_macro_split_chip,
                            split.proteinPercent,
                            split.fatPercent,
                            split.carbsPercent,
                        ),
                    )
                },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    kcal: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(R.string.kcal_amount, kcal),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun WorkActivity.label(): String =
    stringResource(
        when (this) {
            WorkActivity.SEDENTARY -> R.string.work_sedentary
            WorkActivity.STANDING -> R.string.work_standing
            WorkActivity.PHYSICAL -> R.string.work_physical
            WorkActivity.HEAVY -> R.string.work_heavy
        },
    )

@Composable
private fun WorkActivity.description(): String =
    stringResource(
        when (this) {
            WorkActivity.SEDENTARY -> R.string.work_sedentary_desc
            WorkActivity.STANDING -> R.string.work_standing_desc
            WorkActivity.PHYSICAL -> R.string.work_physical_desc
            WorkActivity.HEAVY -> R.string.work_heavy_desc
        },
    )

@Composable
private fun ExerciseLevel.label(): String =
    stringResource(
        when (this) {
            ExerciseLevel.NONE -> R.string.exercise_none
            ExerciseLevel.LIGHT -> R.string.exercise_light
            ExerciseLevel.MODERATE -> R.string.exercise_moderate
            ExerciseLevel.FREQUENT -> R.string.exercise_frequent
            ExerciseLevel.INTENSE -> R.string.exercise_intense
        },
    )

@Composable
private fun ExerciseLevel.description(): String =
    stringResource(
        when (this) {
            ExerciseLevel.NONE -> R.string.exercise_none_desc
            ExerciseLevel.LIGHT -> R.string.exercise_light_desc
            ExerciseLevel.MODERATE -> R.string.exercise_moderate_desc
            ExerciseLevel.FREQUENT -> R.string.exercise_frequent_desc
            ExerciseLevel.INTENSE -> R.string.exercise_intense_desc
        },
    )

@Composable
private fun WeightPlan.label(): String =
    stringResource(
        when (this) {
            WeightPlan.LOSE_STEADY -> R.string.plan_lose_steady
            WeightPlan.LOSE_FAST -> R.string.plan_lose_fast
            WeightPlan.MAINTAIN -> R.string.plan_maintain
            WeightPlan.GAIN_LEAN -> R.string.plan_gain_lean
            WeightPlan.GAIN_FAST -> R.string.plan_gain_fast
        },
    )

@Composable
private fun WeightPlan.description(): String =
    stringResource(
        when (this) {
            WeightPlan.LOSE_STEADY -> R.string.plan_lose_steady_desc
            WeightPlan.LOSE_FAST -> R.string.plan_lose_fast_desc
            WeightPlan.MAINTAIN -> R.string.plan_maintain_desc
            WeightPlan.GAIN_LEAN -> R.string.plan_gain_lean_desc
            WeightPlan.GAIN_FAST -> R.string.plan_gain_fast_desc
        },
    )
