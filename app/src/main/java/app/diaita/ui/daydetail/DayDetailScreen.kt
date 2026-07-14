package app.diaita.ui.daydetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.data.db.entities.MealSlot
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.common.label
import app.diaita.ui.diary.DiaryRow
import app.diaita.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DayDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(formatDate(uiState.date)) },
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
                    .padding(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.kcal_amount, uiState.totalCalories.toInt()),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        stringResource(
                            R.string.macros_line,
                            uiState.totalProtein.toInt(),
                            uiState.totalFat.toInt(),
                            uiState.totalCarbs.toInt(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.sections.isEmpty()) {
                Text(stringResource(R.string.day_detail_empty), style = MaterialTheme.typography.bodyMedium)
            } else {
                val showHeaders =
                    uiState.sections.size > 1 ||
                        uiState.sections.firstOrNull()?.slot != MealSlot.UNSPECIFIED
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.sections.forEach { section ->
                        if (showHeaders) {
                            item(key = "header_${section.slot}") {
                                Text(section.slot.label(), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                        items(section.rows, key = ::rowKey) { row ->
                            DayDetailRowCard(row)
                        }
                    }
                }
            }
        }
    }
}

private fun rowKey(row: DiaryRow): Long =
    when (row) {
        is DiaryRow.Food -> row.entryWithFood.entry.id
        is DiaryRow.Preset ->
            row.entries
                .first()
                .entry.id
    }

@Composable
private fun DayDetailRowCard(row: DiaryRow) {
    val title =
        when (row) {
            is DiaryRow.Food -> row.entryWithFood.food.name
            is DiaryRow.Preset -> row.presetName
        }
    val time = DateUtils.formatTime(row.loggedAt)
    val subtitle =
        when (row) {
            is DiaryRow.Food ->
                stringResource(
                    R.string.diary_food_subtitle,
                    time,
                    row.entryWithFood.entry.quantityGrams
                        .toInt(),
                    row.calories.toInt(),
                )
            is DiaryRow.Preset ->
                stringResource(
                    R.string.diary_preset_subtitle,
                    time,
                    row.entries.size,
                    row.calories.toInt(),
                )
        }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatDate(isoDate: String): String =
    runCatching { LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("d MMMM yyyy")) }
        .getOrDefault(isoDate)
