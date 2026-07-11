package app.diaita.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.repository.GoalRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class GoalsUiState(
    val calorieTarget: String = "",
    val proteinTarget: String = "",
    val fatTarget: String = "",
    val carbsTarget: String = "",
    val weightTarget: String = "",
    val hasError: Boolean = false,
)

class GoalsViewModel(
    private val goalRepository: GoalRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    /**
     * One-shot event per successful save. A SharedFlow (not a `saved` Boolean in [uiState]) because
     * resetting a Boolean from the collector cancels the LaunchedEffect keyed on it mid-snackbar —
     * that bug made the "Goal saved" confirmation never actually appear.
     */
    private val _savedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            goalRepository.getGoal().collect { goal ->
                if (goal != null) {
                    _uiState.update {
                        it.copy(
                            calorieTarget = goal.calorieTarget.toString(),
                            proteinTarget = goal.proteinTargetG.toString(),
                            fatTarget = goal.fatTargetG.toString(),
                            carbsTarget = goal.carbsTargetG.toString(),
                            weightTarget = goal.weightTargetKg
                                ?.let { kg -> String.format(Locale.US, "%.1f", kg) }
                                .orEmpty(),
                        )
                    }
                }
            }
        }
    }

    fun onCalorieTargetChanged(value: String) = _uiState.update { it.copy(calorieTarget = value, hasError = false) }
    fun onProteinTargetChanged(value: String) = _uiState.update { it.copy(proteinTarget = value, hasError = false) }
    fun onFatTargetChanged(value: String) = _uiState.update { it.copy(fatTarget = value, hasError = false) }
    fun onCarbsTargetChanged(value: String) = _uiState.update { it.copy(carbsTarget = value, hasError = false) }
    fun onWeightTargetChanged(value: String) = _uiState.update { it.copy(weightTarget = value, hasError = false) }

    /**
     * Quick-split helper: 30% protein / 25% fat / 45% carbs of the calorie target, converted to
     * grams. This sits inside the commonly recommended healthy macro ranges (protein 10-35%,
     * fat 20-35%, carbs 45-65% of energy) while keeping protein high enough for satiety.
     */
    fun applyQuickSplit() {
        val calories = _uiState.value.calorieTarget.toIntOrNull() ?: return
        val proteinG = (calories * 0.30 / 4).toInt()
        val fatG = (calories * 0.25 / 9).toInt()
        val carbsG = (calories * 0.45 / 4).toInt()
        _uiState.update {
            it.copy(
                proteinTarget = proteinG.toString(),
                fatTarget = fatG.toString(),
                carbsTarget = carbsG.toString(),
            )
        }
    }

    fun saveGoal() {
        val state = _uiState.value
        val calories = state.calorieTarget.toIntOrNull()
        val protein = state.proteinTarget.toIntOrNull()
        val fat = state.fatTarget.toIntOrNull()
        val carbs = state.carbsTarget.toIntOrNull()
        // Weight target is optional: blank = no target; non-blank must parse.
        val weightText = state.weightTarget.trim().replace(',', '.')
        val weight = if (weightText.isBlank()) null else weightText.toDoubleOrNull()
        val weightInvalid = weightText.isNotBlank() && (weight == null || weight <= 0)
        if (calories == null || protein == null || fat == null || carbs == null || weightInvalid) {
            _uiState.update { it.copy(hasError = true) }
            return
        }
        viewModelScope.launch {
            goalRepository.setGoal(calories, protein, fat, carbs, weight)
            _savedEvent.emit(Unit)
        }
    }

}
