package app.diaita.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.diaita.data.db.entities.MonthlyAggregate
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyAggregateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(aggregate: MonthlyAggregate)

    @Query("SELECT * FROM monthly_aggregates WHERE yearMonth = :yearMonth")
    suspend fun get(yearMonth: String): MonthlyAggregate?

    @Query("SELECT * FROM monthly_aggregates ORDER BY yearMonth DESC")
    fun getAll(): Flow<List<MonthlyAggregate>>
}
