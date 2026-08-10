package app.diaita.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.diaita.data.db.entities.WeightEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WeightEntry)

    @Query("SELECT * FROM weight_entries WHERE date BETWEEN :fromDate AND :toDate ORDER BY date ASC")
    fun range(
        fromDate: String,
        toDate: String,
    ): Flow<List<WeightEntry>>

    /** One-shot lookup used by the weight-reminder worker. */
    @Query("SELECT * FROM weight_entries WHERE date = :date")
    suspend fun forDateOnce(date: String): List<WeightEntry>

    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT 1")
    fun latest(): Flow<WeightEntry?>

    @Query("SELECT * FROM weight_entries ORDER BY date ASC")
    suspend fun getAll(): List<WeightEntry>

    @Query("DELETE FROM weight_entries WHERE date = :date")
    suspend fun deleteForDate(date: String)
}
