package app.diaita.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.data.reminder.ReminderScheduler
import app.diaita.data.settings.CoachTone
import app.diaita.data.settings.DayPartOverThresholds
import app.diaita.data.settings.DayPartShares
import app.diaita.data.settings.MacroOverTolerances
import app.diaita.data.settings.MicronutrientOverTolerances
import app.diaita.data.settings.WeightUnit
import app.diaita.ui.AppViewModelProvider
import kotlin.math.roundToInt

/** Which time-of-day boundary the picker dialog is currently editing. */
private enum class BoundaryTarget { MORNING_END, DAY_END }

/** Which single-choice setting the radio dialog is currently editing. */
private enum class ChoiceTarget { WEIGHT_UNIT, COACH_TONE }

@Composable
fun TrackingSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val trackMacros by viewModel.trackMacros.collectAsStateWithLifecycle()
    val trackMicronutrients by viewModel.trackMicronutrients.collectAsStateWithLifecycle()
    val dayPartBoundaries by viewModel.dayPartBoundaries.collectAsStateWithLifecycle()
    val dayPartShares by viewModel.dayPartShares.collectAsStateWithLifecycle()
    val coachTone by viewModel.coachTone.collectAsStateWithLifecycle()
    val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()
    val dayPartRemindersEnabled by viewModel.dayPartRemindersEnabled.collectAsStateWithLifecycle()
    val macroOverTolerances by viewModel.macroOverTolerances.collectAsStateWithLifecycle()
    val microOverTolerances by viewModel.microOverTolerances.collectAsStateWithLifecycle()
    val athleticBmiBands by viewModel.athleticBmiBands.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var boundaryTarget by remember { mutableStateOf<BoundaryTarget?>(null) }
    var choiceTarget by remember { mutableStateOf<ChoiceTarget?>(null) }
    var showSharesDialog by remember { mutableStateOf(false) }
    var showMacroOverDialog by remember { mutableStateOf(false) }
    var showMicroOverDialog by remember { mutableStateOf(false) }
    var showDayPartOverDialog by remember { mutableStateOf(false) }
    var showWaterGlassDialog by remember { mutableStateOf(false) }
    val dayPartOverThresholds by viewModel.dayPartOverThresholds.collectAsStateWithLifecycle()
    val trackWater by viewModel.trackWater.collectAsStateWithLifecycle()
    val waterGlassMl by viewModel.waterGlassMl.collectAsStateWithLifecycle()

    SettingsSubScreen(
        titleRes = R.string.settings_section_tracking,
        onBack = onBack,
        modifier = modifier,
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_track_macros)) },
            supportingContent = { Text(stringResource(R.string.settings_track_macros_desc)) },
            trailingContent = {
                Switch(checked = trackMacros, onCheckedChange = viewModel::setTrackMacros)
            },
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_track_micros)) },
            supportingContent = { Text(stringResource(R.string.settings_track_micros_desc)) },
            trailingContent = {
                Switch(
                    checked = trackMicronutrients,
                    onCheckedChange = viewModel::setTrackMicronutrients,
                )
            },
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_track_water)) },
            supportingContent = { Text(stringResource(R.string.settings_track_water_desc)) },
            trailingContent = {
                Switch(checked = trackWater, onCheckedChange = viewModel::setTrackWater)
            },
        )
        HorizontalDivider()
        if (trackWater) {
            ValueRow(
                titleRes = R.string.settings_water_glass,
                descriptionRes = R.string.settings_water_glass_desc,
                value = stringResource(R.string.water_ml_value, waterGlassMl),
                onClick = { showWaterGlassDialog = true },
            )
            HorizontalDivider()
        }
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_athletic_bmi)) },
            supportingContent = { Text(stringResource(R.string.settings_athletic_bmi_desc)) },
            trailingContent = {
                Switch(checked = athleticBmiBands, onCheckedChange = viewModel::setAthleticBmiBands)
            },
        )
        HorizontalDivider()
        ValueRow(
            titleRes = R.string.settings_macro_over,
            descriptionRes = R.string.settings_macro_over_desc,
            value =
                stringResource(
                    R.string.macro_over_value,
                    macroOverTolerances.proteinPercent,
                    macroOverTolerances.fatPercent,
                    macroOverTolerances.carbsPercent,
                ),
            onClick = { showMacroOverDialog = true },
        )
        HorizontalDivider()
        ValueRow(
            titleRes = R.string.settings_micro_over,
            descriptionRes = R.string.settings_micro_over_desc,
            value =
                stringResource(
                    R.string.micro_over_value,
                    microOverTolerances.fiberPercent,
                    microOverTolerances.sugarPercent,
                    microOverTolerances.saltPercent,
                ),
            onClick = { showMicroOverDialog = true },
        )
        HorizontalDivider()
        ValueRow(
            titleRes = R.string.settings_weight_unit,
            value = weightUnit.fullLabel(),
            onClick = { choiceTarget = ChoiceTarget.WEIGHT_UNIT },
        )
        HorizontalDivider()
        ValueRow(
            titleRes = R.string.settings_coach,
            descriptionRes = R.string.settings_coach_desc,
            value = coachTone.label(),
            onClick = { choiceTarget = ChoiceTarget.COACH_TONE },
        )
        HorizontalDivider()
        TimeRow(
            titleRes = R.string.settings_morning_end,
            descriptionRes = R.string.settings_morning_end_desc,
            minutes = dayPartBoundaries.morningEndMinutes,
            onClick = { boundaryTarget = BoundaryTarget.MORNING_END },
        )
        HorizontalDivider()
        TimeRow(
            titleRes = R.string.settings_day_end,
            descriptionRes = R.string.settings_day_end_desc,
            minutes = dayPartBoundaries.dayEndMinutes,
            onClick = { boundaryTarget = BoundaryTarget.DAY_END },
        )
        HorizontalDivider()
        ValueRow(
            titleRes = R.string.settings_day_part_over,
            descriptionRes = R.string.settings_day_part_over_desc,
            value = stringResource(R.string.day_part_over_value, dayPartOverThresholds.amberPercent),
            onClick = { showDayPartOverDialog = true },
        )
        HorizontalDivider()
        ValueRow(
            titleRes = R.string.settings_day_part_shares,
            value =
                stringResource(
                    R.string.day_part_shares_value,
                    dayPartShares.morningPercent,
                    dayPartShares.dayPercent,
                    dayPartShares.eveningPercent,
                ),
            onClick = { showSharesDialog = true },
        )
        HorizontalDivider()
    }

    boundaryTarget?.let { target ->
        SettingsTimePickerDialog(
            titleRes =
                when (target) {
                    BoundaryTarget.MORNING_END -> R.string.settings_morning_end
                    BoundaryTarget.DAY_END -> R.string.settings_day_end
                },
            initialMinutes =
                when (target) {
                    BoundaryTarget.MORNING_END -> dayPartBoundaries.morningEndMinutes
                    BoundaryTarget.DAY_END -> dayPartBoundaries.dayEndMinutes
                },
            onDismiss = { boundaryTarget = null },
            onConfirm = { minutes ->
                val updated =
                    when (target) {
                        BoundaryTarget.MORNING_END -> viewModel.setMorningEndMinutes(minutes)
                        BoundaryTarget.DAY_END -> viewModel.setDayEndMinutes(minutes)
                    }
                // Day-part reminders fire exactly at these boundaries — follow the change.
                if (dayPartRemindersEnabled) {
                    ReminderScheduler.scheduleDayPartReminders(context, updated)
                }
                boundaryTarget = null
            },
        )
    }

    when (choiceTarget) {
        ChoiceTarget.WEIGHT_UNIT ->
            ChoiceDialog(
                titleRes = R.string.settings_weight_unit,
                options =
                    WeightUnit.entries.map { unit ->
                        ChoiceOption(
                            label = unit.fullLabel(),
                            selected = unit == weightUnit,
                            onSelect = { viewModel.setWeightUnit(unit) },
                        )
                    },
                onDismiss = { choiceTarget = null },
            )
        ChoiceTarget.COACH_TONE ->
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
                onDismiss = { choiceTarget = null },
            )
        null -> Unit
    }

    if (showSharesDialog) {
        DayPartSharesDialog(
            current = dayPartShares,
            onDismiss = { showSharesDialog = false },
            onConfirm = { morning, day ->
                viewModel.setDayPartShares(morning, day)
                showSharesDialog = false
            },
        )
    }

    if (showMacroOverDialog) {
        MacroOverDialog(
            current = macroOverTolerances,
            onDismiss = { showMacroOverDialog = false },
            onConfirm = { protein, fat, carbs ->
                viewModel.setMacroOverTolerances(protein, fat, carbs)
                showMacroOverDialog = false
            },
        )
    }

    if (showWaterGlassDialog) {
        WaterGlassDialog(
            currentMl = waterGlassMl,
            onDismiss = { showWaterGlassDialog = false },
            onConfirm = { ml ->
                viewModel.setWaterGlassMl(ml)
                showWaterGlassDialog = false
            },
        )
    }

    if (showDayPartOverDialog) {
        DayPartOverDialog(
            current = dayPartOverThresholds,
            onDismiss = { showDayPartOverDialog = false },
            onConfirm = { amber ->
                viewModel.setDayPartOverThresholds(amber)
                showDayPartOverDialog = false
            },
        )
    }

    if (showMicroOverDialog) {
        MicroOverDialog(
            current = microOverTolerances,
            onDismiss = { showMicroOverDialog = false },
            onConfirm = { fiber, sugar, salt ->
                viewModel.setMicroOverTolerances(fiber, sugar, salt)
                showMicroOverDialog = false
            },
        )
    }
}

@Composable
private fun WeightUnit.fullLabel(): String =
    stringResource(if (this == WeightUnit.KG) R.string.unit_kg_full else R.string.unit_lb_full)

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

/**
 * Three sliders for the per-macro overshoot tolerance. The defaults are asymmetric on
 * purpose (protein overshoot is benign, fat is the tightest calorie budget, carbs sit in
 * between) — see [MacroOverTolerances] for the nutrition reasoning.
 */
@Composable
private fun MacroOverDialog(
    current: MacroOverTolerances,
    onDismiss: () -> Unit,
    onConfirm: (proteinPercent: Int, fatPercent: Int, carbsPercent: Int) -> Unit,
) {
    var protein by remember { mutableIntStateOf(current.proteinPercent) }
    var fat by remember { mutableIntStateOf(current.fatPercent) }
    var carbs by remember { mutableIntStateOf(current.carbsPercent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_macro_over)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.macro_over_dialog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.macro_over_protein, protein))
                Slider(
                    value = protein.toFloat(),
                    onValueChange = { protein = it.roundToInt() },
                    valueRange = 0f..MacroOverTolerances.MAX_PERCENT.toFloat(),
                )
                Text(stringResource(R.string.macro_over_fat, fat))
                Slider(
                    value = fat.toFloat(),
                    onValueChange = { fat = it.roundToInt() },
                    valueRange = 0f..MacroOverTolerances.MAX_PERCENT.toFloat(),
                )
                Text(stringResource(R.string.macro_over_carbs, carbs))
                Slider(
                    value = carbs.toFloat(),
                    onValueChange = { carbs = it.roundToInt() },
                    valueRange = 0f..MacroOverTolerances.MAX_PERCENT.toFloat(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(protein, fat, carbs) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/**
 * Three sliders for the slack past the fixed fiber/sugar/salt daily limits (70/50/5 g)
 * before the totals line turns red. The hint names the honesty caveats: the app tracks
 * TOTAL sugars while the 50 g norm is about free sugars, and salt counts only what the
 * food itself carries — see [MicronutrientOverTolerances] for the default reasoning.
 */
@Composable
private fun MicroOverDialog(
    current: MicronutrientOverTolerances,
    onDismiss: () -> Unit,
    onConfirm: (fiberPercent: Int, sugarPercent: Int, saltPercent: Int) -> Unit,
) {
    var fiber by remember { mutableIntStateOf(current.fiberPercent) }
    var sugar by remember { mutableIntStateOf(current.sugarPercent) }
    var salt by remember { mutableIntStateOf(current.saltPercent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_micro_over)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.micro_over_dialog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.micro_over_fiber, fiber))
                Slider(
                    value = fiber.toFloat(),
                    onValueChange = { fiber = it.roundToInt() },
                    valueRange = 0f..MicronutrientOverTolerances.MAX_PERCENT.toFloat(),
                )
                Text(stringResource(R.string.micro_over_sugar, sugar))
                Slider(
                    value = sugar.toFloat(),
                    onValueChange = { sugar = it.roundToInt() },
                    valueRange = 0f..MicronutrientOverTolerances.MAX_PERCENT.toFloat(),
                )
                Text(stringResource(R.string.micro_over_salt, salt))
                Slider(
                    value = salt.toFloat(),
                    onValueChange = { salt = it.roundToInt() },
                    valueRange = 0f..MicronutrientOverTolerances.MAX_PERCENT.toFloat(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(fiber, sugar, salt) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** One slider, 50-500 ml in 50 ml steps: how much one tap of the water button logs. */
@Composable
private fun WaterGlassDialog(
    currentMl: Int,
    onDismiss: () -> Unit,
    onConfirm: (ml: Int) -> Unit,
) {
    var ml by remember { mutableIntStateOf(currentMl) }
    val range = SettingsViewModel.WATER_GLASS_RANGE_ML

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_water_glass)) },
        text = {
            Column {
                Text(stringResource(R.string.water_ml_value, ml))
                Slider(
                    value = ml.toFloat(),
                    onValueChange = { ml = (it / 50).roundToInt() * 50 },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(ml) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/**
 * One slider for the amber slack past a morning/day/evening target. Red is not a threshold
 * of its own: a segment goes red only when the day total is over too (see [dayPartOverBand]).
 */
@Composable
private fun DayPartOverDialog(
    current: DayPartOverThresholds,
    onDismiss: () -> Unit,
    onConfirm: (amberPercent: Int) -> Unit,
) {
    var amber by remember { mutableIntStateOf(current.amberPercent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_day_part_over)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.day_part_over_dialog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.day_part_over_amber, amber))
                Slider(
                    value = amber.toFloat(),
                    onValueChange = { amber = it.roundToInt() },
                    valueRange = 0f..DayPartOverThresholds.MAX_PERCENT.toFloat(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(amber) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Two sliders for morning/day percent; evening is derived so the three always sum to 100. */
@Composable
private fun DayPartSharesDialog(
    current: DayPartShares,
    onDismiss: () -> Unit,
    onConfirm: (morningPercent: Int, dayPercent: Int) -> Unit,
) {
    var morning by remember { mutableIntStateOf(current.morningPercent) }
    var day by remember { mutableIntStateOf(current.dayPercent) }
    val sanitized = DayPartShares.sanitized(morning, day)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_day_part_shares)) },
        text = {
            Column {
                Text(stringResource(R.string.share_morning, sanitized.morningPercent))
                Slider(
                    value = sanitized.morningPercent.toFloat(),
                    onValueChange = { morning = it.roundToInt() },
                    valueRange = 10f..80f,
                )
                Text(stringResource(R.string.share_day, sanitized.dayPercent))
                Slider(
                    value = sanitized.dayPercent.toFloat(),
                    onValueChange = { day = it.roundToInt() },
                    valueRange = 10f..80f,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.share_evening, sanitized.eveningPercent),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sanitized.morningPercent, sanitized.dayPercent) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
