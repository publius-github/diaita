package app.diaita.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.repository.GoalRepository
import app.diaita.data.settings.SettingsRepository
import app.diaita.data.settings.WeightUnit
import app.diaita.util.ExerciseLevel
import app.diaita.util.GoalCalculator
import app.diaita.util.WeightPlan
import app.diaita.util.WorkActivity
import app.diaita.util.normalizedDecimalOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Computed numbers for the wizard's result step. */
data class WizardResult(
    val bmr: Int,
    val tdee: Int,
    val targetCalories: Int,
    /** True when the raw suggestion fell below the safe minimum and was raised to it. */
    val raisedToMinimum: Boolean,
)

data class GoalWizardUiState(
    val step: Int = 0,
    val male: Boolean = true,
    val ageText: String = "",
    val heightText: String = "",
    val weightText: String = "",
    val work: WorkActivity = WorkActivity.SEDENTARY,
    val exercise: ExerciseLevel = ExerciseLevel.NONE,
    val plan: WeightPlan = WeightPlan.MAINTAIN,
    val currentGoalCalories: Int? = null,
    /** Unit the weight field is typed in; the BMR math below always runs on kilograms. */
    val weightUnit: WeightUnit = WeightUnit.KG,
) {
    val age: Int? get() = ageText.toIntOrNull()?.takeIf { it in 10..100 }
    val height: Double? get() = heightText.normalizedDecimalOrNull()?.takeIf { it in 100.0..250.0 }
    val weight: Double?
        get() =
            weightText
                .normalizedDecimalOrNull()
                ?.let(weightUnit::toKg)
                ?.takeIf { it in 30.0..300.0 }
    val aboutYouValid: Boolean get() = age != null && height != null && weight != null

    val result: WizardResult?
        get() {
            val age = age ?: return null
            val height = height ?: return null
            val weight = weight ?: return null
            val bmr = GoalCalculator.bmr(male, age, height, weight)
            val tdee = GoalCalculator.tdee(male, age, height, weight, work, exercise)
            val target = GoalCalculator.dailyCalories(male, age, height, weight, work, exercise, plan)
            return WizardResult(
                bmr = bmr.toInt(),
                tdee = tdee.toInt(),
                targetCalories = target,
                raisedToMinimum = target > (tdee + plan.calorieDelta).toInt(),
            )
        }
}

class GoalWizardViewModel(
    private val goalRepository: GoalRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GoalWizardUiState())
    val uiState: StateFlow<GoalWizardUiState> = _uiState.asStateFlow()

    /** One-shot per successful save so the screen can close itself (same pattern as [GoalsViewModel]). */
    private val _savedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            goalRepository.getGoal().collect { goal ->
                _uiState.update { it.copy(currentGoalCalories = goal?.calorieTarget) }
            }
        }
        viewModelScope.launch {
            settingsRepository.weightUnit.collect { unit ->
                _uiState.update { it.copy(weightUnit = unit) }
            }
        }
    }

    fun onSexSelected(male: Boolean) = _uiState.update { it.copy(male = male) }

    fun onAgeChanged(value: String) = _uiState.update { it.copy(ageText = value) }

    fun onHeightChanged(value: String) = _uiState.update { it.copy(heightText = value) }

    fun onWeightChanged(value: String) = _uiState.update { it.copy(weightText = value) }

    fun onWorkSelected(work: WorkActivity) = _uiState.update { it.copy(work = work) }

    fun onExerciseSelected(exercise: ExerciseLevel) = _uiState.update { it.copy(exercise = exercise) }

    fun onPlanSelected(plan: WeightPlan) = _uiState.update { it.copy(plan = plan) }

    fun nextStep() = _uiState.update { it.copy(step = (it.step + 1).coerceAtMost(LAST_STEP)) }

    fun previousStep() = _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    /**
     * Persists the suggestion as the daily goal: calories from the wizard, macros from the
     * same 30/25/45 split the Goals screen uses, weight target carried over unchanged.
     */
    fun applyAsGoal() {
        val result = _uiState.value.result ?: return
        val calories = result.targetCalories
        viewModelScope.launch {
            val existingWeightTarget = goalRepository.getGoal().first()?.weightTargetKg
            goalRepository.setGoal(
                calorieTarget = calories,
                proteinTargetG = (calories * 0.30 / 4).toInt(),
                fatTargetG = (calories * 0.25 / 9).toInt(),
                carbsTargetG = (calories * 0.45 / 4).toInt(),
                weightTargetKg = existingWeightTarget,
            )
            _savedEvent.emit(Unit)
        }
    }

    companion object {
        const val LAST_STEP = 4
    }
}
