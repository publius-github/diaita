package app.diaita.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.diaita.data.db.entities.SettingMirrorEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsMirrorCodecTest {
    @Test
    fun `preferences survive a serialize-apply round trip`() {
        val original = mutablePreferencesOf()
        original[stringPreferencesKey("coach_tone")] = "BRUTAL"
        original[intPreferencesKey("day_end_minutes")] = 1200
        original[booleanPreferencesKey("athletic_bmi_bands")] = true
        original[doublePreferencesKey("wizard_height_cm")] = 180.0

        val entries = serializePreferences(original.asMap())
        val restored = mutablePreferencesOf()
        entries.forEach { applyMirrorEntry(restored, it, overwrite = false) }

        assertEquals("BRUTAL", restored[stringPreferencesKey("coach_tone")])
        assertEquals(1200, restored[intPreferencesKey("day_end_minutes")])
        assertEquals(true, restored[booleanPreferencesKey("athletic_bmi_bands")])
        assertEquals(180.0, restored[doublePreferencesKey("wizard_height_cm")]!!, 1e-9)
    }

    @Test
    fun `gap-fill never overwrites a live value but replay does`() {
        val live = mutablePreferencesOf()
        live[stringPreferencesKey("coach_tone")] = "GENTLE"
        val entry = SettingMirrorEntry(key = "coach_tone", type = "string", value = "BRUTAL")

        applyMirrorEntry(live, entry, overwrite = false)
        assertEquals("GENTLE", live[stringPreferencesKey("coach_tone")])

        applyMirrorEntry(live, entry, overwrite = true)
        assertEquals("BRUTAL", live[stringPreferencesKey("coach_tone")])
    }

    @Test
    fun `excluded keys are neither serialized nor applied`() {
        val prefs = mutablePreferencesOf()
        prefs[stringPreferencesKey("auto_backup_uri")] = "content://old-grant"
        prefs[booleanPreferencesKey("settings_replay_pending")] = true
        assertTrue(serializePreferences(prefs.asMap()).isEmpty())

        val restored = mutablePreferencesOf()
        applyMirrorEntry(
            restored,
            SettingMirrorEntry(key = "auto_backup_uri", type = "string", value = "content://old-grant"),
            overwrite = true,
        )
        assertNull(restored[stringPreferencesKey("auto_backup_uri")])
    }

    @Test
    fun `broken rows are ignored instead of failing the replay`() {
        val restored = mutablePreferencesOf()
        applyMirrorEntry(restored, SettingMirrorEntry("day_end_minutes", "int", "not-a-number"), overwrite = true)
        applyMirrorEntry(restored, SettingMirrorEntry("mystery", "uuid", "whatever"), overwrite = true)
        assertNull(restored[intPreferencesKey("day_end_minutes")])
    }
}
