package app.diaita.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.diaita.data.db.entities.DailyGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setGoal(goal: DailyGoal)

    @Query("SELECT * FROM daily_goals WHERE id = 1")
    fun getGoal(): Flow<DailyGoal?>
}
