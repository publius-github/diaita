package app.diaita.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.data.reminder.ReminderScheduler
import app.diaita.ui.AppViewModelProvider

@Composable
fun SettingsScreen(
    onGoalsClick: () -> Unit,
    onBackupClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val openFoodFactsEnabled by viewModel.openFoodFactsEnabled.collectAsStateWithLifecycle()
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The permission result never disables the toggle — a denied permission just means the
    // worker stays silent; the user can grant it later from system settings.
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { }

    fun onReminderToggled(enabled: Boolean) {
        viewModel.setReminderEnabled(enabled)
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            ReminderScheduler.schedule(context)
        } else {
            ReminderScheduler.cancel(context)
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_goals)) },
            supportingContent = { Text(stringResource(R.string.settings_goals_desc)) },
            trailingContent = {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onGoalsClick),
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.backup_title)) },
            supportingContent = { Text(stringResource(R.string.settings_backup_desc)) },
            trailingContent = {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onBackupClick),
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.search_off_checkbox)) },
            supportingContent = { Text(stringResource(R.string.settings_off_desc)) },
            trailingContent = {
                Switch(
                    checked = openFoodFactsEnabled,
                    onCheckedChange = viewModel::setOpenFoodFactsEnabled,
                )
            },
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_reminder)) },
            supportingContent = { Text(stringResource(R.string.settings_reminder_desc)) },
            trailingContent = {
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = ::onReminderToggled,
                )
            },
        )
        HorizontalDivider()
    }
}
