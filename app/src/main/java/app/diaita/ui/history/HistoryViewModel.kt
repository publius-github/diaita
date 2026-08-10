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
import app.diaita.util.AdaptiveTdee
import app.diaita.util.DateUtils
import app.diaita.util.WeightTrend
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
    /** Trailing 7-day average of logged days, for the trend polyline; null until data exists. */
    val rollingAvg: Double? = null,
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
    /** "N of the last 7 logged-or-not days were on track" — the streak line. */
    val onTrackOfLast7: Int = 0,
    val loggedOfLast7: Int = 0,
    /** Least-squares weight change over the visible range, kg/week; null when too sparse. */
    val weightRatePerWeek: Double? = null,
    /** Date the weight trend crosses the target, ISO; null when flat/diverging/absent. */
    val projectedTargetDate: String? = null,
    /** TDEE observed from the last 28 days of logs and weigh-ins; null when data is too thin. */
    val adaptiveTdee: Int? = null,
    /** Average macros per logged day over the visible range; null when nothing is logged. */
    val avgProteinG: Int? = null,
    val avgFatG: Int? = null,
    val avgCarbsG: Int? = null,
) {
    val selectedDay: HistoryDayUi? get() = selectedIndex?.let { days.getOrNull(it) }
    val selectedDayWeightKg: Double? get() =
        selectedDay?.let { day ->
            weights.firstOrNull { it.date == day.date }?.weightKg
        }
    val hasAnyCalories: Boolean get() = days.any { it.calories > 0 }
}

private val ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
private val LABEL_FORMATTER = DateTimeFormatter.ofPattern("d.MM")

class HistoryViewModel(
    diaryRepository: DiaryRepository,
    goalRepository: GoalRepository,
    private val weightRepository: WeightRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val selectedIndexFlow = MutableStateFlow<Int?>(null)
    private val rangeDaysFlow = MutableStateFlow(DEFAULT_RANGE_DAYS)

    /** Adaptive-TDEE inputs are a fixed 28-day window, independent of the visible range. */
    private val adaptiveTdeeFlow =
        run {
            val to = LocalDate.now()
            val from = to.minusDays(ADAPTIVE_TDEE_WINDOW_DAYS - 1L)
            combine(
                diaryRepository.historyTotals(from.format(ISO_FORMATTER), to.format(ISO_FORMATTER)),
                weightRepository.range(from.format(ISO_FORMATTER), to.format(ISO_FORMATTER)),
            ) { totals, weights ->
                estimateAdaptiveTdee(totals, weights)
            }
        }

    val uiState: StateFlow<HistoryUiState> =
        rangeDaysFlow
            .flatMapLatest { rangeDays ->
                val toDate = LocalDate.now()
                val fromDate = toDate.minusDays(rangeDays - 1L)
                val fromIso = fromDate.format(ISO_FORMATTER)
                val toIso = toDate.format(ISO_FORMATTER)
                combine(
                    diaryRepository.historyMacroTotals(fromIso, toIso),
                    goalRepository.getGoal(),
                    weightRepository.range(fromIso, toIso),
                    diaryRepository.monthlyAverages(),
                    combine(selectedIndexFlow, settingsRepository.weightUnit, adaptiveTdeeFlow, ::Triple),
                ) { macroTotals, goal, weights, aggregates, (selectedIndex, weightUnit, adaptiveTdee) ->
                    val totals = macroTotals.map { DayTotal(it.logDate, it.totalCalories) }
                    val days =
                        buildFullRange(fromDate, toDate, totals, goal?.calorieTarget, goal?.maintenanceCalories)
                    val last7 = days.takeLast(7)
                    val weightPoints = weights.map { LocalDate.parse(it.date) to it.weightKg }
                    val loggedDays = macroTotals.filter { it.totalCalories > 0 }
                    HistoryUiState(
                        rangeDays = rangeDays,
                        days = days,
                        goalCalories = goal?.calorieTarget,
                        maintenanceCalories = goal?.maintenanceCalories,
                        weightTargetKg = goal?.weightTargetKg,
                        selectedIndex = selectedIndex,
                        weights = weights,
                        monthlyAverages = aggregates,
                        weightUnit = weightUnit,
                        onTrackOfLast7 = last7.count { it.calories > 0 && it.zone == CalorieZone.ON_TRACK },
                        loggedOfLast7 = last7.count { it.calories > 0 },
                        weightRatePerWeek = WeightTrend.ratePerWeek(weightPoints),
                        projectedTargetDate =
                            goal?.weightTargetKg?.let { target ->
                                WeightTrend.projectedTargetDate(weightPoints, target)?.format(ISO_FORMATTER)
                            },
                        adaptiveTdee = adaptiveTdee,
                        avgProteinG =
                            loggedDays
                                .takeIf { it.isNotEmpty() }
                                ?.map { it.totalProtein }
                                ?.average()
                                ?.toInt(),
                        avgFatG =
                            loggedDays
                                .takeIf { it.isNotEmpty() }
                                ?.map { it.totalFat }
                                ?.average()
                                ?.toInt(),
                        avgCarbsG =
                            loggedDays
                                .takeIf { it.isNotEmpty() }
                                ?.map { it.totalCarbs }
                                ?.average()
                                ?.toInt(),
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
        val window = ArrayDeque<Double>()
        var date = fromDate
        while (!date.isAfter(toDate)) {
            val iso = date.format(ISO_FORMATTER)
            val calories = caloriesByDate[iso] ?: 0.0
            if (calories > 0) {
                window.addLast(calories)
                if (window.size > ROLLING_WINDOW_DAYS) window.removeFirst()
            }
            days.add(
                HistoryDayUi(
                    date = iso,
                    label = date.format(LABEL_FORMATTER),
                    calories = calories,
                    zone = calorieZone(calories, goalCalories, maintenanceCalories),
                    rollingAvg = window.takeIf { it.isNotEmpty() }?.average(),
                ),
            )
            date = date.plusDays(1)
        }
        return days
    }

    private fun estimateAdaptiveTdee(
        totals: List<DayTotal>,
        weights: List<WeightEntry>,
    ): Int? {
        val first = weights.firstOrNull() ?: return null
        val last = weights.lastOrNull() ?: return null
        val spanDays =
            (LocalDate.parse(last.date).toEpochDay() - LocalDate.parse(first.date).toEpochDay()).toInt()
        return AdaptiveTdee.estimate(
            dailyIntakes = totals.map { it.totalCalories }.filter { it > 0 },
            startWeightKg = first.weightKg,
            endWeightKg = last.weightKg,
            spanDays = spanDays,
        )
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

        /** Retention keeps raw rows up to a year, so the chart can range that far too. */
        val RANGE_OPTIONS = listOf(7, 14, 30, 90, 365)

        const val ROLLING_WINDOW_DAYS = 7
        const val ADAPTIVE_TDEE_WINDOW_DAYS = 28
    }
}
