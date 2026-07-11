package app.diaita.data.export

import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.FoodItem
import app.diaita.util.DateUtils
import app.diaita.util.NutrientMath
import java.util.Locale

object DiaryCsvExporter {
    private val HEADER =
        listOf("date", "time", "food", "quantity_g", "calories", "protein_g", "fat_g", "carbs_g", "preset")

    fun toCsv(
        entries: List<DiaryEntry>,
        foodsById: Map<Long, FoodItem>,
    ): String {
        val rows =
            entries.map { entry ->
                val food = foodsById[entry.foodItemId]
                listOf(
                    entry.logDate,
                    DateUtils.formatTime(entry.loggedAt),
                    food?.name.orEmpty(),
                    entry.quantityGrams.toString(),
                    // Locale.US keeps the decimal point a '.' — comma-decimal locales would break the CSV columns.
                    food?.let { "%.1f".format(Locale.US, NutrientMath.caloriesFor(it, entry.quantityGrams)) } ?: "",
                    food?.let { "%.1f".format(Locale.US, NutrientMath.proteinFor(it, entry.quantityGrams)) } ?: "",
                    food?.let { "%.1f".format(Locale.US, NutrientMath.fatFor(it, entry.quantityGrams)) } ?: "",
                    food?.let { "%.1f".format(Locale.US, NutrientMath.carbsFor(it, entry.quantityGrams)) } ?: "",
                    entry.sourcePresetName.orEmpty(),
                )
            }
        return (listOf(HEADER) + rows).joinToString("\n") { row -> row.joinToString(",") { escapeCsvField(it) } }
    }

    private fun escapeCsvField(field: String): String =
        if (field.contains(',') || field.contains('"') || field.contains('\n')) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
}
