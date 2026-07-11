package app.diaita.ui.presets

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.ui.AppViewModelProvider

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
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.undo)

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
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPresetClick) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_preset))
            }
        },
    ) { innerPadding ->
        if (presets.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.no_presets_yet))
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(presets, key = { it.preset.id }) { presetWithItems ->
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .clickable { onEditPresetClick(presetWithItems.preset.id) },
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(presetWithItems.preset.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(
                                        R.string.items_count_kcal,
                                        presetWithItems.items.size,
                                        presetWithItems.totalCalories.toInt(),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.logPreset(presetWithItems.preset.id)
                                    },
                                ) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = stringResource(R.string.log_preset),
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.deletePreset(presetWithItems.preset)
                                    },
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.delete_preset),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
