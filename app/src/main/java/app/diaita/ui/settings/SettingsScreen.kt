package app.diaita.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.diaita.R

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
                .padding(vertical = 16.dp),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        CategoryRow(
            titleRes = R.string.settings_goals,
            descriptionRes = R.string.settings_goals_desc,
            onClick = onGoalsClick,
        )
        CategoryRow(
            titleRes = R.string.settings_wizard,
            descriptionRes = R.string.settings_wizard_desc,
            onClick = onWizardClick,
        )
        CategoryRow(
            titleRes = R.string.settings_section_appearance,
            descriptionRes = R.string.settings_appearance_desc,
            onClick = onAppearanceClick,
        )
        CategoryRow(
            titleRes = R.string.settings_section_tracking,
            descriptionRes = R.string.settings_tracking_desc,
            onClick = onTrackingClick,
        )
        CategoryRow(
            titleRes = R.string.settings_section_notifications,
            descriptionRes = R.string.settings_notifications_desc,
            onClick = onNotificationsClick,
        )
        CategoryRow(
            titleRes = R.string.settings_section_data,
            descriptionRes = R.string.settings_data_desc,
            onClick = onDataClick,
        )
        CategoryRow(
            titleRes = R.string.nav_about,
            descriptionRes = R.string.settings_about_desc,
            onClick = onAboutClick,
        )
    }
}

@Composable
private fun CategoryRow(
    titleRes: Int,
    descriptionRes: Int,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(titleRes)) },
        supportingContent = { Text(stringResource(descriptionRes)) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider()
}
