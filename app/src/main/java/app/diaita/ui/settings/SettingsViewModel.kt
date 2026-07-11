package app.diaita.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.backup.DatabaseBackupManager
import app.diaita.data.backup.RestoreOutcome
import app.diaita.data.repository.ExportRepository
import app.diaita.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val exportRepository: ExportRepository,
    private val backupManager: DatabaseBackupManager,
) : ViewModel() {
    val openFoodFactsEnabled: StateFlow<Boolean> =
        settingsRepository.openFoodFactsEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setOpenFoodFactsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setOpenFoodFactsEnabled(enabled) }
    }

    val reminderEnabled: StateFlow<Boolean> =
        settingsRepository.reminderEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReminderEnabled(enabled) }
    }

    suspend fun exportDiaryCsv(): String = exportRepository.exportDiaryCsv()

    suspend fun exportDatabase(uri: Uri): Boolean = runCatching { backupManager.exportTo(uri) }.isSuccess

    suspend fun restoreDatabase(uri: Uri): RestoreOutcome = backupManager.restoreFrom(uri)
}
