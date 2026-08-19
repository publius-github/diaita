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

    @Query("SELECT * FROM water_entries WHERE date = :date ORDER BY loggedAt ASC")
    fun entriesForDate(date: String): Flow<List<WaterEntry>>

    @Query("UPDATE water_entries SET amountMl = :amountMl WHERE id = :id")
    suspend fun updateAmount(
        id: Long,
        amountMl: Int,
    )

    @Query("DELETE FROM water_entries WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM water_entries ORDER BY date ASC, loggedAt ASC")
    suspend fun getAll(): List<WaterEntry>
}
