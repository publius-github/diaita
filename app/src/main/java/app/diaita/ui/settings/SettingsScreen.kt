package app.diaita.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.diaita.R
import app.diaita.ui.theme.ScreenPadding

/**
 * Settings root: a short list of categories, each opening its own sub-screen — the toggles
 * themselves live one level down so this screen never becomes a wall of switches.
 */
@Composable
fun SettingsScreen(
    onGoalsClick: () -> Unit,
    onWizardClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onTrackingClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onDataClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = ScreenPadding),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = ScreenPadding),
        )
        SettingsGroupHeader(R.string.settings_group_goals)
        NavRow(
            titleRes = R.string.settings_goals,
            descriptionRes = R.string.settings_goals_desc,
            onClick = onGoalsClick,
        )
        NavRow(
            titleRes = R.string.settings_wizard,
            descriptionRes = R.string.settings_wizard_desc,
            onClick = onWizardClick,
        )

        SettingsGroupHeader(R.string.settings_group_app)
        NavRow(
            titleRes = R.string.settings_section_appearance,
            descriptionRes = R.string.settings_appearance_desc,
            onClick = onAppearanceClick,
        )
        NavRow(
            titleRes = R.string.settings_section_tracking,
            descriptionRes = R.string.settings_tracking_desc,
            onClick = onTrackingClick,
        )
        NavRow(
            titleRes = R.string.settings_section_notifications,
            descriptionRes = R.string.settings_notifications_desc,
            onClick = onNotificationsClick,
        )
        NavRow(
            titleRes = R.string.settings_section_data,
            descriptionRes = R.string.settings_data_desc,
            onClick = onDataClick,
        )
        NavRow(
            titleRes = R.string.nav_about,
            descriptionRes = R.string.settings_about_desc,
            onClick = onAboutClick,
        )
    }
}
