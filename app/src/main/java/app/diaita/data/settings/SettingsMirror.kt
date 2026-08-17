package app.diaita.data.settings

import app.diaita.data.db.dao.SettingsMirrorDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Keeps every DataStore preference mirrored into the `settings_mirror` table so the
 * single-file database backup carries settings along with the data. On start it first
 * replays the mirror back into DataStore — gap-fill normally (covers the reinstall case,
 * where sideloaded updates signed by a different dev machine wipe DataStore but the user
 * restores the database from a backup), full overwrite when the replay marker was set by
 * an explicit backup restore. It then follows every preference change write-through.
 */
class SettingsMirror(
    private val settingsRepository: SettingsRepository,
    private val dao: SettingsMirrorDao,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            val overwrite = settingsRepository.consumeSettingsReplayPending()
            val entries = dao.getAll()
            if (entries.isNotEmpty()) {
                settingsRepository.restoreFromMirror(entries, overwrite)
            }
            settingsRepository.rawPreferences.collect { preferences ->
                dao.replaceAll(serializePreferences(preferences.asMap()))
            }
        }
    }
}
