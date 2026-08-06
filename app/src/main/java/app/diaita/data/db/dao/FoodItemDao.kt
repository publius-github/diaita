package app.diaita.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.FoodSource
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {
    @Insert
    suspend fun insert(food: FoodItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(foods: List<FoodItem>)

    @Update
    suspend fun update(food: FoodItem)

    /**
     * Name search ranked by how often (and, as a tiebreak, how recently) the food was actually
     * logged since [since] — the staples someone eats every week surface above the alphabet.
     * Never-logged foods sort alphabetically below them. QUICK rows are one-off calorie logs,
     * not catalog entries, so they never appear.
     */
    @Query(
        """
        SELECT f.* FROM food_items f
        LEFT JOIN diary_entries d ON d.foodItemId = f.id AND d.loggedAt >= :since
        WHERE f.name LIKE '%' || :query || '%' AND f.source != 'QUICK'
        GROUP BY f.id
        ORDER BY COUNT(d.id) DESC, MAX(d.loggedAt) DESC, f.name ASC
        LIMIT 50
        """,
    )
    fun search(
        query: String,
        since: Long,
    ): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items")
    suspend fun getAll(): List<FoodItem>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getById(id: Long): FoodItem?

    @Query("SELECT * FROM food_items WHERE offId = :offId LIMIT 1")
    suspend fun getByOffId(offId: String): FoodItem?

    @Query("SELECT * FROM food_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): FoodItem?

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun count(): Int

    @Query("SELECT * FROM food_items WHERE source = :source")
    suspend fun getBySource(source: FoodSource): List<FoodItem>
}
