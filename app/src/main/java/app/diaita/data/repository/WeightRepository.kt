package app.diaita.data.repository

import app.diaita.data.db.dao.WeightEntryDao
import app.diaita.data.db.entities.WeightEntry
import kotlinx.coroutines.flow.Flow

class WeightRepository(
    private val weightEntryDao: WeightEntryDao,
) {
    fun range(
        fromDate: String,
        toDate: String,
    ): Flow<List<WeightEntry>> = weightEntryDao.range(fromDate, toDate)

    suspend fun logWeight(
        date: String,
        weightKg: Double,
    ) {
        weightEntryDao.upsert(WeightEntry(date = date, weightKg = weightKg))
    }
}
