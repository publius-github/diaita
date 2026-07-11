package app.diaita.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

    private val reminderEnabledKey = booleanPreferencesKey("daily_reminder_enabled")

    /** Off by default — notifications are opt-in. */
    val reminderEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs -> prefs[reminderEnabledKey] ?: false }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[reminderEnabledKey] = enabled }
    }

    /** Last seed-catalog version applied to the database; 0 means never synced. */
    val seedCatalogVersion: Flow<Int> =
        context.settingsDataStore.data.map { prefs -> prefs[seedCatalogVersionKey] ?: 0 }

    suspend fun setSeedCatalogVersion(version: Int) {
        context.settingsDataStore.edit { prefs -> prefs[seedCatalogVersionKey] = version }
    }
}
