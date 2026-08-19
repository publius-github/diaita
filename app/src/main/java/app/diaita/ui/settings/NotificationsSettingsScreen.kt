package app.diaita.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.data.reminder.ReminderScheduler
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.theme.ItemGap
import app.diaita.ui.theme.ScreenPadding

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
    val eveningReminderEnabled by viewModel.eveningReminderEnabled.collectAsStateWithLifecycle()
    val dayPartBoundaries by viewModel.dayPartBoundaries.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var timeTarget by remember { mutableStateOf<ReminderTimeTarget?>(null) }

    fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    // Tracked so a denial is called out loudly on screen — a toggle that stays on while
    // nothing can fire is a silent lie. Re-checked on every resume, since the user may
    // grant/revoke the permission in system settings and come back.
    var notificationsAllowed by remember { mutableStateOf(hasNotificationPermission()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) notificationsAllowed = hasNotificationPermission()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted -> notificationsAllowed = granted }

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

    val anyReminderEnabled =
        reminderEnabled || weightReminderEnabled || dayPartRemindersEnabled || eveningReminderEnabled

    SettingsSubScreen(
        titleRes = R.string.settings_section_notifications,
        onBack = onBack,
        modifier = modifier,
    ) {
        if (anyReminderEnabled && !notificationsAllowed) {
            Text(
                stringResource(R.string.notifications_permission_missing),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = ScreenPadding, vertical = ItemGap),
            )
            HorizontalDivider()
        }
        SettingsGroupHeader(R.string.settings_group_log_reminders)
        SwitchRow(
            titleRes = R.string.settings_reminder,
            descriptionRes = R.string.settings_reminder_desc,
            checked = reminderEnabled,
            onCheckedChange = ::onReminderToggled,
        )
        if (reminderEnabled) {
            TimeRow(
                titleRes = R.string.settings_reminder_time,
                descriptionRes = null,
                minutes = reminderTimeMinutes,
                onClick = { timeTarget = ReminderTimeTarget.FOOD },
            )
        }
        SwitchRow(
            titleRes = R.string.settings_weight_reminder,
            descriptionRes = R.string.settings_weight_reminder_desc,
            checked = weightReminderEnabled,
            onCheckedChange = ::onWeightReminderToggled,
        )
        if (weightReminderEnabled) {
            TimeRow(
                titleRes = R.string.settings_reminder_time,
                descriptionRes = null,
                minutes = weightReminderTimeMinutes,
                onClick = { timeTarget = ReminderTimeTarget.WEIGHT },
            )
        }

        // These two fire off the day-part boundaries rather than a time of their own, which
        // is why they are a group apart — their schedule lives in Tracking > Day parts.
        SettingsGroupHeader(R.string.settings_group_day_reminders)
        SwitchRow(
            titleRes = R.string.settings_day_part_reminder,
            descriptionRes = R.string.settings_day_part_reminder_desc,
            checked = dayPartRemindersEnabled,
            onCheckedChange = ::onDayPartRemindersToggled,
        )
        SwitchRow(
            titleRes = R.string.settings_evening_reminder,
            descriptionRes = R.string.settings_evening_reminder_desc,
            checked = eveningReminderEnabled,
            onCheckedChange = { enabled ->
                viewModel.setEveningReminderEnabled(enabled)
                if (enabled) {
                    requestNotificationPermission()
                    ReminderScheduler.scheduleEveningReminder(context)
                } else {
                    ReminderScheduler.cancelEveningReminder(context)
                }
            },
        )
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
