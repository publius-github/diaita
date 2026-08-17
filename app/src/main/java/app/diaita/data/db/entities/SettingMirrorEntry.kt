package app.diaita.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Write-through copy of one DataStore preference, kept inside the Room database so the
 * single-file database backup carries the user's settings too. DataStore does not survive
 * an uninstall/reinstall cycle (the reality of sideloaded updates signed by different dev
 * machines), and before this table existed a restore brought back the diary but silently
 * reset every setting. [type] names the preference's value type so the entry can be
 * replayed into a typed DataStore key on restore.
 */
@Entity(tableName = "settings_mirror")
data class SettingMirrorEntry(
    @PrimaryKey val key: String,
    val type: String,
    val value: String,
)
