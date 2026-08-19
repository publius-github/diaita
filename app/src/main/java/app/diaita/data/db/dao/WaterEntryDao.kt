package app.diaita.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.diaita.data.db.entities.WaterEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterEntryDao {
    @Insert
    suspend fun insert(entry: WaterEntry): Long

    /** Null when the day has no water rows at all (SUM over nothing), 0 never occurs. */
    @Query("SELECT SUM(amountMl) FROM water_entries WHERE date = :date")
    fun totalForDate(date: String): Flow<Int?>

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM water_entries ORDER BY date ASC, loggedAt ASC")
    suspend fun getAll(): List<WaterEntry>
}
