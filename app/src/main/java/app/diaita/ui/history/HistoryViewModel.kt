package app.diaita.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.db.dao.DayTotal
import app.diaita.data.db.entities.MonthlyAggregate
import app.diaita.data.db.entities.WeightEntry
import app.diaita.data.repository.DiaryRepository
import app.diaita.data.repository.GoalRepository
import app.diaita.data.repository.WeightRepository
import app.diaita.data.settings.SettingsRepository
import app.diaita.data.settings.WeightUnit
import app.diaita.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HistoryDayUi(
    val date: String,
    val label: String,
    val calories: Double,
    val zone: CalorieZone,
)

data class HistoryUiState(
    val rangeDays: Int = HistoryViewModel.DEFAULT_RANGE_DAYS,
    val days: List<HistoryDayUi> = emptyList(),
    val goalCalories: Int? = null,
    /** Wizard-estimated TDEE; null until the wizard has been completed. */
    val maintenanceCalories: Int? = null,
    val weightTargetKg: Double? = null,
    val selectedIndex: Int? = null,
    val weights: List<WeightEntry> = emptyList(),
    val monthlyAverages: List<MonthlyAggregate> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.KG,
) {
    val selectedDay: HistoryDayUi? get() = selectedIndex?.let { days.getOrNull(it) }
    val selectedDayWeightKg: Double? get() = selectedDay?.let { day -> weights.firstOrNull { it.date == day.date }?.weightKg }
    val hasAnyCalories: Boolean get() = days.any { it.calories > 0 }
}

private val ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
private val LABEL_FORMATTER = DateTimeFormatter.ofPattern("d.MM")

class HistoryViewModel(
    diaryRepository: DiaryRepository,
    goalRepository: GoalRepository,
    private val weightRepository: WeightRepository,
    monthlyAverages: Flow<List<MonthlyAggregate>>,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val selectedIndexFlow = MutableStateFlow<Int?>(null)
    private val rangeDaysFlow = MutableStateFlow(DEFAULT_RANGE_DAYS)

    val uiState: StateFlow<HistoryUiState> =
        rangeDaysFlow
            .flatMapLatest { rangeDays ->
                val toDate = LocalDate.now()
                val fromDate = toDate.minusDays(rangeDays - 1L)
                val fromIso = fromDate.format(ISO_FORMATTER)
                val toIso = toDate.format(ISO_FORMATTER)
                combine(
                    diaryRepository.historyTotals(fromIso, toIso),
                    goalRepository.getGoal(),
                    weightRepository.range(fromIso, toIso),
                    monthlyAverages,
                    combine(selectedIndexFlow, settingsRepository.weightUnit, ::Pair),
                ) { totals, goal, weights, aggregates, (selectedIndex, weightUnit) ->
                    HistoryUiState(
                        rangeDays = rangeDays,
                        days = buildFullRange(fromDate, toDate, totals, goal?.calorieTarget, goal?.maintenanceCalories),
                        goalCalories = goal?.calorieTarget,
                        maintenanceCalories = goal?.maintenanceCalories,
                        weightTargetKg = goal?.weightTargetKg,
                        selectedIndex = selectedIndex,
                        weights = weights,
                        monthlyAverages = aggregates,
                        weightUnit = weightUnit,
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    private fun buildFullRange(
        fromDate: LocalDate,
        toDate: LocalDate,
        totals: List<DayTotal>,
        goalCalories: Int?,
        maintenanceCalories: Int?,
    ): List<HistoryDayUi> {
        val caloriesByDate = totals.associate { it.logDate to it.totalCalories }
        val days = mutableListOf<HistoryDayUi>()
        var date = fromDate
        while (!date.isAfter(toDate)) {
            val iso = date.format(ISO_FORMATTER)
            val calories = caloriesByDate[iso] ?: 0.0
            days.add(
                HistoryDayUi(
                    date = iso,
                    label = date.format(LABEL_FORMATTER),
                    calories = calories,
                    zone = calorieZone(calories, goalCalories, maintenanceCalories),
                ),
            )
            date = date.plusDays(1)
        }
        return days
    }

    fun setRangeDays(days: Int) {
        selectedIndexFlow.value = null
        rangeDaysFlow.value = days
    }

    fun onBarSelected(index: Int) {
        selectedIndexFlow.value = if (selectedIndexFlow.value == index) null else index
    }

    fun logWeight(weightKg: Double) {
        viewModelScope.launch { weightRepository.logWeight(DateUtils.today(), weightKg) }
    }

    companion object {
        const val DEFAULT_RANGE_DAYS = 7
        val RANGE_OPTIONS = listOf(7, 14, 30)
    }
}
