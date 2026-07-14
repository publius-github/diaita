package app.diaita.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.data.reminder.ReminderScheduler
import app.diaita.ui.AppViewModelProvider

/** Which reminder's time the picker dialog is currently editing. */
private enum class ReminderTimeTarget { FOOD, WEIGHT }

@Composable
fun NotificationsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val reminderTimeMinutes by viewModel.reminderTimeMinutes.collectAsStateWithLifecycle()
    val weightReminderEnabled by viewModel.weightReminderEnabled.collectAsStateWithLifecycle()
    val weightReminderTimeMinutes by viewModel.weightReminderTimeMinutes.collectAsStateWithLifecycle()
    val dayPartRemindersEnabled by viewModel.dayPartRemindersEnabled.collectAsStateWithLifecycle()
    val dayPartBoundaries by viewModel.dayPartBoundaries.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var timeTarget by remember { mutableStateOf<ReminderTimeTarget?>(null) }

    // The permission result never disables the toggle — a denied permission just means the
    // worker stays silent; the user can grant it later from system settings.
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun onReminderToggled(enabled: Boolean) {
        viewModel.setReminderEnabled(enabled)
        if (enabled) {
            requestNotificationPermission()
            ReminderScheduler.scheduleFoodReminder(context, reminderTimeMinutes)
        } else {
            ReminderScheduler.cancelFoodReminder(context)
        }
    }

    fun onWeightReminderToggled(enabled: Boolean) {
        viewModel.setWeightReminderEnabled(enabled)
        if (enabled) {
            requestNotificationPermission()
            ReminderScheduler.scheduleWeightReminder(context, weightReminderTimeMinutes)
        } else {
            ReminderScheduler.cancelWeightReminder(context)
        }
    }

    fun onDayPartRemindersToggled(enabled: Boolean) {
        viewModel.setDayPartRemindersEnabled(enabled)
        if (enabled) {
            requestNotificationPermission()
            ReminderScheduler.scheduleDayPartReminders(context, dayPartBoundaries)
        } else {
            ReminderScheduler.cancelDayPartReminders(context)
        }
    }

    SettingsSubScreen(
        titleRes = R.string.settings_section_notifications,
        onBack = onBack,
        modifier = modifier,
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_reminder)) },
            supportingContent = { Text(stringResource(R.string.settings_reminder_desc)) },
            trailingContent = {
                Switch(checked = reminderEnabled, onCheckedChange = ::onReminderToggled)
            },
        )
        if (reminderEnabled) {
            TimeRow(
                titleRes = R.string.settings_reminder_time,
                descriptionRes = null,
                minutes = reminderTimeMinutes,
                onClick = { timeTarget = ReminderTimeTarget.FOOD },
            )
        }
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_weight_reminder)) },
            supportingContent = { Text(stringResource(R.string.settings_weight_reminder_desc)) },
            trailingContent = {
                Switch(checked = weightReminderEnabled, onCheckedChange = ::onWeightReminderToggled)
            },
        )
        if (weightReminderEnabled) {
            TimeRow(
                titleRes = R.string.settings_reminder_time,
                descriptionRes = null,
                minutes = weightReminderTimeMinutes,
                onClick = { timeTarget = ReminderTimeTarget.WEIGHT },
            )
        }
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_day_part_reminder)) },
            supportingContent = { Text(stringResource(R.string.settings_day_part_reminder_desc)) },
            trailingContent = {
                Switch(checked = dayPartRemindersEnabled, onCheckedChange = ::onDayPartRemindersToggled)
            },
        )
        HorizontalDivider()
    }

    timeTarget?.let { target ->
        SettingsTimePickerDialog(
            titleRes = R.string.settings_reminder_time,
            initialMinutes =
                when (target) {
                    ReminderTimeTarget.FOOD -> reminderTimeMinutes
                    ReminderTimeTarget.WEIGHT -> weightReminderTimeMinutes
                },
            onDismiss = { timeTarget = null },
            onConfirm = { minutes ->
                when (target) {
                    ReminderTimeTarget.FOOD -> {
                        viewModel.setReminderTimeMinutes(minutes)
                        if (reminderEnabled) ReminderScheduler.scheduleFoodReminder(context, minutes)
                    }
                    ReminderTimeTarget.WEIGHT -> {
                        viewModel.setWeightReminderTimeMinutes(minutes)
                        if (weightReminderEnabled) ReminderScheduler.scheduleWeightReminder(context, minutes)
                    }
                }
                timeTarget = null
            },
        )
    }
}
