package app.diaita.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.diaita.data.db.entities.FoodItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {
    @Insert
    suspend fun insert(food: FoodItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(foods: List<FoodItem>)

    @Update
    suspend fun update(food: FoodItem)

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' ORDER BY name LIMIT 50")
    fun search(query: String): Flow<List<FoodItem>>

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
}
