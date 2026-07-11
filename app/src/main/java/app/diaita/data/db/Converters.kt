package app.diaita.data.db

import androidx.room.TypeConverter
import app.diaita.data.db.entities.FoodSource
import app.diaita.data.db.entities.MealSlot

class Converters {
    @TypeConverter
    fun foodSourceToString(value: FoodSource): String = value.name

    @TypeConverter
    fun stringToFoodSource(value: String): FoodSource = FoodSource.valueOf(value)

    @TypeConverter
    fun mealSlotToString(value: MealSlot): String = value.name

    @TypeConverter
    fun stringToMealSlot(value: String): MealSlot = MealSlot.valueOf(value)
}
