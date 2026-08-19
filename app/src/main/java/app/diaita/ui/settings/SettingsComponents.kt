package app.diaita.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.diaita.R
import app.diaita.ui.theme.LabelGap
import app.diaita.ui.theme.ScreenPadding
import app.diaita.ui.theme.SectionGap

/** Scaffold shared by every settings sub-screen: top bar with back arrow, scrollable column. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSubScreen(
    titleRes: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
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
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = SectionGap),
            content = content,
        )
    }
}

/** Header above a group of related settings; rows keep their own dividers under it. */
@Composable
internal fun SettingsGroupHeader(titleRes: Int) {
    Text(
        stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier.padding(
                start = ScreenPadding,
                end = ScreenPadding,
                top = SectionGap,
                bottom = LabelGap,
            ),
    )
}

/**
 * A settings row with the current value on the right; tapping opens its editor dialog.
 * Every row type here closes with its own divider, so screens list rows and never repeat the
 * separator by hand.
 */
@Composable
internal fun ValueRow(
    titleRes: Int,
    value: String,
    onClick: () -> Unit,
    descriptionRes: Int? = null,
) {
    ListItem(
        headlineContent = { Text(stringResource(titleRes)) },
        supportingContent = descriptionRes?.let { { Text(stringResource(it)) } },
        trailingContent = {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider()
}

/** A settings row carrying a switch — the on/off half of every settings screen. */
@Composable
internal fun SwitchRow(
    titleRes: Int,
    descriptionRes: Int?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(titleRes)) },
        supportingContent = descriptionRes?.let { { Text(stringResource(it)) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
    )
    HorizontalDivider()
}

/** A settings row that opens another screen. */
@Composable
internal fun NavRow(
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

/** A settings row showing an HH:mm value; tapping opens the time picker dialog. */
@Composable
internal fun TimeRow(
    titleRes: Int,
    descriptionRes: Int?,
    minutes: Int,
    onClick: () -> Unit,
) {
    ValueRow(
        titleRes = titleRes,
        descriptionRes = descriptionRes,
        value = stringResource(R.string.time_value, minutes / 60, minutes % 60),
        onClick = onClick,
    )
}

internal data class ChoiceOption(
    val label: String,
    val selected: Boolean,
    val onSelect: () -> Unit,
)

/** Single-choice radio dialog; selecting an option applies it and closes the dialog. */
@Composable
internal fun ChoiceDialog(
    titleRes: Int,
    options: List<ChoiceOption>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = option.selected,
                                    onClick = {
                                        option.onSelect()
                                        onDismiss()
                                    },
                                ).padding(vertical = 8.dp),
                    ) {
                        RadioButton(selected = option.selected, onClick = null)
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text(option.label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsTimePickerDialog(
    titleRes: Int,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val state =
        rememberTimePickerState(
            initialHour = initialMinutes / 60,
            initialMinute = initialMinutes % 60,
            is24Hour =
                android.text.format.DateFormat
                    .is24HourFormat(LocalContext.current),
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
