package app.diaita.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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

    SettingsSubScreen(
        titleRes = R.string.settings_section_data,
        onBack = onBack,
        modifier = modifier,
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.search_off_checkbox)) },
            supportingContent = { Text(stringResource(R.string.settings_off_desc)) },
            trailingContent = {
                Switch(checked = openFoodFactsEnabled, onCheckedChange = viewModel::setOpenFoodFactsEnabled)
            },
        )
        HorizontalDivider()
        ValueRow(
            titleRes = R.string.settings_recent_foods,
            descriptionRes = R.string.settings_recent_foods_desc,
            value = recentFoodsCount.toString(),
            onClick = { showRecentCountDialog = true },
        )
        HorizontalDivider()
        ValueRow(
            titleRes = R.string.settings_retention,
            descriptionRes = R.string.settings_retention_desc,
            value = retentionLabel(retentionDays),
            onClick = { showRetentionDialog = true },
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
                        onSelect = { viewModel.setRetentionDays(days) },
                    )
                },
            onDismiss = { showRetentionDialog = false },
        )
    }
}

@Composable
private fun retentionLabel(days: Int): String =
    stringResource(
        when (days) {
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
