package app.diaita.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val context: Context,
) {
    /**
     * All reads go through this flow: a failed prefs-file read (per DataStore guidance,
     * an [IOException]) degrades to defaults instead of crashing every UI collector.
     */
    private val prefs: Flow<Preferences> =
        context.settingsDataStore.data.catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    private val openFoodFactsKey = booleanPreferencesKey("open_food_facts_enabled")
    private val seedCatalogVersionKey = intPreferencesKey("seed_catalog_version")

    val openFoodFactsEnabled: Flow<Boolean> =
        prefs.map { prefs -> prefs[openFoodFactsKey] ?: true }

    suspend fun setOpenFoodFactsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[openFoodFactsKey] = enabled }
    }

    // --- Tracking ---

    private val trackMacrosKey = booleanPreferencesKey("track_macros")

    /** Whether the user cares about P/F/C at all; hides the macro progress UI when off. */
    val trackMacros: Flow<Boolean> =
        prefs.map { prefs -> prefs[trackMacrosKey] ?: true }

    suspend fun setTrackMacros(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[trackMacrosKey] = enabled }
    }

    private val morningEndMinutesKey = intPreferencesKey("morning_end_minutes")
    private val dayEndMinutesKey = intPreferencesKey("day_end_minutes")

    /** Cut-off times splitting the day into morning/day/evening; see [DayPartBoundaries]. */
    val dayPartBoundaries: Flow<DayPartBoundaries> =
        prefs.map { prefs ->
            DayPartBoundaries(
                morningEndMinutes =
                    prefs[morningEndMinutesKey] ?: DayPartBoundaries.DEFAULT_MORNING_END_MINUTES,
                dayEndMinutes =
                    prefs[dayEndMinutesKey] ?: DayPartBoundaries.DEFAULT_DAY_END_MINUTES,
            )
        }

    suspend fun setDayPartBoundaries(boundaries: DayPartBoundaries) {
        context.settingsDataStore.edit { prefs ->
            prefs[morningEndMinutesKey] = boundaries.morningEndMinutes
            prefs[dayEndMinutesKey] = boundaries.dayEndMinutes
        }
    }

    private val morningSharePercentKey = intPreferencesKey("morning_share_percent")
    private val daySharePercentKey = intPreferencesKey("day_share_percent")

    /** Reference calorie shares per day part; see [DayPartShares]. */
    val dayPartShares: Flow<DayPartShares> =
        prefs.map { prefs ->
            DayPartShares(
                morningPercent = prefs[morningSharePercentKey] ?: DayPartShares.DEFAULT_MORNING_PERCENT,
                dayPercent = prefs[daySharePercentKey] ?: DayPartShares.DEFAULT_DAY_PERCENT,
            )
        }

    suspend fun setDayPartShares(shares: DayPartShares) {
        context.settingsDataStore.edit { prefs ->
            prefs[morningSharePercentKey] = shares.morningPercent
            prefs[daySharePercentKey] = shares.dayPercent
        }
    }

    private val coachToneKey = stringPreferencesKey("coach_tone")

    /** Voice of the Home-card pace message; the original strings are the SNARKY set. */
    val coachTone: Flow<CoachTone> =
        prefs.map { prefs ->
            prefs[coachToneKey]?.let { stored -> CoachTone.entries.find { it.name == stored } }
                ?: CoachTone.SNARKY
        }

    suspend fun setCoachTone(tone: CoachTone) {
        context.settingsDataStore.edit { prefs -> prefs[coachToneKey] = tone.name }
    }

    private val weightUnitKey = stringPreferencesKey("weight_unit")

    /** Display unit for body weight — storage stays in kg. */
    val weightUnit: Flow<WeightUnit> =
        prefs.map { prefs ->
            prefs[weightUnitKey]?.let { stored -> WeightUnit.entries.find { it.name == stored } }
                ?: WeightUnit.KG
        }

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.settingsDataStore.edit { prefs -> prefs[weightUnitKey] = unit.name }
    }

    // --- Goal wizard ---

    private val wizardMaleKey = booleanPreferencesKey("wizard_male")
    private val wizardAgeKey = intPreferencesKey("wizard_age")
    private val wizardHeightCmKey = doublePreferencesKey("wizard_height_cm")
    private val wizardWeightKgKey = doublePreferencesKey("wizard_weight_kg")
    private val wizardWorkKey = stringPreferencesKey("wizard_work")
    private val wizardExerciseKey = stringPreferencesKey("wizard_exercise")

    /**
     * Body stats the goal wizard was last completed with. Null until the wizard has been
     * applied once. Used to prefill the wizard and to notice "your weight changed —
     * recalculate?" when new weigh-ins drift from the stored value.
     */
    val wizardInputs: Flow<WizardInputs?> =
        prefs.map { prefs ->
            val age = prefs[wizardAgeKey] ?: return@map null
            val heightCm = prefs[wizardHeightCmKey] ?: return@map null
            val weightKg = prefs[wizardWeightKgKey] ?: return@map null
            WizardInputs(
                male = prefs[wizardMaleKey] ?: true,
                age = age,
                heightCm = heightCm,
                weightKg = weightKg,
                work = prefs[wizardWorkKey],
                exercise = prefs[wizardExerciseKey],
            )
        }

    suspend fun setWizardInputs(inputs: WizardInputs) {
        context.settingsDataStore.edit { prefs ->
            prefs[wizardMaleKey] = inputs.male
            prefs[wizardAgeKey] = inputs.age
            prefs[wizardHeightCmKey] = inputs.heightCm
            prefs[wizardWeightKgKey] = inputs.weightKg
            inputs.work?.let { prefs[wizardWorkKey] = it }
            inputs.exercise?.let { prefs[wizardExerciseKey] = it }
        }
    }

    private val macroSplitProteinKey = intPreferencesKey("macro_split_protein")
    private val macroSplitFatKey = intPreferencesKey("macro_split_fat")

    /** Macro calorie split applied by the wizard; carbs take the remainder to 100. */
    val macroSplit: Flow<MacroSplit> =
        prefs.map { prefs ->
            MacroSplit(
                proteinPercent = prefs[macroSplitProteinKey] ?: MacroSplit.DEFAULT_PROTEIN_PERCENT,
                fatPercent = prefs[macroSplitFatKey] ?: MacroSplit.DEFAULT_FAT_PERCENT,
            )
        }

    suspend fun setMacroSplit(split: MacroSplit) {
        context.settingsDataStore.edit { prefs ->
            prefs[macroSplitProteinKey] = split.proteinPercent
            prefs[macroSplitFatKey] = split.fatPercent
        }
    }

    // --- Notifications ---

    private val reminderEnabledKey = booleanPreferencesKey("daily_reminder_enabled")
    private val reminderTimeKey = intPreferencesKey("daily_reminder_time_minutes")

    /** Off by default — notifications are opt-in. */
    val reminderEnabled: Flow<Boolean> =
        prefs.map { prefs -> prefs[reminderEnabledKey] ?: false }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[reminderEnabledKey] = enabled }
    }

    /** Minutes of day when the "nothing logged yet" reminder fires. Default 20:00. */
    val reminderTimeMinutes: Flow<Int> =
        prefs.map { prefs -> prefs[reminderTimeKey] ?: DEFAULT_REMINDER_MINUTES }

    suspend fun setReminderTimeMinutes(minutes: Int) {
        context.settingsDataStore.edit { prefs -> prefs[reminderTimeKey] = minutes }
    }

    private val weightReminderEnabledKey = booleanPreferencesKey("weight_reminder_enabled")
    private val weightReminderTimeKey = intPreferencesKey("weight_reminder_time_minutes")

    /** Off by default — notifications are opt-in. */
    val weightReminderEnabled: Flow<Boolean> =
        prefs.map { prefs -> prefs[weightReminderEnabledKey] ?: false }

    suspend fun setWeightReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[weightReminderEnabledKey] = enabled }
    }

    /** Minutes of day when the "no weight logged" reminder fires. Default 09:00. */
    val weightReminderTimeMinutes: Flow<Int> =
        prefs.map { prefs ->
            prefs[weightReminderTimeKey] ?: DEFAULT_WEIGHT_REMINDER_MINUTES
        }

    suspend fun setWeightReminderTimeMinutes(minutes: Int) {
        context.settingsDataStore.edit { prefs -> prefs[weightReminderTimeKey] = minutes }
    }

    private val dayPartRemindersEnabledKey = booleanPreferencesKey("day_part_reminders_enabled")

    /** Off by default — fires at the morning/day boundaries when that part logged nothing. */
    val dayPartRemindersEnabled: Flow<Boolean> =
        prefs.map { prefs -> prefs[dayPartRemindersEnabledKey] ?: false }

    suspend fun setDayPartRemindersEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[dayPartRemindersEnabledKey] = enabled }
    }

    private val eveningReminderEnabledKey = booleanPreferencesKey("evening_reminder_enabled")

    /** Off by default — the end-of-day "evening logged nothing" nudge. */
    val eveningReminderEnabled: Flow<Boolean> =
        prefs.map { prefs -> prefs[eveningReminderEnabledKey] ?: false }

    suspend fun setEveningReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[eveningReminderEnabledKey] = enabled }
    }

    // --- Appearance ---

    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")

    val themeMode: Flow<ThemeMode> =
        prefs.map { prefs ->
            prefs[themeModeKey]?.let { stored -> ThemeMode.entries.find { it.name == stored } }
                ?: ThemeMode.SYSTEM
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs -> prefs[themeModeKey] = mode.name }
    }

    /** Material You dynamic color (Android 12+); the brand palette is used when off. */
    val dynamicColor: Flow<Boolean> =
        prefs.map { prefs -> prefs[dynamicColorKey] ?: true }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[dynamicColorKey] = enabled }
    }

    // --- Data ---

    private val recentFoodsCountKey = intPreferencesKey("recent_foods_count")

    /** How many recently logged foods the quick-access chip row shows. */
    val recentFoodsCount: Flow<Int> =
        prefs.map { prefs -> prefs[recentFoodsCountKey] ?: DEFAULT_RECENT_FOODS_COUNT }

    suspend fun setRecentFoodsCount(count: Int) {
        context.settingsDataStore.edit { prefs -> prefs[recentFoodsCountKey] = count }
    }

    private val retentionDaysKey = intPreferencesKey("retention_days")

    /** Days raw diary rows are kept before folding into monthly averages. */
    val retentionDays: Flow<Int> =
        prefs.map { prefs -> prefs[retentionDaysKey] ?: DEFAULT_RETENTION_DAYS }

    suspend fun setRetentionDays(days: Int) {
        context.settingsDataStore.edit { prefs -> prefs[retentionDaysKey] = days }
    }

    private val autoBackupUriKey = stringPreferencesKey("auto_backup_uri")

    /** SAF tree URI the weekly automatic backup writes into; null = auto-backup off. */
    val autoBackupUri: Flow<String?> =
        prefs.map { prefs -> prefs[autoBackupUriKey] }

    suspend fun setAutoBackupUri(uri: String?) {
        context.settingsDataStore.edit { prefs ->
            if (uri == null) prefs.remove(autoBackupUriKey) else prefs[autoBackupUriKey] = uri
        }
    }

    private val lastBackupAtKey = longPreferencesKey("last_backup_at")

    /** When a database backup was last exported; null = never. Drives the "back up your data" nudge. */
    val lastBackupAt: Flow<Long?> =
        prefs.map { prefs -> prefs[lastBackupAtKey] }

    suspend fun setLastBackupAt(timestamp: Long) {
        context.settingsDataStore.edit { prefs -> prefs[lastBackupAtKey] = timestamp }
    }

    // --- Internal bookkeeping ---

    /** Last seed-catalog version applied to the database; 0 means never synced. */
    val seedCatalogVersion: Flow<Int> =
        prefs.map { prefs -> prefs[seedCatalogVersionKey] ?: 0 }

    suspend fun setSeedCatalogVersion(version: Int) {
        context.settingsDataStore.edit { prefs -> prefs[seedCatalogVersionKey] = version }
    }

    companion object {
        const val DEFAULT_REMINDER_MINUTES = 20 * 60
        const val DEFAULT_WEIGHT_REMINDER_MINUTES = 9 * 60
        const val DEFAULT_RECENT_FOODS_COUNT = 10
        const val DEFAULT_RETENTION_DAYS = 365
    }
}
