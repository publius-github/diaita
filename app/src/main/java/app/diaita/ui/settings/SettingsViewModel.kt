package app.diaita.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.backup.DatabaseBackupManager
import app.diaita.data.backup.RestoreOutcome
import app.diaita.data.repository.ExportRepository
import app.diaita.data.repository.ImportOutcome
import app.diaita.data.settings.CoachTone
import app.diaita.data.settings.DayPartBoundaries
import app.diaita.data.settings.DayPartOverThresholds
import app.diaita.data.settings.DayPartShares
import app.diaita.data.settings.HomeProgressStyle
import app.diaita.data.settings.MacroOverTolerances
import app.diaita.data.settings.MicronutrientOverTolerances
import app.diaita.data.settings.SettingsRepository
import app.diaita.data.settings.ThemeMode
import app.diaita.data.settings.WaterOverThresholds
import app.diaita.data.settings.WeightUnit
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

    // --- Tracking ---

    val trackMacros: StateFlow<Boolean> =
        settingsRepository.trackMacros
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setTrackMacros(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTrackMacros(enabled) }
    }

    val trackMicronutrients: StateFlow<Boolean> =
        settingsRepository.trackMicronutrients
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setTrackMicronutrients(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTrackMicronutrients(enabled) }
    }

    val trackWater: StateFlow<Boolean> =
        settingsRepository.trackWater
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setTrackWater(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTrackWater(enabled) }
    }

    val waterGlassMl: StateFlow<Int> =
        settingsRepository.waterGlassMl
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                SettingsRepository.DEFAULT_WATER_GLASS_ML,
            )

    fun setWaterGlassMl(ml: Int) {
        viewModelScope.launch {
            settingsRepository.setWaterGlassMl(ml.coerceIn(SettingsRepository.WATER_GLASS_RANGE_ML))
        }
    }

    val waterOverThresholds: StateFlow<WaterOverThresholds> =
        settingsRepository.waterOverThresholds
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WaterOverThresholds())

    fun setWaterOverThresholds(
        amberPercent: Int,
        redPercent: Int,
    ) {
        viewModelScope.launch {
            settingsRepository.setWaterOverThresholds(
                WaterOverThresholds.sanitized(amberPercent, redPercent),
            )
        }
    }

    val dayPartBoundaries: StateFlow<DayPartBoundaries> =
        settingsRepository.dayPartBoundaries
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayPartBoundaries())

    /**
     * Keeps morning-end strictly before day-end (at least [MIN_DAY_PART_GAP_MINUTES] apart).
     * Returns the sanitized result so the caller can e.g. reschedule day-part reminders.
     */
    fun setMorningEndMinutes(minutes: Int): DayPartBoundaries {
        val current = dayPartBoundaries.value
        val updated =
            current.copy(
                morningEndMinutes = minutes.coerceAtMost(current.dayEndMinutes - MIN_DAY_PART_GAP_MINUTES),
            )
        viewModelScope.launch { settingsRepository.setDayPartBoundaries(updated) }
        return updated
    }

    /** Counterpart of [setMorningEndMinutes] for the day/evening boundary. */
    fun setDayEndMinutes(minutes: Int): DayPartBoundaries {
        val current = dayPartBoundaries.value
        val updated =
            current.copy(
                dayEndMinutes = minutes.coerceAtLeast(current.morningEndMinutes + MIN_DAY_PART_GAP_MINUTES),
            )
        viewModelScope.launch { settingsRepository.setDayPartBoundaries(updated) }
        return updated
    }

    val dayPartShares: StateFlow<DayPartShares> =
        settingsRepository.dayPartShares
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayPartShares())

    fun setDayPartShares(
        morningPercent: Int,
        dayPercent: Int,
    ) {
        viewModelScope.launch {
            settingsRepository.setDayPartShares(DayPartShares.sanitized(morningPercent, dayPercent))
        }
    }

    val athleticBmiBands: StateFlow<Boolean> =
        settingsRepository.athleticBmiBands
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setAthleticBmiBands(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAthleticBmiBands(enabled) }
    }

    val macroOverTolerances: StateFlow<MacroOverTolerances> =
        settingsRepository.macroOverTolerances
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MacroOverTolerances())

    fun setMacroOverTolerances(
        proteinPercent: Int,
        fatPercent: Int,
        carbsPercent: Int,
    ) {
        viewModelScope.launch {
            settingsRepository.setMacroOverTolerances(
                MacroOverTolerances.sanitized(proteinPercent, fatPercent, carbsPercent),
            )
        }
    }

    val dayPartOverThresholds: StateFlow<DayPartOverThresholds> =
        settingsRepository.dayPartOverThresholds
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayPartOverThresholds())

    fun setDayPartOverThresholds(amberPercent: Int) {
        viewModelScope.launch {
            settingsRepository.setDayPartOverThresholds(
                DayPartOverThresholds.sanitized(amberPercent),
            )
        }
    }

    val microOverTolerances: StateFlow<MicronutrientOverTolerances> =
        settingsRepository.microOverTolerances
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MicronutrientOverTolerances())

    fun setMicroOverTolerances(
        fiberPercent: Int,
        sugarPercent: Int,
        saltPercent: Int,
    ) {
        viewModelScope.launch {
            settingsRepository.setMicroOverTolerances(
                MicronutrientOverTolerances.sanitized(fiberPercent, sugarPercent, saltPercent),
            )
        }
    }

    val coachTone: StateFlow<CoachTone> =
        settingsRepository.coachTone
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoachTone.SNARKY)

    fun setCoachTone(tone: CoachTone) {
        viewModelScope.launch { settingsRepository.setCoachTone(tone) }
    }

    val weightUnit: StateFlow<WeightUnit> =
        settingsRepository.weightUnit
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeightUnit.KG)

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch { settingsRepository.setWeightUnit(unit) }
    }

    // --- Appearance ---

    val themeMode: StateFlow<ThemeMode> =
        settingsRepository.themeMode
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    val dynamicColor: StateFlow<Boolean> =
        settingsRepository.dynamicColor
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    val homeProgressStyle: StateFlow<HomeProgressStyle> =
        settingsRepository.homeProgressStyle
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeProgressStyle.RING)

    fun setHomeProgressStyle(style: HomeProgressStyle) {
        viewModelScope.launch { settingsRepository.setHomeProgressStyle(style) }
    }

    // --- Notifications ---

    val reminderEnabled: StateFlow<Boolean> =
        settingsRepository.reminderEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReminderEnabled(enabled) }
    }

    val reminderTimeMinutes: StateFlow<Int> =
        settingsRepository.reminderTimeMinutes
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                SettingsRepository.DEFAULT_REMINDER_MINUTES,
            )

    fun setReminderTimeMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepository.setReminderTimeMinutes(minutes) }
    }

    val weightReminderEnabled: StateFlow<Boolean> =
        settingsRepository.weightReminderEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setWeightReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setWeightReminderEnabled(enabled) }
    }

    val weightReminderTimeMinutes: StateFlow<Int> =
        settingsRepository.weightReminderTimeMinutes
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                SettingsRepository.DEFAULT_WEIGHT_REMINDER_MINUTES,
            )

    fun setWeightReminderTimeMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepository.setWeightReminderTimeMinutes(minutes) }
    }

    val dayPartRemindersEnabled: StateFlow<Boolean> =
        settingsRepository.dayPartRemindersEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDayPartRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDayPartRemindersEnabled(enabled) }
    }

    val eveningReminderEnabled: StateFlow<Boolean> =
        settingsRepository.eveningReminderEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setEveningReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setEveningReminderEnabled(enabled) }
    }

    // --- Data ---

    val recentFoodsCount: StateFlow<Int> =
        settingsRepository.recentFoodsCount
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                SettingsRepository.DEFAULT_RECENT_FOODS_COUNT,
            )

    fun setRecentFoodsCount(count: Int) {
        viewModelScope.launch { settingsRepository.setRecentFoodsCount(count.coerceIn(RECENT_FOODS_RANGE)) }
    }

    val retentionDays: StateFlow<Int> =
        settingsRepository.retentionDays
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                SettingsRepository.DEFAULT_RETENTION_DAYS,
            )

    fun setRetentionDays(days: Int) {
        viewModelScope.launch { settingsRepository.setRetentionDays(days) }
    }

    val lastBackupAt: StateFlow<Long?> =
        settingsRepository.lastBackupAt
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val autoBackupUri: StateFlow<String?> =
        settingsRepository.autoBackupUri
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setAutoBackupUri(uri: String?) {
        viewModelScope.launch { settingsRepository.setAutoBackupUri(uri) }
    }

    suspend fun exportAllCsv(): String = exportRepository.exportAllCsv()

    suspend fun importCsv(content: String): ImportOutcome = exportRepository.importCsv(content)

    suspend fun exportDatabase(uri: Uri): Boolean =
        runCatching { backupManager.exportTo(uri) }
            .onSuccess { settingsRepository.setLastBackupAt(System.currentTimeMillis()) }
            .isSuccess

    suspend fun restoreDatabase(uri: Uri): RestoreOutcome =
        backupManager.restoreFrom(uri).also { outcome ->
            // The restored file carries the settings mirror; make the next start replay it
            // over DataStore so the backup's settings come back along with the data.
            if (outcome == RestoreOutcome.Success) settingsRepository.setSettingsReplayPending()
        }

    companion object {
        const val MIN_DAY_PART_GAP_MINUTES = 60
        val RECENT_FOODS_RANGE = 5..20
        val RETENTION_OPTIONS_DAYS = listOf(90, 180, 365, 730, SettingsRepository.RETENTION_FOREVER)
    }
}
