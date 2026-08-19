package app.diaita.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.db.dao.DiaryEntryWithFood
import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.repository.DiaryRepository
import app.diaita.data.repository.GoalRepository
import app.diaita.data.repository.WeightRepository
import app.diaita.data.settings.CoachTone
import app.diaita.data.settings.DayPartBoundaries
import app.diaita.data.settings.DayPartOverThresholds
import app.diaita.data.settings.DayPartShares
import app.diaita.data.settings.HomeProgressStyle
import app.diaita.data.settings.MacroOverTolerances
import app.diaita.data.settings.MicronutrientOverTolerances
import app.diaita.data.settings.SettingsRepository
import app.diaita.data.settings.WeightUnit
import app.diaita.util.DateUtils
import app.diaita.util.NutrientMath
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** One row in the Diary list: either a single logged food, or a whole preset collapsed into one row. */
sealed class DiaryRow {
    abstract val calories: Double
    abstract val loggedAt: Long

    data class Food(
        val entryWithFood: DiaryEntryWithFood,
    ) : DiaryRow() {
        override val calories: Double
            get() = NutrientMath.caloriesFor(entryWithFood.food, entryWithFood.entry.quantityGrams)
        override val loggedAt: Long
            get() = entryWithFood.entry.loggedAt
    }

    data class Preset(
        /** Name snapshot from log time; null (legacy rows) maps to a localized fallback in the UI. */
        val presetName: String?,
        val entries: List<DiaryEntryWithFood>,
    ) : DiaryRow() {
        override val calories: Double
            get() = entries.sumOf { NutrientMath.caloriesFor(it.food, it.entry.quantityGrams) }
        override val loggedAt: Long
            get() = entries.first().entry.loggedAt
    }
}

data class DiaryUiState(
    /** False until the first combine emission — keeps the "no goal" banner from flashing on load. */
    val loaded: Boolean = false,
    /** The ISO date the diary currently shows; the date navigation moves it, today by default. */
    val date: String = DateUtils.today(),
    val isToday: Boolean = true,
    val sections: List<DiarySection> = emptyList(),
    val dayPartCalories: Map<DayPart, Double> = emptyMap(),
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalCarbs: Double = 0.0,
    /** Null means no logged food carried the nutrient — the line is then hidden, not zeroed. */
    val totalFiber: Double? = null,
    val totalSugar: Double? = null,
    val totalSalt: Double? = null,
    val goalCalories: Int? = null,
    /** TDEE stored by the goal wizard; splits "over goal" into amber (under it) and red (past it). */
    val maintenanceCalories: Int? = null,
    val goalProteinG: Int? = null,
    val goalFatG: Int? = null,
    val goalCarbsG: Int? = null,
    val todayWeightKg: Double? = null,
    /** Mirrors the "track macros" setting — hides the P/F/C progress UI when false. */
    val trackMacros: Boolean = true,
    /** Mirrors the "track fiber/sugar/salt" setting — hides the micronutrient line when false. */
    val trackMicronutrients: Boolean = true,
    /** Per-macro overshoot slack before a bar flips from "goal reached" to a warning. */
    val macroOverTolerances: MacroOverTolerances = MacroOverTolerances(),
    /** Per-nutrient slack past the fiber/sugar/salt limits before the totals line turns red. */
    val microOverTolerances: MicronutrientOverTolerances = MicronutrientOverTolerances(),
    /** Overshoot bands (green/amber/red) for the morning/day/evening segments. */
    val dayPartOverThresholds: DayPartOverThresholds = DayPartOverThresholds(),
    val dayPartShares: DayPartShares = DayPartShares(),
    val dayPartBoundaries: DayPartBoundaries = DayPartBoundaries(),
    val coachTone: CoachTone = CoachTone.SNARKY,
    val weightUnit: WeightUnit = WeightUnit.KG,
    /** Shape of the day-total calorie indicator on the summary card (Appearance setting). */
    val homeProgressStyle: HomeProgressStyle = HomeProgressStyle.RING,
) {
    val isEmpty: Boolean get() = sections.isEmpty()
}

/** Emitted right after a delete so the UI can offer a Snackbar "Undo". */
data class UndoDeleteEvent(
    val entries: List<DiaryEntry>,
    /** Null when a legacy preset row had no name snapshot; the UI substitutes a resource. */
    val title: String?,
)

/** The "which nutrients do I care about" toggles, paired to fit the 5-flow combine overload. */
private data class TrackingFlags(
    val macros: Boolean,
    val micronutrients: Boolean,
    val macroOverTolerances: MacroOverTolerances,
    val microOverTolerances: MicronutrientOverTolerances,
    val dayPartOverThresholds: DayPartOverThresholds,
)

/** Purely presentational choices for the Home card, paired to fit the 5-flow combine overload. */
private data class PresentationSettings(
    val coachTone: CoachTone,
    val weightUnit: WeightUnit,
    val homeProgressStyle: HomeProgressStyle,
)

/** The Home-screen slice of user settings, bundled to fit the 5-flow combine overload. */
private data class HomeSettings(
    val tracking: TrackingFlags,
    val boundaries: DayPartBoundaries,
    val shares: DayPartShares,
    val presentation: PresentationSettings,
)

class DiaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val goalRepository: GoalRepository,
    private val weightRepository: WeightRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    /** The date the diary shows and every action (log, weight, copy) targets. */
    private val selectedDate = MutableStateFlow(DateUtils.today())

    val uiState: StateFlow<DiaryUiState> =
        selectedDate
            .flatMapLatest { date ->
                combine(
                    diaryRepository.entriesForDate(date),
                    diaryRepository.dailyTotals(date),
                    goalRepository.getGoal(),
                    weightRepository.range(date, date),
                    combine(
                        combine(
                            settingsRepository.trackMacros,
                            settingsRepository.trackMicronutrients,
                            settingsRepository.macroOverTolerances,
                            settingsRepository.microOverTolerances,
                            settingsRepository.dayPartOverThresholds,
                            ::TrackingFlags,
                        ),
                        settingsRepository.dayPartBoundaries,
                        settingsRepository.dayPartShares,
                        combine(
                            settingsRepository.coachTone,
                            settingsRepository.weightUnit,
                            settingsRepository.homeProgressStyle,
                            ::PresentationSettings,
                        ),
                        ::HomeSettings,
                    ),
                ) { entries, totals, goal, dayWeights, settings ->
                    DiaryUiState(
                        loaded = true,
                        date = date,
                        isToday = date == DateUtils.today(),
                        sections = buildDiarySections(entries),
                        dayPartCalories = buildDayPartCalories(entries, settings.boundaries),
                        totalCalories = totals.totalCalories,
                        totalProtein = totals.totalProtein,
                        totalFat = totals.totalFat,
                        totalCarbs = totals.totalCarbs,
                        totalFiber = totals.totalFiber,
                        totalSugar = totals.totalSugar,
                        totalSalt = totals.totalSalt,
                        goalCalories = goal?.calorieTarget,
                        maintenanceCalories = goal?.maintenanceCalories,
                        goalProteinG = goal?.proteinTargetG,
                        goalFatG = goal?.fatTargetG,
                        goalCarbsG = goal?.carbsTargetG,
                        todayWeightKg = dayWeights.firstOrNull()?.weightKg,
                        trackMacros = settings.tracking.macros,
                        trackMicronutrients = settings.tracking.micronutrients,
                        macroOverTolerances = settings.tracking.macroOverTolerances,
                        microOverTolerances = settings.tracking.microOverTolerances,
                        dayPartOverThresholds = settings.tracking.dayPartOverThresholds,
                        dayPartShares = settings.shares,
                        dayPartBoundaries = settings.boundaries,
                        coachTone = settings.presentation.coachTone,
                        weightUnit = settings.presentation.weightUnit,
                        homeProgressStyle = settings.presentation.homeProgressStyle,
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = DiaryUiState(),
            )

    fun selectPreviousDay() {
        selectedDate.value =
            LocalDate
                .parse(selectedDate.value)
                .minusDays(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /** Steps forward one day, never past today — the diary has no future days to plan. */
    fun selectNextDay() {
        val next = LocalDate.parse(selectedDate.value).plusDays(1)
        if (next > LocalDate.now()) return
        selectedDate.value = next.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /** Jumps straight to [date] (calendar picker); future days are refused like in [selectNextDay]. */
    fun selectDate(date: LocalDate) {
        if (date > LocalDate.now()) return
        selectedDate.value = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /** Snaps back to today — the Home tab button doubles as "back to today". */
    fun selectToday() {
        selectedDate.value = DateUtils.today()
    }

    fun logWeight(weightKg: Double) {
        viewModelScope.launch { weightRepository.logWeight(selectedDate.value, weightKg) }
    }

    private val _copyResultEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    /** Emits how many entries a copy inserted; 0 means the source day/meal was empty. */
    val copyResultEvent: SharedFlow<Int> = _copyResultEvent.asSharedFlow()

    /** Copies the day before the shown date onto it — "repeat yesterday" relative to the selection. */
    fun copyYesterday() {
        val target = selectedDate.value
        val dayBefore =
            LocalDate
                .parse(target)
                .minusDays(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
        copyFrom(dayBefore, slot = null)
    }

    /**
     * Copies [fromDate] (whole day, or just [slot]) onto the shown date. Works on non-empty
     * days too — copying tops the day up, it never replaces anything.
     */
    fun copyFrom(
        fromDate: String,
        slot: MealSlot?,
    ) {
        viewModelScope.launch {
            val target = selectedDate.value
            val copied =
                if (slot == null) {
                    diaryRepository.copyDay(fromDate, target)
                } else {
                    diaryRepository.copyMeal(fromDate, target, slot)
                }
            _copyResultEvent.emit(copied)
        }
    }

    private val _undoEvent = MutableSharedFlow<UndoDeleteEvent>(extraBufferCapacity = 1)
    val undoEvent: SharedFlow<UndoDeleteEvent> = _undoEvent.asSharedFlow()

    private val _editingEntry = MutableStateFlow<DiaryEntryWithFood?>(null)
    val editingEntry: StateFlow<DiaryEntryWithFood?> = _editingEntry.asStateFlow()

    private val _editingPresetRow = MutableStateFlow<DiaryRow.Preset?>(null)
    val editingPresetRow: StateFlow<DiaryRow.Preset?> = _editingPresetRow.asStateFlow()

    /** Deletes immediately; the emitted event lets the snackbar offer Undo. */
    fun delete(row: DiaryRow) {
        val (entries, title) =
            when (row) {
                is DiaryRow.Food -> listOf(row.entryWithFood.entry) to row.entryWithFood.food.name
                is DiaryRow.Preset -> row.entries.map { it.entry } to row.presetName
            }
        viewModelScope.launch {
            entries.forEach { diaryRepository.deleteEntry(it) }
            _undoEvent.emit(UndoDeleteEvent(entries, title))
        }
    }

    fun undoDelete(entries: List<DiaryEntry>) {
        viewModelScope.launch {
            entries.forEach { diaryRepository.restoreEntry(it) }
        }
    }

    /** Food rows edit quantity+slot; a collapsed preset row has no single quantity, so it edits the slot only. */
    fun requestEdit(row: DiaryRow) {
        when (row) {
            is DiaryRow.Food -> _editingEntry.value = row.entryWithFood
            is DiaryRow.Preset -> _editingPresetRow.value = row
        }
    }

    fun cancelEdit() {
        _editingEntry.value = null
        _editingPresetRow.value = null
    }

    fun confirmEdit(
        newQuantityGrams: Double,
        newMealSlot: MealSlot,
    ) {
        val current = _editingEntry.value ?: return
        _editingEntry.value = null
        viewModelScope.launch {
            diaryRepository.updateEntry(
                current.entry.copy(quantityGrams = newQuantityGrams, mealSlot = newMealSlot),
            )
        }
    }

    fun confirmPresetSlot(newMealSlot: MealSlot) {
        val row = _editingPresetRow.value ?: return
        _editingPresetRow.value = null
        viewModelScope.launch {
            row.entries.forEach { withFood ->
                diaryRepository.updateEntry(withFood.entry.copy(mealSlot = newMealSlot))
            }
        }
    }
}
