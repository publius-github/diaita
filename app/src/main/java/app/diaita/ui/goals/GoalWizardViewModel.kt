package app.diaita.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.repository.GoalRepository
import app.diaita.data.repository.WeightRepository
import app.diaita.data.settings.MacroSplit
import app.diaita.data.settings.SettingsRepository
import app.diaita.data.settings.WeightUnit
import app.diaita.data.settings.WizardInputs
import app.diaita.util.ExerciseLevel
import app.diaita.util.GoalCalculator
import app.diaita.util.WeightPlan
import app.diaita.util.WorkActivity
import app.diaita.util.formatGramsForInput
import app.diaita.util.formatWeightForDisplay
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

private const val CM_PER_FOOT = 30.48
private const val CM_PER_INCH = 2.54

data class GoalWizardUiState(
    val step: Int = 0,
    val male: Boolean = true,
    val ageText: String = "",
    /** Metric height entry (cm); used when [useImperialHeight] is false. */
    val heightText: String = "",
    /** Imperial height entry (feet + inches); shown when the weight unit is pounds. */
    val heightFeetText: String = "",
    val heightInchesText: String = "",
    val weightText: String = "",
    val work: WorkActivity = WorkActivity.SEDENTARY,
    val exercise: ExerciseLevel = ExerciseLevel.NONE,
    val plan: WeightPlan = WeightPlan.MAINTAIN,
    val macroSplit: MacroSplit = MacroSplit(),
    val currentGoalCalories: Int? = null,
    /** Unit the weight field is typed in; the BMR math below always runs on kilograms. */
    val weightUnit: WeightUnit = WeightUnit.KG,
) {
    val useImperialHeight: Boolean get() = weightUnit == WeightUnit.LB

    val age: Int? get() = ageText.toIntOrNull()?.takeIf { it in 10..100 }

    /** Height in cm regardless of the entry unit. */
    val height: Double?
        get() {
            val cm =
                if (useImperialHeight) {
                    val feet = heightFeetText.toIntOrNull() ?: return null
                    val inches = heightInchesText.normalizedDecimalOrNull() ?: 0.0
                    feet * CM_PER_FOOT + inches * CM_PER_INCH
                } else {
                    heightText.normalizedDecimalOrNull() ?: return null
                }
            return cm.takeIf { it in 100.0..250.0 }
        }
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
    private val settingsRepository: SettingsRepository,
    weightRepository: WeightRepository,
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
        viewModelScope.launch {
            settingsRepository.macroSplit.collect { split ->
                _uiState.update { it.copy(macroSplit = split) }
            }
        }
        // Prefill from the last completed run and the latest weigh-in: sex/age/height rarely
        // change, and the weight the user actually logs beats retyping it here.
        viewModelScope.launch {
            val unit = settingsRepository.weightUnit.first()
            val stored = settingsRepository.wizardInputs.first()
            val latestWeightKg = weightRepository.latest().first()?.weightKg ?: stored?.weightKg
            _uiState.update { state ->
                if (state.ageText.isNotBlank()) return@update state
                state.copy(
                    male = stored?.male ?: state.male,
                    ageText = stored?.age?.toString() ?: "",
                    heightText = stored?.heightCm?.let(::formatGramsForInput) ?: "",
                    heightFeetText = stored?.heightCm?.let { (it / CM_PER_FOOT).toInt().toString() } ?: "",
                    heightInchesText =
                        stored?.heightCm?.let {
                            formatGramsForInput(((it % CM_PER_FOOT) / CM_PER_INCH).toInt().toDouble())
                        } ?: "",
                    weightText = latestWeightKg?.let { formatWeightForDisplay(unit.fromKg(it)) } ?: "",
                    work = stored?.work?.let { name -> WorkActivity.entries.find { it.name == name } } ?: state.work,
                    exercise =
                        stored?.exercise?.let { name -> ExerciseLevel.entries.find { it.name == name } }
                            ?: state.exercise,
                )
            }
        }
    }

    fun onSexSelected(male: Boolean) = _uiState.update { it.copy(male = male) }

    fun onAgeChanged(value: String) = _uiState.update { it.copy(ageText = value) }

    fun onHeightChanged(value: String) = _uiState.update { it.copy(heightText = value) }

    fun onHeightFeetChanged(value: String) = _uiState.update { it.copy(heightFeetText = value) }

    fun onHeightInchesChanged(value: String) = _uiState.update { it.copy(heightInchesText = value) }

    fun onWeightChanged(value: String) = _uiState.update { it.copy(weightText = value) }

    fun onWorkSelected(work: WorkActivity) = _uiState.update { it.copy(work = work) }

    fun onExerciseSelected(exercise: ExerciseLevel) = _uiState.update { it.copy(exercise = exercise) }

    fun onPlanSelected(plan: WeightPlan) = _uiState.update { it.copy(plan = plan) }

    fun onMacroSplitSelected(split: MacroSplit) = _uiState.update { it.copy(macroSplit = split) }

    fun nextStep() = _uiState.update { it.copy(step = (it.step + 1).coerceAtMost(LAST_STEP)) }

    fun previousStep() = _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    /**
     * Persists the suggestion as the daily goal: calories from the wizard, macros from the
     * chosen split, weight target carried over unchanged. The wizard's TDEE is stored as
     * maintenance so History can draw the second reference line, and the body stats are
     * remembered so the next run starts prefilled (and Goals can notice weight drift).
     */
    fun applyAsGoal() {
        val state = _uiState.value
        val result = state.result ?: return
        val calories = result.targetCalories
        viewModelScope.launch {
            val existingWeightTarget = goalRepository.getGoal().first()?.weightTargetKg
            goalRepository.setGoal(
                calorieTarget = calories,
                proteinTargetG = state.macroSplit.proteinGrams(calories),
                fatTargetG = state.macroSplit.fatGrams(calories),
                carbsTargetG = state.macroSplit.carbsGrams(calories),
                weightTargetKg = existingWeightTarget,
                maintenanceCalories = result.tdee,
            )
            settingsRepository.setMacroSplit(state.macroSplit)
            settingsRepository.setWizardInputs(
                WizardInputs(
                    male = state.male,
                    age = checkNotNull(state.age),
                    heightCm = checkNotNull(state.height),
                    weightKg = checkNotNull(state.weight),
                    work = state.work.name,
                    exercise = state.exercise.name,
                ),
            )
            _savedEvent.emit(Unit)
        }
    }

    companion object {
        const val LAST_STEP = 4
    }
}
