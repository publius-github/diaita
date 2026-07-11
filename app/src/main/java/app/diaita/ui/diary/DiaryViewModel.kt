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

/** One row in the Diary list: either a single logged food, or a whole preset collapsed into one row. */
sealed class DiaryRow {
    abstract val calories: Double
    abstract val loggedAt: Long

    data class Food(val entryWithFood: DiaryEntryWithFood) : DiaryRow() {
        override val calories: Double
            get() = NutrientMath.caloriesFor(entryWithFood.food, entryWithFood.entry.quantityGrams)
        override val loggedAt: Long
            get() = entryWithFood.entry.loggedAt
    }

    data class Preset(val presetName: String, val entries: List<DiaryEntryWithFood>) : DiaryRow() {
        override val calories: Double
            get() = entries.sumOf { NutrientMath.caloriesFor(it.food, it.entry.quantityGrams) }
        override val loggedAt: Long
            get() = entries.first().entry.loggedAt
    }
}

data class DiaryUiState(
    val sections: List<DiarySection> = emptyList(),
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

/** Emitted after a confirmed delete so the UI can offer a Snackbar "Undo". */
data class UndoDeleteEvent(val entries: List<DiaryEntry>)

class DiaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val goalRepository: GoalRepository,
    private val weightRepository: WeightRepository,
) : ViewModel() {
    private val today = DateUtils.today()

    val uiState: StateFlow<DiaryUiState> = combine(
        diaryRepository.entriesForDate(today),
        diaryRepository.dailyTotals(today),
        goalRepository.getGoal(),
        weightRepository.range(today, today),
    ) { entries, totals, goal, todayWeights ->
        DiaryUiState(
            sections = buildDiarySections(entries),
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

    private val _confirmDeleteRow = MutableStateFlow<DiaryRow?>(null)
    val confirmDeleteRow: StateFlow<DiaryRow?> = _confirmDeleteRow.asStateFlow()

    private val _undoEvent = MutableSharedFlow<UndoDeleteEvent>(extraBufferCapacity = 1)
    val undoEvent: SharedFlow<UndoDeleteEvent> = _undoEvent.asSharedFlow()

    private val _editingEntry = MutableStateFlow<DiaryEntryWithFood?>(null)
    val editingEntry: StateFlow<DiaryEntryWithFood?> = _editingEntry.asStateFlow()

    fun requestDelete(row: DiaryRow) {
        _confirmDeleteRow.value = row
    }

    fun cancelDelete() {
        _confirmDeleteRow.value = null
    }

    fun confirmDelete() {
        val row = _confirmDeleteRow.value ?: return
        _confirmDeleteRow.value = null
        val entries = when (row) {
            is DiaryRow.Food -> listOf(row.entryWithFood.entry)
            is DiaryRow.Preset -> row.entries.map { it.entry }
        }
        viewModelScope.launch {
            entries.forEach { diaryRepository.deleteEntry(it) }
            _undoEvent.emit(UndoDeleteEvent(entries))
        }
    }

    fun undoDelete(entries: List<DiaryEntry>) {
        viewModelScope.launch {
            entries.forEach { diaryRepository.restoreEntry(it) }
        }
    }

    /** Editing is only offered for single-food rows — a collapsed preset group has no single quantity to edit. */
    fun requestEdit(row: DiaryRow) {
        if (row is DiaryRow.Food) _editingEntry.value = row.entryWithFood
    }

    fun cancelEdit() {
        _editingEntry.value = null
    }

    fun confirmEdit(newQuantityGrams: Double, newMealSlot: MealSlot) {
        val current = _editingEntry.value ?: return
        _editingEntry.value = null
        viewModelScope.launch {
            diaryRepository.updateEntry(
                current.entry.copy(quantityGrams = newQuantityGrams, mealSlot = newMealSlot),
            )
        }
    }
}
