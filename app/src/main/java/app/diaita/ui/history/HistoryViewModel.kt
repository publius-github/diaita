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
import app.diaita.data.settings.WizardInputs
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
)

data class HistoryUiState(
    val rangeDays: Int = HistoryViewModel.DEFAULT_RANGE_DAYS,
    /** Days per calorie bar: 1 on daily ranges, 7 when the year range aggregates weekly. */
    val bucketDays: Int = 1,
    val days: List<HistoryDayUi> = emptyList(),
    val goalCalories: Int? = null,
    /** Wizard-estimated TDEE; null until the wizard has been completed. */
    val maintenanceCalories: Int? = null,
    val weightTargetKg: Double? = null,
    val selectedIndex: Int? = null,
    val weights: List<WeightEntry> = emptyList(),
    val monthlyAverages: List<MonthlyAggregate> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    /** Height from the last completed goal wizard; null until it ran once. Feeds the BMI tile. */
    val heightCm: Double? = null,
    /** Widens the BMI tile's upper bands for muscular builds (informal heuristic, user-opted). */
    val athleticBmiBands: Boolean = false,
    /** Least-squares weight change over the visible range, kg/week; null when too sparse. */
    val weightRatePerWeek: Double? = null,
    /** Date the weight trend crosses the target, ISO; null when flat/diverging/absent. */
    val projectedTargetDate: String? = null,
) {
    val selectedDay: HistoryDayUi? get() = selectedIndex?.let { days.getOrNull(it) }
    val selectedDayWeightKg: Double? get() =
        selectedDay?.let { day ->
            weights.firstOrNull { it.date == day.date }?.weightKg
        }
    val hasAnyCalories: Boolean get() = days.any { it.calories > 0 }
}

private val ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

/** UI-preference slice of the state, bundled to fit the 5-flow combine overload. */
private data class HistoryPrefs(
    val selectedIndex: Int?,
    val weightUnit: WeightUnit,
    val wizardInputs: WizardInputs?,
    val athleticBmiBands: Boolean,
)

private val LABEL_FORMATTER = DateTimeFormatter.ofPattern("d.MM")

class HistoryViewModel(
    diaryRepository: DiaryRepository,
    goalRepository: GoalRepository,
    private val weightRepository: WeightRepository,
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
                    diaryRepository.monthlyAverages(),
                    combine(
                        selectedIndexFlow,
                        settingsRepository.weightUnit,
                        settingsRepository.wizardInputs,
                        settingsRepository.athleticBmiBands,
                        ::HistoryPrefs,
                    ),
                ) { totals, goal, weights, aggregates, prefs ->
                    val allDays =
                        buildFullRange(fromDate, toDate, totals, goal?.calorieTarget, goal?.maintenanceCalories)
                    val bucketDays = if (rangeDays > MAX_DAILY_BARS_RANGE) 7 else 1
                    val days =
                        if (bucketDays > 1) {
                            bucketWeekly(allDays, goal?.calorieTarget, goal?.maintenanceCalories)
                        } else {
                            allDays
                        }
                    val weightPoints = weights.map { LocalDate.parse(it.date) to it.weightKg }
                    HistoryUiState(
                        rangeDays = rangeDays,
                        bucketDays = bucketDays,
                        days = days,
                        goalCalories = goal?.calorieTarget,
                        maintenanceCalories = goal?.maintenanceCalories,
                        weightTargetKg = goal?.weightTargetKg,
                        selectedIndex = prefs.selectedIndex,
                        weights = weights,
                        monthlyAverages = aggregates,
                        weightUnit = prefs.weightUnit,
                        heightCm = prefs.wizardInputs?.heightCm,
                        athleticBmiBands = prefs.athleticBmiBands,
                        weightRatePerWeek = WeightTrend.ratePerWeek(weightPoints),
                        projectedTargetDate =
                            goal?.weightTargetKg?.let { target ->
                                WeightTrend.projectedTargetDate(weightPoints, target)?.format(ISO_FORMATTER)
                            },
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
        const val DEFAULT_RANGE_DAYS = 30

        /** Longest range still drawn as one bar per day — 365 daily bars were unreadable. */
        const val MAX_DAILY_BARS_RANGE = 90

        /** The year range only became viable with weekly bucketing, see [bucketWeekly]. */
        val RANGE_OPTIONS = listOf(7, 14, 30, 90, 365)
    }
}

/**
 * Folds a per-day range into 7-day buckets for the year view: one bar per week, its value
 * the average of the week's logged days (empty days don't drag the average toward zero;
 * a week with nothing logged stays 0 and is skipped by the chart). Each bucket keeps its
 * first day's date/label, so date labels and week-of info lines stay real dates.
 */
internal fun bucketWeekly(
    days: List<HistoryDayUi>,
    goalCalories: Int?,
    maintenanceCalories: Int?,
): List<HistoryDayUi> =
    days.chunked(7).map { week ->
        val logged = week.filter { it.calories > 0 }
        val avgCalories = if (logged.isEmpty()) 0.0 else logged.sumOf { it.calories } / logged.size
        HistoryDayUi(
            date = week.first().date,
            label = week.first().label,
            calories = avgCalories,
            zone = calorieZone(avgCalories, goalCalories, maintenanceCalories),
        )
    }
