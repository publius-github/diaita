package app.diaita.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val context: Context,
) {
    private val openFoodFactsKey = booleanPreferencesKey("open_food_facts_enabled")
    private val seedCatalogVersionKey = intPreferencesKey("seed_catalog_version")

    val openFoodFactsEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs -> prefs[openFoodFactsKey] ?: true }

    suspend fun setOpenFoodFactsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[openFoodFactsKey] = enabled }
    }

    // --- Tracking ---

    private val trackMacrosKey = booleanPreferencesKey("track_macros")

    /** Whether the user cares about P/F/C at all; hides the macro progress UI when off. */
    val trackMacros: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs -> prefs[trackMacrosKey] ?: true }

    suspend fun setTrackMacros(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[trackMacrosKey] = enabled }
    }

    private val morningEndMinutesKey = intPreferencesKey("morning_end_minutes")
    private val dayEndMinutesKey = intPreferencesKey("day_end_minutes")

    /** Cut-off times splitting the day into morning/day/evening; see [DayPartBoundaries]. */
    val dayPartBoundaries: Flow<DayPartBoundaries> =
        context.settingsDataStore.data.map { prefs ->
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
        context.settingsDataStore.data.map { prefs ->
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
        context.settingsDataStore.data.map { prefs ->
            prefs[coachToneKey]?.let { stored -> CoachTone.entries.find { it.name == stored } }
                ?: CoachTone.SNARKY
        }

    suspend fun setCoachTone(tone: CoachTone) {
        context.settingsDataStore.edit { prefs -> prefs[coachToneKey] = tone.name }
    }

    private val weightUnitKey = stringPreferencesKey("weight_unit")

    /** Display unit for body weight — storage stays in kg. */
    val weightUnit: Flow<WeightUnit> =
        context.settingsDataStore.data.map { prefs ->
            prefs[weightUnitKey]?.let { stored -> WeightUnit.entries.find { it.name == stored } }
                ?: WeightUnit.KG
        }

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.settingsDataStore.edit { prefs -> prefs[weightUnitKey] = unit.name }
    }

    // --- Notifications ---

    private val reminderEnabledKey = booleanPreferencesKey("daily_reminder_enabled")
    private val reminderTimeKey = intPreferencesKey("daily_reminder_time_minutes")

    /** Off by default — notifications are opt-in. */
    val reminderEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs -> prefs[reminderEnabledKey] ?: false }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[reminderEnabledKey] = enabled }
    }

    /** Minutes of day when the "nothing logged yet" reminder fires. Default 20:00. */
    val reminderTimeMinutes: Flow<Int> =
        context.settingsDataStore.data.map { prefs -> prefs[reminderTimeKey] ?: DEFAULT_REMINDER_MINUTES }

    suspend fun setReminderTimeMinutes(minutes: Int) {
        context.settingsDataStore.edit { prefs -> prefs[reminderTimeKey] = minutes }
    }

    private val weightReminderEnabledKey = booleanPreferencesKey("weight_reminder_enabled")
    private val weightReminderTimeKey = intPreferencesKey("weight_reminder_time_minutes")

    /** Off by default — notifications are opt-in. */
    val weightReminderEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs -> prefs[weightReminderEnabledKey] ?: false }

    suspend fun setWeightReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[weightReminderEnabledKey] = enabled }
    }

    /** Minutes of day when the "no weight logged" reminder fires. Default 09:00. */
    val weightReminderTimeMinutes: Flow<Int> =
        context.settingsDataStore.data.map { prefs ->
            prefs[weightReminderTimeKey] ?: DEFAULT_WEIGHT_REMINDER_MINUTES
        }

    suspend fun setWeightReminderTimeMinutes(minutes: Int) {
        context.settingsDataStore.edit { prefs -> prefs[weightReminderTimeKey] = minutes }
    }

    private val dayPartRemindersEnabledKey = booleanPreferencesKey("day_part_reminders_enabled")

    /** Off by default — fires at the morning/day boundaries when that part logged nothing. */
    val dayPartRemindersEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs -> prefs[dayPartRemindersEnabledKey] ?: false }

    suspend fun setDayPartRemindersEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[dayPartRemindersEnabledKey] = enabled }
    }

    // --- Appearance ---

    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")

    val themeMode: Flow<ThemeMode> =
        context.settingsDataStore.data.map { prefs ->
            prefs[themeModeKey]?.let { stored -> ThemeMode.entries.find { it.name == stored } }
                ?: ThemeMode.SYSTEM
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs -> prefs[themeModeKey] = mode.name }
    }

    /** Material You dynamic color (Android 12+); the brand palette is used when off. */
    val dynamicColor: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs -> prefs[dynamicColorKey] ?: true }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[dynamicColorKey] = enabled }
    }

    // --- Data ---

    private val recentFoodsCountKey = intPreferencesKey("recent_foods_count")

    /** How many recently logged foods the quick-access chip row shows. */
    val recentFoodsCount: Flow<Int> =
        context.settingsDataStore.data.map { prefs -> prefs[recentFoodsCountKey] ?: DEFAULT_RECENT_FOODS_COUNT }

    suspend fun setRecentFoodsCount(count: Int) {
        context.settingsDataStore.edit { prefs -> prefs[recentFoodsCountKey] = count }
    }

    private val retentionDaysKey = intPreferencesKey("retention_days")

    /** Days raw diary rows are kept before folding into monthly averages. */
    val retentionDays: Flow<Int> =
        context.settingsDataStore.data.map { prefs -> prefs[retentionDaysKey] ?: DEFAULT_RETENTION_DAYS }

    suspend fun setRetentionDays(days: Int) {
        context.settingsDataStore.edit { prefs -> prefs[retentionDaysKey] = days }
    }

    // --- Internal bookkeeping ---

    /** Last seed-catalog version applied to the database; 0 means never synced. */
    val seedCatalogVersion: Flow<Int> =
        context.settingsDataStore.data.map { prefs -> prefs[seedCatalogVersionKey] ?: 0 }

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
