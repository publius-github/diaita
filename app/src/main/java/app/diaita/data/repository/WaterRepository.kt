package app.diaita.data.repository

import app.diaita.data.db.dao.WaterEntryDao
import app.diaita.data.db.entities.WaterEntry
import kotlinx.coroutines.flow.Flow

class WaterRepository(
    private val waterEntryDao: WaterEntryDao,
) {
    fun totalForDate(date: String): Flow<Int?> = waterEntryDao.totalForDate(date)

    /** Returns the stored row (with its id) so the caller can offer an exact undo. */
    suspend fun logWater(
        date: String,
        amountMl: Int,
    ): WaterEntry {
        val entry = WaterEntry(date = date, amountMl = amountMl)
        val id = waterEntryDao.insert(entry)
        return entry.copy(id = id)
    }

    suspend fun delete(entry: WaterEntry) = waterEntryDao.deleteById(entry.id)
}
