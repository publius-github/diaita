package app.diaita.ui.presets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.repository.PresetSummary
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.common.CompactRowCard

private const val TAB_DISHES = 0
private const val TAB_CUSTOM = 1

@Composable
fun PresetListScreen(
    onAddPresetClick: () -> Unit,
    onEditPresetClick: (Long) -> Unit,
    onPresetLogged: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PresetViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val justLogged by viewModel.justLogged.collectAsStateWithLifecycle()
    val dishPendingPortion by viewModel.dishPendingPortion.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.undo)
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_DISHES) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(justLogged) {
        if (justLogged) {
            viewModel.consumeJustLogged()
            onPresetLogged()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.undoEvent.collect { presetWithItems ->
            val result =
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.deleted_named, presetWithItems.preset.name),
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short,
                )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDeletePreset(presetWithItems)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val (dishes, custom) = presets.partition { it.preset.cookedWeightGrams != null }
        val tabPresets = if (selectedTab == TAB_DISHES) dishes else custom
        val query = searchQuery.trim()
        val shownPresets =
            if (query.isEmpty()) {
                tabPresets
            } else {
                tabPresets.filter { it.preset.name.contains(query, ignoreCase = true) }
            }

        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == TAB_DISHES,
                    onClick = { selectedTab = TAB_DISHES },
                    text = { Text(stringResource(R.string.tab_dishes)) },
                )
                Tab(
                    selected = selectedTab == TAB_CUSTOM,
                    onClick = { selectedTab = TAB_CUSTOM },
                    text = { Text(stringResource(R.string.tab_custom_presets)) },
                )
            }
            // Search plus Add sit under the tabs, not at the bottom: at the bottom the
            // keyboard pushed the whole screen up every time the field took focus.
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.preset_search_hint)) },
                    trailingIcon =
                        if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.clear_search),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                )
                Button(onClick = onAddPresetClick) {
                    Text(stringResource(R.string.add))
                }
            }
            if (shownPresets.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(
                            when {
                                query.isNotEmpty() -> R.string.preset_search_no_matches
                                selectedTab == TAB_DISHES -> R.string.no_dishes_yet
                                else -> R.string.no_presets_yet
                            },
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val sections = groupPresetsBySlot(shownPresets)
                    // Seeded dishes carry no meal slot, so a dishes-only list would show a
                    // single meaningless "Other" header — drop it and render a flat list.
                    val loneUnspecified = sections.size == 1 && sections.single().slot == MealSlot.UNSPECIFIED
                    sections.forEach { section ->
                        if (!loneUnspecified) {
                            item(key = "header_${section.slot}") { PresetSectionHeader(section) }
                        }
                        items(section.presets, key = { it.preset.id }) { summary ->
                            PresetCard(
                                summary = summary,
                                onClick = { onEditPresetClick(summary.preset.id) },
                                onLog = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (summary.preset.cookedWeightGrams != null) {
                                        viewModel.requestDishPortion(summary)
                                    } else {
                                        viewModel.logPreset(summary.preset.id)
                                    }
                                },
                                onDelete = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.deletePreset(summary.preset)
                                },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }

    dishPendingPortion?.let { dish ->
        DishPortionSheet(
            dish = dish,
            onDismiss = viewModel::dismissDishPortion,
            onConfirm = { eatenGrams ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.confirmDishPortion(eatenGrams)
            },
        )
    }
}

@Composable
private fun PresetCard(
    summary: PresetSummary,
    onClick: () -> Unit,
    onLog: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cookedWeight = summary.preset.cookedWeightGrams
    val subtitle =
        if (cookedWeight != null) {
            stringResource(
                R.string.dish_items_kcal_per_100g,
                summary.items.size,
                (summary.totalCalories / cookedWeight * 100).toInt(),
            )
        } else {
            stringResource(R.string.items_count_kcal, summary.items.size, summary.totalCalories.toInt())
        }
    CompactRowCard(
        title = summary.preset.name,
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier,
    ) {
        // Default IconButton size keeps the Material-minimum 48dp touch target (a11y).
        IconButton(onClick = onLog) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.log_preset),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.delete_preset),
            )
        }
    }
}
