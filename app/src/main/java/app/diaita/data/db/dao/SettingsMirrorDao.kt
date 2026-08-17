package app.diaita.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.diaita.data.db.entities.SettingMirrorEntry

@Dao
interface SettingsMirrorDao {
    @Query("SELECT * FROM settings_mirror")
    suspend fun getAll(): List<SettingMirrorEntry>

    @Query("DELETE FROM settings_mirror")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<SettingMirrorEntry>)

    /** Full-snapshot write: the mirror always reflects the complete current preference set. */
    @Transaction
    suspend fun replaceAll(entries: List<SettingMirrorEntry>) {
        deleteAll()
        insertAll(entries)
    }
}
