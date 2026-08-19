package app.diaita.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.data.settings.WeightUnit
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.goals.GoalWizardScreen
import app.diaita.ui.theme.ItemGap
import app.diaita.ui.theme.ScreenPadding
import app.diaita.ui.theme.SectionGap

/**
 * First-run flow shown instead of the main nav graph: a welcome step (privacy pitch +
 * weight-unit choice), then the regular goal wizard. Every exit — skip, the wizard's back
 * arrow, or a successfully applied goal — stamps the onboarding-done flag; MainActivity
 * watches that flag and swaps in the main UI.
 */
@Composable
fun OnboardingFlow(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    var showWizard by rememberSaveable { mutableStateOf(false) }

    if (showWizard) {
        GoalWizardScreen(onDone = viewModel::finish, modifier = modifier)
    } else {
        WelcomeStep(
            viewModel = viewModel,
            onSetUpGoal = { showWizard = true },
            onSkip = viewModel::finish,
            modifier = modifier,
        )
    }
}

@Composable
private fun WelcomeStep(
    viewModel: OnboardingViewModel,
    onSetUpGoal: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.onboarding_welcome_title, stringResource(R.string.app_name)),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(ItemGap))
        Text(
            stringResource(R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(SectionGap))
        Text(
            stringResource(R.string.onboarding_units_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(ItemGap))
        Row(horizontalArrangement = Arrangement.spacedBy(ItemGap)) {
            FilterChip(
                selected = weightUnit == WeightUnit.KG,
                onClick = { viewModel.setWeightUnit(WeightUnit.KG) },
                label = { Text(stringResource(R.string.unit_kg_full)) },
            )
            FilterChip(
                selected = weightUnit == WeightUnit.LB,
                onClick = { viewModel.setWeightUnit(WeightUnit.LB) },
                label = { Text(stringResource(R.string.unit_lb_full)) },
            )
        }
        Spacer(modifier = Modifier.height(SectionGap))
        Button(onClick = onSetUpGoal, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_set_goal))
        }
        Spacer(modifier = Modifier.height(ItemGap))
        TextButton(onClick = onSkip) {
            Text(stringResource(R.string.onboarding_skip))
        }
    }
}
