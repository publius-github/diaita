package app.diaita.ui.daydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.db.dao.DiaryEntryWithFood
import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.repository.DiaryRepository
import app.diaita.data.settings.SettingsRepository
import app.diaita.ui.diary.DiaryRow
import app.diaita.ui.diary.DiarySection
import app.diaita.ui.diary.buildDiarySections
import app.diaita.util.DateUtils
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

data class DayDetailUiState(
    val date: String = "",
    val sections: List<DiarySection> = emptyList(),
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalCarbs: Double = 0.0,
    /** Null means no logged food carried the nutrient — the line is then hidden, not zeroed. */
    val totalFiber: Double? = null,
    val totalSugar: Double? = null,
    val totalSalt: Double? = null,
    /** Mirrors the "track macros" setting — hides the P/F/C line when false. */
    val trackMacros: Boolean = true,
    /** Mirrors the "track fiber/sugar/salt" setting — hides the micronutrient line when false. */
    val trackMicronutrients: Boolean = true,
)

/** Emitted right after a delete so the UI can offer a Snackbar "Undo". */
data class DayDetailUndoEvent(
    val entries: List<DiaryEntry>,
    val title: String?,
)

/**
 * A past day opened from the History chart. No longer a dead end: entries can be edited,
 * deleted (with undo) and the day — or one meal of it — copied onto today.
 */
class DayDetailViewModel(
    private val diaryRepository: DiaryRepository,
    settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val date: String = savedStateHandle["date"] ?: DateUtils.today()

    val uiState: StateFlow<DayDetailUiState> =
        combine(
            diaryRepository.entriesForDate(date),
            diaryRepository.dailyTotals(date),
            settingsRepository.trackMacros,
            settingsRepository.trackMicronutrients,
        ) { entries, totals, trackMacros, trackMicronutrients ->
            DayDetailUiState(
                date = date,
                sections = buildDiarySections(entries),
                totalCalories = totals.totalCalories,
                totalProtein = totals.totalProtein,
                totalFat = totals.totalFat,
                totalCarbs = totals.totalCarbs,
                totalFiber = totals.totalFiber,
                totalSugar = totals.totalSugar,
                totalSalt = totals.totalSalt,
                trackMacros = trackMacros,
                trackMicronutrients = trackMicronutrients,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DayDetailUiState(date = date),
        )

    private val _undoEvent = MutableSharedFlow<DayDetailUndoEvent>(extraBufferCapacity = 1)
    val undoEvent: SharedFlow<DayDetailUndoEvent> = _undoEvent.asSharedFlow()

    private val _copyResultEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    /** Emits how many entries "copy to today" inserted; 0 means nothing new to copy. */
    val copyResultEvent: SharedFlow<Int> = _copyResultEvent.asSharedFlow()

    private val _editingEntry = MutableStateFlow<DiaryEntryWithFood?>(null)
    val editingEntry: StateFlow<DiaryEntryWithFood?> = _editingEntry.asStateFlow()

    /** Deletes immediately; the emitted event lets the snackbar offer Undo. */
    fun delete(row: DiaryRow) {
        val (entries, title) =
            when (row) {
                is DiaryRow.Food -> listOf(row.entryWithFood.entry) to row.entryWithFood.food.name
                is DiaryRow.Preset -> row.entries.map { it.entry } to row.presetName
            }
        viewModelScope.launch {
            entries.forEach { diaryRepository.deleteEntry(it) }
            _undoEvent.emit(DayDetailUndoEvent(entries, title))
        }
    }

    fun undoDelete(entries: List<DiaryEntry>) {
        viewModelScope.launch {
            entries.forEach { diaryRepository.restoreEntry(it) }
        }
    }

    /** Food rows edit quantity+slot; collapsed preset rows are not editable here (same as Diary). */
    fun requestEdit(row: DiaryRow) {
        if (row is DiaryRow.Food) _editingEntry.value = row.entryWithFood
    }

    fun cancelEdit() {
        _editingEntry.value = null
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

    /** Copies this day (or one [slot] of it) onto today; duplicates are skipped, nothing replaced. */
    fun copyToToday(slot: MealSlot? = null) {
        viewModelScope.launch {
            val today = DateUtils.today()
            val copied =
                if (slot == null) {
                    diaryRepository.copyDay(date, today)
                } else {
                    diaryRepository.copyMeal(date, today, slot)
                }
            _copyResultEvent.emit(copied)
        }
    }
}
