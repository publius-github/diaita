package app.diaita.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.settings.SettingsRepository
import app.diaita.data.settings.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val weightUnit: StateFlow<WeightUnit> =
        settingsRepository.weightUnit
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeightUnit.KG)

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch { settingsRepository.setWeightUnit(unit) }
    }

    /**
     * Stamps onboarding as done (used by both the skip path and wizard completion).
     * MainActivity observes the flag and swaps the main UI in once it lands.
     */
    fun finish() {
        viewModelScope.launch { settingsRepository.setOnboardingDone() }
    }
}
