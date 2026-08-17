package app.diaita.data.settings

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.diaita.data.db.entities.SettingMirrorEntry

/**
 * Keys deliberately left out of the settings mirror. The SAF grant behind the auto-backup
 * folder does not survive a reinstall, so replaying its URI would schedule a worker that
 * can only fail — the user has to re-pick the folder anyway. The replay marker is restore
 * plumbing, not a setting.
 */
val MIRROR_EXCLUDED_KEYS = setOf("auto_backup_uri", "settings_replay_pending")

private const val TYPE_BOOLEAN = "boolean"
private const val TYPE_INT = "int"
private const val TYPE_LONG = "long"
private const val TYPE_FLOAT = "float"
private const val TYPE_DOUBLE = "double"
private const val TYPE_STRING = "string"

/** Snapshot of the whole preference map as mirror rows; unsupported value types are skipped. */
fun serializePreferences(preferences: Map<Preferences.Key<*>, Any>): List<SettingMirrorEntry> =
    preferences.mapNotNull { (key, value) ->
        if (key.name in MIRROR_EXCLUDED_KEYS) return@mapNotNull null
        val type =
            when (value) {
                is Boolean -> TYPE_BOOLEAN
                is Int -> TYPE_INT
                is Long -> TYPE_LONG
                is Float -> TYPE_FLOAT
                is Double -> TYPE_DOUBLE
                is String -> TYPE_STRING
                else -> return@mapNotNull null
            }
        SettingMirrorEntry(key = key.name, type = type, value = value.toString())
    }

/**
 * Replays one mirror row into the preference map. Without [overwrite] only absent keys are
 * filled (the reinstall case: an empty DataStore next to a restored database); with it the
 * mirror wins (an explicit backup restore: the backup's settings are part of the backup).
 * Unparseable or unknown-typed rows are ignored — a broken row must never block the rest.
 */
fun applyMirrorEntry(
    preferences: MutablePreferences,
    entry: SettingMirrorEntry,
    overwrite: Boolean,
) {
    if (entry.key in MIRROR_EXCLUDED_KEYS) return
    val raw = entry.value
    when (entry.type) {
        TYPE_BOOLEAN -> put(preferences, booleanPreferencesKey(entry.key), raw.toBooleanStrictOrNull(), overwrite)
        TYPE_INT -> put(preferences, intPreferencesKey(entry.key), raw.toIntOrNull(), overwrite)
        TYPE_LONG -> put(preferences, longPreferencesKey(entry.key), raw.toLongOrNull(), overwrite)
        TYPE_FLOAT -> put(preferences, floatPreferencesKey(entry.key), raw.toFloatOrNull(), overwrite)
        TYPE_DOUBLE -> put(preferences, doublePreferencesKey(entry.key), raw.toDoubleOrNull(), overwrite)
        TYPE_STRING -> put(preferences, stringPreferencesKey(entry.key), raw, overwrite)
    }
}

private fun <T> put(
    preferences: MutablePreferences,
    key: Preferences.Key<T>,
    value: T?,
    overwrite: Boolean,
) {
    if (value == null) return
    if (overwrite || key !in preferences) preferences[key] = value
}
