package app.diaita.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.db.dao.DiaryEntryWithFood
import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.repository.DiaryRepository
import app.diaita.data.repository.GoalRepository
import app.diaita.data.repository.WeightRepository
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
) {
    val isEmpty: Boolean get() = sections.isEmpty()
}

/** Emitted right after a delete so the UI can offer a Snackbar "Undo". */
data class UndoDeleteEvent(
    val entries: List<DiaryEntry>,
    val title: String,
)

class DiaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val goalRepository: GoalRepository,
    private val weightRepository: WeightRepository,
) : ViewModel() {
    private val today = DateUtils.today()

    val uiState: StateFlow<DiaryUiState> =
        combine(
            diaryRepository.entriesForDate(today),
            diaryRepository.dailyTotals(today),
            goalRepository.getGoal(),
            weightRepository.range(today, today),
        ) { entries, totals, goal, todayWeights ->
            DiaryUiState(
                loaded = true,
                sections = buildDiarySections(entries),
                dayPartCalories = buildDayPartCalories(entries),
                totalCalories = totals.totalCalories,
                totalProtein = totals.totalProtein,
                totalFat = totals.totalFat,
                totalCarbs = totals.totalCarbs,
                goalCalories = goal?.calorieTarget,
                goalProteinG = goal?.proteinTargetG,
                goalFatG = goal?.fatTargetG,
                goalCarbsG = goal?.carbsTargetG,
                todayWeightKg = todayWeights.firstOrNull()?.weightKg,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DiaryUiState(),
        )

    fun logWeight(weightKg: Double) {
        viewModelScope.launch { weightRepository.logWeight(today, weightKg) }
    }

    private val _copyResultEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    /** Emits how many entries "repeat yesterday" copied; 0 means yesterday was empty. */
    val copyResultEvent: SharedFlow<Int> = _copyResultEvent.asSharedFlow()

    fun copyYesterday() {
        viewModelScope.launch {
            val yesterday =
                LocalDate
                    .parse(today)
                    .minusDays(1)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE)
            _copyResultEvent.emit(diaryRepository.copyDay(yesterday, today))
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
