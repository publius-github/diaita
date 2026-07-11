package app.diaita.ui.daydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.repository.DiaryRepository
import app.diaita.ui.diary.DiarySection
import app.diaita.ui.diary.buildDiarySections
import app.diaita.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DayDetailUiState(
    val date: String = "",
    val sections: List<DiarySection> = emptyList(),
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalCarbs: Double = 0.0,
)

/** Read-only view of an arbitrary past day, opened from the History chart. */
class DayDetailViewModel(
    diaryRepository: DiaryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val date: String = savedStateHandle["date"] ?: DateUtils.today()

    val uiState: StateFlow<DayDetailUiState> = combine(
        diaryRepository.entriesForDate(date),
        diaryRepository.dailyTotals(date),
    ) { entries, totals ->
        DayDetailUiState(
            date = date,
            sections = buildDiarySections(entries),
            totalCalories = totals.totalCalories,
            totalProtein = totals.totalProtein,
            totalFat = totals.totalFat,
            totalCarbs = totals.totalCarbs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DayDetailUiState(date = date),
    )
}
