package app.diaita.data.repository

import app.diaita.data.db.dao.DiaryEntryDao
import app.diaita.data.db.dao.FoodItemDao
import app.diaita.data.export.DiaryCsvExporter

class ExportRepository(
    private val diaryEntryDao: DiaryEntryDao,
    private val foodItemDao: FoodItemDao,
) {
    suspend fun exportDiaryCsv(): String {
        val entries = diaryEntryDao.getAllEntries()
        val foodsById = foodItemDao.getAll().associateBy { it.id }
        return DiaryCsvExporter.toCsv(entries, foodsById)
    }
}
