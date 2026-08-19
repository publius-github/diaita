package app.diaita.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.data.settings.CoachTone
import app.diaita.data.settings.HomeProgressStyle
import app.diaita.data.settings.ThemeMode
import app.diaita.ui.AppViewModelProvider

@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val homeProgressStyle by viewModel.homeProgressStyle.collectAsStateWithLifecycle()
    val coachTone by viewModel.coachTone.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showProgressStyleDialog by remember { mutableStateOf(false) }
    var showCoachDialog by remember { mutableStateOf(false) }

    SettingsSubScreen(
        titleRes = R.string.settings_section_appearance,
        onBack = onBack,
        modifier = modifier,
    ) {
        SettingsGroupHeader(R.string.settings_group_theme)
        ValueRow(
            titleRes = R.string.settings_theme,
            value = themeMode.label(),
            onClick = { showThemeDialog = true },
        )
        SwitchRow(
            titleRes = R.string.settings_dynamic_color,
            descriptionRes = R.string.settings_dynamic_color_desc,
            checked = dynamicColor,
            onCheckedChange = viewModel::setDynamicColor,
        )

        // Both of these decide what the Home card shows, so they sit together here rather
        // than one screen apart (the coach used to live under Tracking).
        SettingsGroupHeader(R.string.settings_group_home)
        ValueRow(
            titleRes = R.string.settings_home_progress_style,
            descriptionRes = R.string.settings_home_progress_style_desc,
            value = homeProgressStyle.label(),
            onClick = { showProgressStyleDialog = true },
        )
        ValueRow(
            titleRes = R.string.settings_coach,
            descriptionRes = R.string.settings_coach_desc,
            value = coachTone.label(),
            onClick = { showCoachDialog = true },
        )
    }

    if (showCoachDialog) {
        ChoiceDialog(
            titleRes = R.string.settings_coach,
            options =
                CoachTone.entries.map { tone ->
                    ChoiceOption(
                        label = tone.label(),
                        selected = tone == coachTone,
                        onSelect = { viewModel.setCoachTone(tone) },
                    )
                },
            onDismiss = { showCoachDialog = false },
        )
    }

    if (showThemeDialog) {
        ChoiceDialog(
            titleRes = R.string.settings_theme,
            options =
                ThemeMode.entries.map { mode ->
                    ChoiceOption(
                        label = mode.label(),
                        selected = mode == themeMode,
                        onSelect = { viewModel.setThemeMode(mode) },
                    )
                },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showProgressStyleDialog) {
        ChoiceDialog(
            titleRes = R.string.settings_home_progress_style,
            options =
                HomeProgressStyle.entries.map { style ->
                    ChoiceOption(
                        label = style.label(),
                        selected = style == homeProgressStyle,
                        onSelect = { viewModel.setHomeProgressStyle(style) },
                    )
                },
            onDismiss = { showProgressStyleDialog = false },
        )
    }
}

@Composable
private fun ThemeMode.label(): String =
    stringResource(
        when (this) {
            ThemeMode.SYSTEM -> R.string.theme_system
            ThemeMode.LIGHT -> R.string.theme_light
            ThemeMode.DARK -> R.string.theme_dark
        },
    )

@Composable
private fun HomeProgressStyle.label(): String =
    stringResource(
        when (this) {
            HomeProgressStyle.RING -> R.string.progress_style_ring
            HomeProgressStyle.COMPACT_RING -> R.string.progress_style_compact_ring
            HomeProgressStyle.GAUGE -> R.string.progress_style_gauge
            HomeProgressStyle.BAR -> R.string.progress_style_bar
        },
    )

@Composable
private fun CoachTone.label(): String =
    stringResource(
        when (this) {
            CoachTone.OFF -> R.string.coach_off
            CoachTone.GENTLE -> R.string.coach_gentle
            CoachTone.SNARKY -> R.string.coach_snarky
            CoachTone.BRUTAL -> R.string.coach_brutal
        },
    )
