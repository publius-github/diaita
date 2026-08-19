package app.diaita.data.repository

import app.diaita.data.db.dao.WaterEntryDao
import app.diaita.data.db.entities.WaterEntry
import kotlinx.coroutines.flow.Flow

class WaterRepository(
    private val waterEntryDao: WaterEntryDao,
) {
    /** The day's individual taps, oldest first — the diary's water row edits these directly. */
    fun entriesForDate(date: String): Flow<List<WaterEntry>> = waterEntryDao.entriesForDate(date)

    /** Returns the stored row (with its id) so the caller can offer an exact undo. */
    suspend fun logWater(
        date: String,
        amountMl: Int,
    ): WaterEntry {
        val entry = WaterEntry(date = date, amountMl = amountMl)
        val id = waterEntryDao.insert(entry)
        return entry.copy(id = id)
    }

    suspend fun updateAmount(
        id: Long,
        amountMl: Int,
    ) = waterEntryDao.updateAmount(id, amountMl)

    suspend fun delete(entries: List<WaterEntry>) = waterEntryDao.deleteByIds(entries.map { it.id })
}
