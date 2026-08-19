package app.diaita.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.data.settings.SettingsRepository
import app.diaita.ui.AppViewModelProvider
import kotlin.math.roundToInt

@Composable
fun DataSettingsScreen(
    onBack: () -> Unit,
    onBackupClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val openFoodFactsEnabled by viewModel.openFoodFactsEnabled.collectAsStateWithLifecycle()
    val recentFoodsCount by viewModel.recentFoodsCount.collectAsStateWithLifecycle()
    val retentionDays by viewModel.retentionDays.collectAsStateWithLifecycle()
    var showRecentCountDialog by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }
    var pendingRetentionDays by remember { mutableStateOf<Int?>(null) }

    SettingsSubScreen(
        titleRes = R.string.settings_section_data,
        onBack = onBack,
        modifier = modifier,
    ) {
        SettingsGroupHeader(R.string.settings_group_food_search)
        ListItem(
            headlineContent = { Text(stringResource(R.string.search_off_checkbox)) },
            supportingContent = { Text(stringResource(R.string.settings_off_desc)) },
            trailingContent = {
                Switch(checked = openFoodFactsEnabled, onCheckedChange = viewModel::setOpenFoodFactsEnabled)
            },
        )
        ValueRow(
            titleRes = R.string.settings_recent_foods,
            descriptionRes = R.string.settings_recent_foods_desc,
            value = recentFoodsCount.toString(),
            onClick = { showRecentCountDialog = true },
        )

        SettingsGroupHeader(R.string.settings_group_history)
        ValueRow(
            titleRes = R.string.settings_retention,
            descriptionRes = R.string.settings_retention_desc,
            value = retentionLabel(retentionDays),
            onClick = { showRetentionDialog = true },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.backup_title)) },
            supportingContent = { Text(stringResource(R.string.settings_backup_desc)) },
            trailingContent = {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onBackupClick),
        )
    }

    if (showRecentCountDialog) {
        RecentFoodsCountDialog(
            current = recentFoodsCount,
            onDismiss = { showRecentCountDialog = false },
            onConfirm = { count ->
                viewModel.setRecentFoodsCount(count)
                showRecentCountDialog = false
            },
        )
    }

    if (showRetentionDialog) {
        ChoiceDialog(
            titleRes = R.string.settings_retention,
            options =
                SettingsViewModel.RETENTION_OPTIONS_DAYS.map { days ->
                    ChoiceOption(
                        label = retentionLabel(days),
                        selected = days == retentionDays,
                        onSelect = {
                            if (retentionShrinks(current = retentionDays, new = days)) {
                                pendingRetentionDays = days
                            } else {
                                viewModel.setRetentionDays(days)
                            }
                        },
                    )
                },
            onDismiss = { showRetentionDialog = false },
        )
    }

    // Reducing retention irreversibly folds the now-out-of-window entries on next app
    // start, so it never happens straight from the radio tap.
    pendingRetentionDays?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingRetentionDays = null },
            title = { Text(stringResource(R.string.settings_retention)) },
            text = { Text(stringResource(R.string.retention_reduce_warning, retentionLabel(pending))) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setRetentionDays(pending)
                        pendingRetentionDays = null
                    },
                ) { Text(stringResource(R.string.retention_reduce_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRetentionDays = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/** True when switching to [new] would fold history that the [current] setting still keeps. */
private fun retentionShrinks(
    current: Int,
    new: Int,
): Boolean {
    if (new == SettingsRepository.RETENTION_FOREVER) return false
    return current == SettingsRepository.RETENTION_FOREVER || new < current
}

@Composable
private fun retentionLabel(days: Int): String =
    stringResource(
        when (days) {
            SettingsRepository.RETENTION_FOREVER -> R.string.retention_forever
            90 -> R.string.retention_3_months
            180 -> R.string.retention_6_months
            730 -> R.string.retention_2_years
            else -> R.string.retention_1_year
        },
    )

@Composable
private fun RecentFoodsCountDialog(
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var count by remember { mutableIntStateOf(current) }
    val range = SettingsViewModel.RECENT_FOODS_RANGE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_recent_foods)) },
        text = {
            Column {
                Text(count.toString(), style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = count.toFloat(),
                    onValueChange = { count = it.roundToInt() },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                    steps = range.last - range.first - 1,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(count) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
