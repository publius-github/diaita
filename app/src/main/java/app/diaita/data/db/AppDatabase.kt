package app.diaita.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.diaita.data.db.dao.DailyGoalDao
import app.diaita.data.db.dao.DiaryEntryDao
import app.diaita.data.db.dao.FoodItemDao
import app.diaita.data.db.dao.MealPresetDao
import app.diaita.data.db.dao.MonthlyAggregateDao
import app.diaita.data.db.dao.WeightEntryDao
import app.diaita.data.db.entities.DailyGoal
import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.MealPreset
import app.diaita.data.db.entities.MealPresetItem
import app.diaita.data.db.entities.MonthlyAggregate
import app.diaita.data.db.entities.WeightEntry
import app.diaita.data.seed.SeedDatabaseCallback
import kotlinx.coroutines.CoroutineScope

@Database(
    entities = [
        FoodItem::class,
        DiaryEntry::class,
        MealPreset::class,
        MealPresetItem::class,
        DailyGoal::class,
        WeightEntry::class,
        MonthlyAggregate::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao
    abstract fun diaryEntryDao(): DiaryEntryDao
    abstract fun mealPresetDao(): MealPresetDao
    abstract fun dailyGoalDao(): DailyGoalDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun monthlyAggregateDao(): MonthlyAggregateDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, appScope: CoroutineScope): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calorie_counter.db",
                )
                    .addCallback(SeedDatabaseCallback(appScope) { INSTANCE!! })
                    // No real user data to preserve yet at this stage of development — just recreate on
                    // schema changes instead of writing Migrations for a still-actively-evolving schema.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
