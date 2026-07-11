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
import app.diaita.data.settings.SettingsRepository
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
    version = 5,
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
        const val DATABASE_NAME = "calorie_counter.db"

        @Volatile private var instance: AppDatabase? = null

        fun getInstance(
            context: Context,
            appScope: CoroutineScope,
        ): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DATABASE_NAME,
                    ).addCallback(
                        SeedDatabaseCallback(
                            appScope,
                            SettingsRepository(context.applicationContext),
                        ) { instance!! },
                    ).addMigrations(*Migrations.all)
                    .build()
                    .also { instance = it }
            }
    }
}
