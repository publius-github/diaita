package app.diaita.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.repository.ExportRepository
import app.diaita.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val exportRepository: ExportRepository,
) : ViewModel() {

    val openFoodFactsEnabled: StateFlow<Boolean> = settingsRepository.openFoodFactsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setOpenFoodFactsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setOpenFoodFactsEnabled(enabled) }
    }

    suspend fun exportDiaryCsv(): String = exportRepository.exportDiaryCsv()
}
