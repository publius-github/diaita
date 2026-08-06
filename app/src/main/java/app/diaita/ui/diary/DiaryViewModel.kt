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
import app.diaita.data.settings.DayPartShares
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
        val presetName: String,
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
    val goalCalories: Int? = null,
    val goalProteinG: Int? = null,
    val goalFatG: Int? = null,
    val goalCarbsG: Int? = null,
    val todayWeightKg: Double? = null,
    /** Mirrors the "track macros" setting — hides the P/F/C progress UI when false. */
    val trackMacros: Boolean = true,
    val dayPartShares: DayPartShares = DayPartShares(),
    val dayPartBoundaries: DayPartBoundaries = DayPartBoundaries(),
    val coachTone: CoachTone = CoachTone.SNARKY,
    val weightUnit: WeightUnit = WeightUnit.KG,
) {
    val isEmpty: Boolean get() = sections.isEmpty()
}

/** Emitted right after a delete so the UI can offer a Snackbar "Undo". */
data class UndoDeleteEvent(
    val entries: List<DiaryEntry>,
    val title: String,
)

/** The Home-screen slice of user settings, bundled to fit the 5-flow combine overload. */
private data class HomeSettings(
    val trackMacros: Boolean,
    val boundaries: DayPartBoundaries,
    val shares: DayPartShares,
    val coachTone: CoachTone,
    val weightUnit: WeightUnit,
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
                        settingsRepository.trackMacros,
                        settingsRepository.dayPartBoundaries,
                        settingsRepository.dayPartShares,
                        settingsRepository.coachTone,
                        settingsRepository.weightUnit,
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
                        goalCalories = goal?.calorieTarget,
                        goalProteinG = goal?.proteinTargetG,
                        goalFatG = goal?.fatTargetG,
                        goalCarbsG = goal?.carbsTargetG,
                        todayWeightKg = dayWeights.firstOrNull()?.weightKg,
                        trackMacros = settings.trackMacros,
                        dayPartShares = settings.shares,
                        dayPartBoundaries = settings.boundaries,
                        coachTone = settings.coachTone,
                        weightUnit = settings.weightUnit,
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

    /** Emits how many entries "repeat yesterday" copied; 0 means yesterday was empty. */
    val copyResultEvent: SharedFlow<Int> = _copyResultEvent.asSharedFlow()

    /** Copies the day before the shown date onto it — "repeat yesterday" relative to the selection. */
    fun copyYesterday() {
        viewModelScope.launch {
            val target = selectedDate.value
            val dayBefore =
                LocalDate
                    .parse(target)
                    .minusDays(1)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE)
            _copyResultEvent.emit(diaryRepository.copyDay(dayBefore, target))
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
