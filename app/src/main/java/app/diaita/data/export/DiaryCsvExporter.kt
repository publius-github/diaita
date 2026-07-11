package app.diaita.data.export

import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.FoodItem
import app.diaita.util.DateUtils
import app.diaita.util.NutrientMath

object DiaryCsvExporter {
    private val HEADER = listOf("date", "time", "food", "quantity_g", "calories", "protein_g", "fat_g", "carbs_g", "preset")

    fun toCsv(entries: List<DiaryEntry>, foodsById: Map<Long, FoodItem>): String {
        val rows = entries.map { entry ->
            val food = foodsById[entry.foodItemId]
            listOf(
                entry.logDate,
                DateUtils.formatTime(entry.loggedAt),
                food?.name.orEmpty(),
                entry.quantityGrams.toString(),
                food?.let { "%.1f".format(NutrientMath.caloriesFor(it, entry.quantityGrams)) } ?: "",
                food?.let { "%.1f".format(NutrientMath.proteinFor(it, entry.quantityGrams)) } ?: "",
                food?.let { "%.1f".format(NutrientMath.fatFor(it, entry.quantityGrams)) } ?: "",
                food?.let { "%.1f".format(NutrientMath.carbsFor(it, entry.quantityGrams)) } ?: "",
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
